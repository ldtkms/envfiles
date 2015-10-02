package hudson.plugins.envfiles;

import java.io.IOException;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.envfiles.*;

public class EnvFilesRunListener extends RunListener<AbstractBuild>
{
    /**
     * {@link Extension} needs parameterless constructor.
     */
    public EnvFilesRunListener()
    {
        super(AbstractBuild.class);
    }

    /**
     * {@inheritDoc}
     * 
     * Adds {@link DownstreamBuildViewAction} to the build. Do this in
     * <tt>onCompleted</tt> affected.
     */
    @Override
    public void onStarted(AbstractBuild r, TaskListener listener)
    {
        try
        {
            r.getEnvironment(listener).putAll(EnvFileBuildWrapper.envMap);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
