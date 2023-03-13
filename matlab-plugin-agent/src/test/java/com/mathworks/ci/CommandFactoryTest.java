package com.mathworks.ci;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;

public class CommandFactoryTest {
    RunMatlabCommandFactory buildFactory = new RunMatlabCommandFactory();

    @Test
    public void verifyBuilderType(){
        Assert.assertEquals(buildFactory.getBuildRunnerInfo().getType(), MatlabConstants.COMMAND_RUNNER_TYPE);
    }

    @Test
    public void verifyServiceType(){
        Assert.assertEquals(buildFactory.createService().getClass(), RunMatlabCommandService.class);
    }
}
