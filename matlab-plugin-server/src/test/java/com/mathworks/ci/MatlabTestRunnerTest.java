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
  public void testRunnerName() {
    String buildName = "Run MATLAB Tests";
    Assert.assertTrue(buildName.equalsIgnoreCase(MatlabConstants.TEST_RUNNER_NAME));
  }

  @Test
  public void testRunnerDescription() {
    String buildDescription = "Run the tests in a MATLAB project.";
    Assert.assertTrue(buildDescription.equalsIgnoreCase(MatlabConstants.TEST_RUNNER_DESCRIPTION));
  }
}
