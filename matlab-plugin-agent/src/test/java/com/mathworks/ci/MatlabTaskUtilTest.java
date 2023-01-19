package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.testng.annotations.Test;

public class MatlabTaskUtilTest extends RunMatlabTestsService {

  public MatlabTaskUtilTest() {
  }

  @Test
  public void testCopyCommand() throws IOException {
    File targetFile = new File("TestFile.txt");
    FileUtils.writeStringToFile(targetFile, "test");
    String actualFile = FileUtils.readFileToString(targetFile);
    MatlabTaskUtils.copyFileInWorkspace("TestFile.txt", targetFile);
    Assert.assertFalse(actualFile.equalsIgnoreCase(FileUtils.readFileToString(targetFile)));
  }

  @Test
  public void testUniqueNameForFolder() {
    String name1 = MatlabTaskUtils.getUniqueNameForRunnerFile();
    String name2 = MatlabTaskUtils.getUniqueNameForRunnerFile();
    Assert.assertFalse(name1.equalsIgnoreCase(name2));
  }
}
