package com.mathworks.ci;

import org.junit.Assert;
import org.testng.annotations.Test;

public class MatlabBuildRunnerTest {

  public MatlabBuildRunnerTest() {

  }

  @org.testng.annotations.Test
  public void testBuildRunnerType() {
    String buildType = "matlabBuildRunner";
    Assert.assertTrue(buildType.equalsIgnoreCase(MatlabConstants.BUILD_RUNNER_TYPE));
  }

  @Test
  public void testBuildRunnerName() {
    String buildName = "Run MATLAB Build";
    Assert.assertTrue(buildName.equalsIgnoreCase(MatlabConstants.BUILD_RUNNER_NAME));
  }

  @Test
  public void testBuildRunnerDescription() {
    String buildDescription = "Runs MATLAB build tasks.";
    Assert.assertTrue(buildDescription.equalsIgnoreCase(MatlabConstants.BUILD_RUNNER_DESCRIPTION));
  }

}
