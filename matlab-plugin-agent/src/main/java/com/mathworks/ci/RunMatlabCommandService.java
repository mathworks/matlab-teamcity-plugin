package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import java.util.List;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class RunMatlabCommandService extends MatlabService {

  private String uniqueTmpFldrName;

  public String getMatlabCommand(){
    return getRunnerParameters().get(MatlabConstants.MATLAB_COMMAND);
  }

  @NotNull
  @Override
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    String matlabPath = getRunnerParameters().get(MatlabConstants.MATLAB_PATH);
    setRunner(getRunnerContext());

    //Add MATLAB into PATH Variable
    addToPath(matlabPath);
    return new SimpleProgramCommandLine(getRunner(), getExecutable(), getBashCommands());
  }

  public List<String> getBashCommands() throws RunBuildException {
    uniqueTmpFldrName = getUniqueNameForRunnerFile().replaceAll("-", "_");

    final String uniqueCommandFileName = "cmd_" + uniqueTmpFldrName;

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


  private void createMatlabScriptByName(File uniqeTmpFolderPath, String uniqueScriptName) throws IOException, InterruptedException {

    // Create a new command runner script in the temp folder.
    final File matlabCommandFile = new File(uniqeTmpFolderPath, uniqueScriptName + ".m");
    final String cmd =
        "cd '" + getWorkspace().getAbsolutePath().replaceAll("'", "''") + "';\n" + getMatlabCommand();

    // Display the commands on console output for users reference
    logMessage("Generating MATLAB script with content:\n" + cmd + "\n");
    FileUtils.writeStringToFile(matlabCommandFile, cmd);
  }

  private String getCommand() {
    return "cd .matlab" +File.separator+ uniqueTmpFldrName + ",cmd_" + uniqueTmpFldrName;
  }

  private void cleanUp(){
    File tempFolder = new File(getWorkspace(), ".matlab/" + uniqueTmpFldrName);
    try {
      FileUtils.deleteDirectory(tempFolder);
    } catch (IOException e) {
      throw new RuntimeException(e);
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
