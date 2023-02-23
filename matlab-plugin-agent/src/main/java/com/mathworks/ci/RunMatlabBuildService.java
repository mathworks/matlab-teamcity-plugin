package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
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
    SimpleProgramCommandLine cmdExecutor = null;
    setRunner(getRunnerContext());

    String matlabPath = getRunnerParameters().get(MatlabConstants.MATLAB_PATH);

    //Add MATLAB to PATH Variable
    addToPath(matlabPath);
    uniqueTmpFldrName = getUniqueNameForRunnerFile().replaceAll("-", "_");
    final String uniqueCommandFileName = "build_" + uniqueTmpFldrName;

    try {
      final File uniqueScriptPath = getFilePathForUniqueFolder(getRunnerContext(), uniqueTmpFldrName);
      createMatlabScriptByName(uniqueScriptPath, uniqueCommandFileName);
      final BuildRunnerContext runner = getRunnerContext();
      cmdExecutor = getProcessToRunMatlabCommand(getCommand(), uniqueTmpFldrName);
    } catch (IOException e) {
      throw new RunBuildException(e);
    } catch (InterruptedException e) {
      throw new RunBuildException(e);
    }
    return cmdExecutor;
  }

  private void createMatlabScriptByName(File uniqueTmpFolderPath, String uniqueScriptName) throws IOException, InterruptedException {

    // Create a new command runner script in the temp folder.
    final File matlabCommandFile = new File(uniqueTmpFolderPath, uniqueScriptName + ".m");
    final String cmd =
        "cd '" + getRunner().getWorkingDirectory().getAbsolutePath().replaceAll("'", "''") + "';\n" + "buildtool " + getRunner().getRunnerParameters()
            .get(MatlabConstants.MATLAB_TASKS);

    // Display the commands on console output for users reference
    getLogger().progressMessage("Generating MATLAB script with content:\n" + cmd + "\n");
    FileUtils.writeStringToFile(matlabCommandFile, cmd);
  }

  public File getFilePathForUniqueFolder(BuildRunnerContext runner, String uniqueTmpFldrName) throws IOException, InterruptedException {
    File tmpDir = new File(getRunner().getWorkingDirectory(), MatlabConstants.TEMP_MATLAB_FOLDER_NAME);
    tmpDir.mkdir();
    File genscriptLocation = new File(tmpDir, uniqueTmpFldrName);
    genscriptLocation.mkdir();
    genscriptLocation.setExecutable(true);
    return genscriptLocation;
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
