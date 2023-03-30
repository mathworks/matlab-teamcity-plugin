package com.mathworks.ci;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;

public class TestsFactoryTest {
    RunMatlabTestsFactory testsFactory = new RunMatlabTestsFactory();

    @Test
    public void verifyBuilderType(){
        Assert.assertEquals(testsFactory.getBuildRunnerInfo().getType(), MatlabConstants.TEST_RUNNER_TYPE);
    }

    @Test
    public void verifyServiceType(){
        Assert.assertEquals(testsFactory.createService().getClass(), RunMatlabTestsService.class);
    }
}
