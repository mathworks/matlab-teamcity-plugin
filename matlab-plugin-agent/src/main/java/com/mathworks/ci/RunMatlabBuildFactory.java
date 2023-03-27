package com.mathworks.ci;

import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import org.jetbrains.annotations.NotNull;

public class RunMatlabBuildFactory implements CommandLineBuildServiceFactory {

  @NotNull
  @Override
  public CommandLineBuildService createService() {
    return new RunMatlabBuildService();
  }

  @NotNull
  @Override
  public AgentBuildRunnerInfo getBuildRunnerInfo() {
    return new AgentBuildRunnerInfo() {
      @NotNull
      @Override
      public String getType() {
        return MatlabConstants.BUILD_RUNNER_TYPE;
      }

      @Override
      public boolean canRun(@NotNull BuildAgentConfiguration buildAgentConfiguration) {
        return true;
      }
    };
  }
}