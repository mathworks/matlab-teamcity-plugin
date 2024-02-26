package com.mathworks.ci;

import java.util.Map;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.BuildServiceAdapter;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;

import org.jetbrains.annotations.NotNull;

public class RunMatlabBuildService extends BuildServiceAdapter {

    private MatlabCommandRunner runner;

    public RunMatlabBuildService(MatlabCommandRunner runner) {
        this.runner = runner;
    }

    public RunMatlabBuildService() {
        this(new MatlabCommandRunner());
    }

    private String getTaskName(){
        final String tasks = getUserInputs().get(MatlabConstants.MATLAB_TASKS);
        return tasks == null ? "" : tasks;
    }

    private String getBuildOptions(){
        final String buildOptions = getUserInputs().get(MatlabConstants.BUILD_OPTIONS);
        return buildOptions == null ? "" : buildOptions;
    }

    @NotNull
    @Override
    public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
        // Set up runner - can't be done at construction time since we don't have access to the context
        this.runner.createUniqueFolder(getContext());

        ProgramCommandLine value;
        try {
            String cmd = "buildtool";
            if (!getTaskName().isEmpty()){
                cmd += " " + getTaskName();
            }
            if (!getBuildOptions().isEmpty()){
                cmd += " " + getBuildOptions();
            }
            value = this.runner.createCommand(getContext(), cmd);
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
        runner.cleanUp(logger());
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
