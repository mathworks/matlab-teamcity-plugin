package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class RunMatlabBuildService extends MatlabService {

  private String uniqueTmpFldrName;

  @NotNull
  @Override
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    setRunner(getRunnerContext());

    String matlabPath = getEnVars().get(MatlabConstants.MATLAB_PATH);

    //Add MATLAB to PATH Variable
    addToPath(matlabPath);
    return new SimpleProgramCommandLine(getRunner(), getExecutable(), getBashCommands());
  }

  private List<String> getBashCommands() throws RunBuildException {
    uniqueTmpFldrName = getUniqueNameForRunnerFile().replaceAll("-", "_");
    final String uniqueCommandFileName = "build_" + uniqueTmpFldrName;

    try {
      final File uniqueScriptPath = getFilePathForUniqueFolder(uniqueTmpFldrName);
      createMatlabScriptByName(uniqueScriptPath, uniqueCommandFileName);
      return getBashCommandsToRunMatlabCommand(getCommand(), uniqueTmpFldrName);
    } catch (IOException e) {
      throw new RunBuildException(e);
    } catch (InterruptedException e) {
      throw new RunBuildException(e);
    }
  }

  private void createMatlabScriptByName(File uniqueTmpFolderPath, String uniqueScriptName) throws IOException, InterruptedException {

    // Create a new command runner script in the temp folder.
    final File matlabCommandFile = new File(uniqueTmpFolderPath, uniqueScriptName + ".m");
    final String cmd =
        "cd '" + getProjectDir().getAbsolutePath().replaceAll("'", "''") + "';\n" + "buildtool " + getEnVars()
            .get(MatlabConstants.MATLAB_TASKS);

    // Display the commands on console output for users reference
    showMessageToUser("Generating MATLAB script with content:\n" + cmd + "\n");
    FileUtils.writeStringToFile(matlabCommandFile, cmd);
  }

  public File getFilePathForUniqueFolder(String uniqueTmpFldrName) throws IOException, InterruptedException {
    File tmpDir = new File(getProjectDir(), MatlabConstants.TEMP_MATLAB_FOLDER_NAME);
    tmpDir.mkdir();
    File genscriptLocation = new File(tmpDir, uniqueTmpFldrName);
    genscriptLocation.mkdir();
    genscriptLocation.setExecutable(true);
    return genscriptLocation;
  }

  private String getCommand() {
    return "cd "+MatlabConstants.TEMP_MATLAB_FOLDER_NAME+ File.separator + uniqueTmpFldrName + ",build_" + uniqueTmpFldrName;
  }

  private void cleanUp() throws RunBuildException {
    File tempFolder = new File(getProjectDir(), MatlabConstants.TEMP_MATLAB_FOLDER_NAME + "/" + uniqueTmpFldrName);
    try {
      FileUtils.deleteDirectory(tempFolder);
    } catch (IOException e) {
      throw new RunBuildException(e);
    }
  }

  /**
   * Cleanup the temporary folders
   *
   * @throws RunBuildException
   */
  @Override
  public void afterProcessFinished() throws RunBuildException {
    cleanUp();
    super.afterProcessFinished();
  }
}
