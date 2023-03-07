package com.mathworks.ci;

import jetbrains.buildServer.agent.BuildAgentConfiguration;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;

public class BuildFactoryTest {
    RunMatlabBuildFactory buildFactory = new RunMatlabBuildFactory();

    @Test
    public void verifyBuilderType(){
        Assert.assertEquals(buildFactory.getBuildRunnerInfo().getType(), MatlabConstants.BUILD_RUNNER_TYPE);
    }

    @Test
    public void verifyCanRun(){
        Assert.assertTrue(buildFactory.getBuildRunnerInfo().canRun(any()));
    }

    @Test
    public void verifyServiceType(){
        Assert.assertEquals(buildFactory.createService().getClass(), RunMatlabBuildService.class);
    }
}
