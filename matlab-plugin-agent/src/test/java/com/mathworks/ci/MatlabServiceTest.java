package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

public class MatlabServiceTest extends MatlabService {


  public void testCopyCommand() throws IOException {
    File targetFile = new File("TestFile.txt");
    FileUtils.writeStringToFile(targetFile, "test");
    String actualFile = FileUtils.readFileToString(targetFile);
    copyFileToWorkspace("TestFile.txt", targetFile);
    Assert.assertFalse(actualFile.equalsIgnoreCase(FileUtils.readFileToString(targetFile)));
  }


  public void testUniqueNameForFolder() {
    String name1 = getUniqueNameForRunnerFile();
    String name2 = getUniqueNameForRunnerFile();
    Assert.assertFalse(name1.equalsIgnoreCase(name2));
  }

  @NotNull
  @Override
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    return null;
  }
}
