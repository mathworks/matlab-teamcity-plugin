package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import java.util.List;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class RunMatlabBuildService extends MatlabService {

  private String uniqueTmpFldrName;
  private String getTaskName(){
    final String tasks = getUserInputs().get(MatlabConstants.MATLAB_TASKS);
    return tasks == null ? "" : tasks;
  }

  @NotNull
  @Override
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    String matlabPath = getRunnerParameters().get(MatlabConstants.MATLAB_PATH);
    setRunner(getRunnerContext());

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
        "cd '" + getWorkspace().getAbsolutePath().replaceAll("'", "''") + "';\n" + "buildtool " + getTaskName();

    // Display the commands on console output for users reference
    logMessage("Generating MATLAB script with content:\n" + cmd + "\n");
    FileUtils.writeStringToFile(matlabCommandFile, cmd);
  }

  private String getCommand() {
    return "cd "+MatlabConstants.TEMP_MATLAB_FOLDER_NAME+ File.separator + uniqueTmpFldrName + ",build_" + uniqueTmpFldrName;
  }

  /**
   * Cleanup the temporary folders
   */
  private void cleanUp() throws RunBuildException {
    File tempFolder = new File(getWorkspace(), MatlabConstants.TEMP_MATLAB_FOLDER_NAME + "/" + uniqueTmpFldrName);
    try {
      FileUtils.deleteDirectory(tempFolder);
    } catch (IOException e) {
      throw new RunBuildException(e);
    }
  }

  /**
   * Executes cleanup activities after the build 
   */
  @Override
  public void afterProcessFinished() throws RunBuildException {
    cleanUp();
    super.afterProcessFinished();
  }
}
