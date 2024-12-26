package com.mathworks.ci;

import java.util.Map;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.BuildServiceAdapter;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;

import org.jetbrains.annotations.NotNull;

public class RunMatlabCommandService extends BuildServiceAdapter {

    private MatlabCommandRunner runner;

    public RunMatlabCommandService(MatlabCommandRunner runner) {
        this.runner = runner;
    }

    public RunMatlabCommandService() {
        this(new MatlabCommandRunner());
    }

    public String getMatlabCommand(){
        return getUserInputs().get(MatlabConstants.MATLAB_COMMAND) == null ? "" : getUserInputs().get(MatlabConstants.MATLAB_COMMAND);
    }

    @NotNull
    @Override
    public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
        // Set up runner - can't be done at construction time since we don't have access to the context
        this.runner.createUniqueFolder(getContext());

        ProgramCommandLine value;
        try {
            value = this.runner.createCommand(getContext(), getMatlabCommand());
            this.runner.copyBuildPluginsToTemp();
            this.runner.copyTestPluginsToTemp();
            // Set build environment variable
            this.runner.addEnvironmentVariable("MW_MATLAB_BUILDTOOL_DEFAULT_PLUGINS_FCN_OVERRIDE","ciplugins.teamcity.getDefaultPlugins");
        } catch (Exception e) {
            throw new RunBuildException(e);
        } 

        return value;
    }

    // Testing stubs
    public Map<String, String> getUserInputs() {
        return getRunnerContext().getRunnerParameters();
    }

    public BuildRunnerContext getContext() {
        return getRunnerContext();
    }

    public BuildProgressLogger logger() {
        return getLogger();
    }

    /**
     * Clean up temp folder
     */
    public void cleanUp() {
        this.runner.cleanUp(logger());
    }

    /**
     * Executes cleanup activities after the build 
     */
    @Override
    public void afterProcessFinished() throws RunBuildException {
        cleanUp();
        super.afterProcessFinished();
    }
}
