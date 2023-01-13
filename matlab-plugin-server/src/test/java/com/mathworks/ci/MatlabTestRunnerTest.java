package com.mathworks.ci;

import org.junit.Assert;
import org.testng.annotations.Test;

public class MatlabTestRunnerTest {
  MatlabTestRunnerTest(){
  }
  @org.testng.annotations.Test
  public void testRunnerType() {
    String buildType = "matlabTestRunner";
    Assert.assertTrue(buildType.equalsIgnoreCase(MatlabConstants.TEST_RUNNER_TYPE));
  }

  @Test
  public void testBRunnerName() {
    String buildName = "Run MATLAB Tests";
    Assert.assertTrue(buildName.equalsIgnoreCase(MatlabConstants.TEST_RUNNER_NAME));
  }

  @Test
  public void testRunnerDescription() {
    String buildDescription = "Runs All MATLAB tests withing current workspace";
    Assert.assertTrue(buildDescription.equalsIgnoreCase(MatlabConstants.TEST_RUNNER_DESCRIPTION));
  }
}
