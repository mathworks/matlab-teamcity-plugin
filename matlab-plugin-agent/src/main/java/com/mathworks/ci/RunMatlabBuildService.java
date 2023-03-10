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

  @NotNull
  @Override
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    String matlabPath = getRunnerParameters().get(MatlabConstants.MATLAB_PATH);
    setRunner(getRunnerContext());

    //Add MATLAB to PATH Variable
    addToPath(matlabPath);
    return new SimpleProgramCommandLine(getRunner(), getExecutable(), getBashCommands());
  }

  public List<String> getBashCommands() throws RunBuildException {
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
        "cd '" + getRunner().getWorkingDirectory().getAbsolutePath().replaceAll("'", "''") + "';\n" + "buildtool " + getRunner().getRunnerParameters()
            .get(MatlabConstants.MATLAB_TASKS);

    // Display the commands on console output for users reference
    logMessage("Generating MATLAB script with content:\n" + cmd + "\n");
    FileUtils.writeStringToFile(matlabCommandFile, cmd);
  }


  private String getCommand() {
    return "cd "+MatlabConstants.TEMP_MATLAB_FOLDER_NAME+"/" + uniqueTmpFldrName + ",build_" + uniqueTmpFldrName;
  }

  /**
   * Cleanup the temporary folders
   *
   * @throws RunBuildException
   */
  @Override
  public void afterProcessFinished() throws RunBuildException {
    File tempFolder = new File(getRunnerContext().getWorkingDirectory(), MatlabConstants.TEMP_MATLAB_FOLDER_NAME + "/" + uniqueTmpFldrName);
    try {
      FileUtils.deleteDirectory(tempFolder);
    } catch (IOException e) {
      throw new RunBuildException(e);
    }
    super.afterProcessFinished();
  }
}
