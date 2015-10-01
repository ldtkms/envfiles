package hudson.plugins.envfiles;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * BuildWrapper to set environment variables from files.
 * 
 * @author Original by Anders Johansson Modified by Tuyen Lieu
 *
 */
public class EnvFileBuildWrapper extends BuildWrapper
{

    private static final Logger logger = Logger.getLogger(EnvFileBuildWrapper.class.getName());
    private static final String NAME = "[envfile] ";
    private static final String DEFAULT_PATH = "$WORKSPACE";
    private String directoryPath;

    @DataBoundConstructor
    public EnvFileBuildWrapper(String directoryPath)
    {
        this.directoryPath = Util.fixEmpty(directoryPath);
    }

    /**
     * Get the path to the files containing environment variables.
     * 
     * @return the filePath
     */
    public String getDirectoryPath()
    {
        return directoryPath;
    }

    /**
     * Get the path to the file containing environment variables.
     * 
     * @param filePath
     *            String path of the file containing environment variables
     */
    public void setDirectoryPath(String directoryPath)
    {
        this.directoryPath = directoryPath;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException
    {

        logger.fine("Reading environment variables from directry. ");

        return new EnvironmentImpl(listener);
    }

    class EnvironmentImpl extends Environment
    {

        private BuildListener listener;

        public EnvironmentImpl(BuildListener listener)
        {

            this.listener = listener;
        }

        /**
         * Get the files containing environment variables.
         * 
         * @param filePath
         *            String path of the file containing environment variables
         */
        private File[] loadFilesFromDirectory(String path, Map<String, String> currentMap)
                throws FileNotFoundException, IOException
        {
            if (path == null)
            {
                // If no value is entered for path, Workspace path will be used
                path = DEFAULT_PATH;
            }

            String resolvedPath = Util.replaceMacro(path, currentMap);
            console("Loading properties from : " + resolvedPath);
            File directory = new File(resolvedPath);
            FilenameFilter filter = new FilenameFilter()
            {

                @Override
                public boolean accept(File dir, String name)
                {
                    // only return files with extension of ".properties"
                    return name.endsWith(".properties");
                }
            };

            return directory.listFiles(filter);
        }

        private void processEnvFileMap(Map<String, String> tmpFileEnvMap, Map<String, String> currentMap)
                throws FileNotFoundException, IOException
        {
            File[] list = loadFilesFromDirectory(directoryPath, currentMap);
            for (File file : list)
            {
                // Fetch env variables from file as properties
                Properties envProps = readPropsFromFile(file);
                if (envProps != null || envProps.size() < 1)
                {

                    // Add env variables to temporary env map and file map
                    // containing new variables.
                    for (Entry<Object, Object> prop : envProps.entrySet())
                    {
                        String key = prop.getKey().toString();
                        String value = prop.getValue().toString();
                        tmpFileEnvMap.put(key, value);
                    }

                }
            }

        }

        private Properties readPropsFromFile(File propFile) throws FileNotFoundException, IOException
        {
            console("Reading : " + propFile.getName());
            Properties props = new Properties();
            FileInputStream fis = null;
            fis = new FileInputStream(propFile);
            props.load(fis);
            close(fis);
            return props;
        }

        /**
         * Helper to close environment file.
         * 
         * @param fis
         *            {@link FileInputStream} for environment file.
         */
        private void close(FileInputStream fis)
        {
            try
            {
                if (fis != null)
                {
                    fis.close();
                }
            }
            catch (Exception e)
            {
                console("Unable to close file");
                logger.warning("Unable to close environment file.");
            }
        }

        /**
         * Return Environment Map will contains our loaded env variables.
         * @param currentMap
         * @return New Map with our loaded env variables 
         *         or return current map in case we can't load our env variables
         */
        private Map<String, String> getEnvFileMap(Map<String, String> currentMap)
        {
            Map<String, String> tmpFileEnvMap = new HashMap<String, String>();

            tmpFileEnvMap.putAll(currentMap);

            try
            {
                processEnvFileMap(tmpFileEnvMap, currentMap);
                return tmpFileEnvMap;
            }
            catch (FileNotFoundException e)
            {
                console("Files not found");
            }
            catch (IOException e)
            {
                console("IO error");
            }
            catch (Exception e) {
                console("An error has occured : " + e.getMessage());
            }
            return currentMap;

        }

        private void console(String str)
        {
            listener.getLogger().println(NAME + str);
        }

        @Override
        public void buildEnvVars(Map<String, String> env)
        {
            env.putAll(getEnvFileMap(env));
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor
    {
        @Override
        public String getDisplayName()
        {
            return "Set environment variables through files in directory";
        }

        @Override
        public boolean isApplicable(AbstractProject item)
        {
            return true;
        }
    }

}
