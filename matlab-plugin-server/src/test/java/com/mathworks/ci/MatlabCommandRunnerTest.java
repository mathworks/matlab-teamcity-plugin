package com.mathworks.ci;

import org.junit.Assert;
import org.testng.annotations.Test;

public class MatlabCommandRunnerTest {

  MatlabCommandRunnerTest() {
  }

  @org.testng.annotations.Test
  public void testCommanddRunnerType() {
    String buildType = "matlabCommandRunner";
    Assert.assertTrue(buildType.equalsIgnoreCase(MatlabConstants.COMMAND_RUNNER_TYPE));
  }

  @Test
  public void testBCommandRunnerName() {
    String buildName = "Run MATLAB Command";
    Assert.assertTrue(buildName.equalsIgnoreCase(MatlabConstants.COMMAND_RUNNER_NAME));
  }

  @Test
  public void testCommandRunnerDescription() {
    String buildDescription = "Runs specific MATLAB command or MATLAB script";
    Assert.assertTrue(buildDescription.equalsIgnoreCase(MatlabConstants.COMMAND_RUNNER_DESCRIPTION));
  }

}
