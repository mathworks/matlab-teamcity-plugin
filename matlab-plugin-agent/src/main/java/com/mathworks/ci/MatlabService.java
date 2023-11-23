package com.mathworks.ci;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.BuildServiceAdapter;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

public abstract class MatlabService extends BuildServiceAdapter {

  private BuildRunnerContext runner;

  @NotNull
  public abstract ProgramCommandLine makeProgramCommandLine() throws RunBuildException;

  public BuildRunnerContext getRunner() {
    return this.runner;
  }

  public void setRunner(BuildRunnerContext runner) {
    this.runner = runner;
  }

  //get build step inputs
  public Map<String, String> getUserInputs() {
    return getRunner().getRunnerParameters();
  }

  public void addToPath(String matlabPath) {
    Map<String, String> envVar = getRunner().getBuildParameters().getEnvironmentVariables();

    for (String name : envVar.keySet()) {
      if (name.equalsIgnoreCase("Path")) {
        String path = envVar.get(name);
        path = matlabPath + File.separator + "bin" + File.pathSeparator + path;
        runner.addEnvironmentVariable(name, path);
        break;
      }
    }
  }

  public String getUniqueNameForRunnerFile() {
    return RandomStringUtils.randomAlphanumeric(8);
  }

  public List<String> getBashCommandsToRunMatlabCommand(String matlabCommand, String uniqueName) throws IOException, InterruptedException {
    final List<String> args = new ArrayList<String>();
    File tempWorkspceFldr = new File(getWorkspace() + File.separator + MatlabConstants.TEMP_MATLAB_FOLDER_NAME);
    if (!isWindows()) {
      final File runnerShScript = new File(tempWorkspceFldr, uniqueName + "/run_matlab_command.sh");

      args.add(runnerShScript.getPath());
      args.add(matlabCommand);

      copyFileToWorkspace(MatlabConstants.SHELL_RUNNER_SCRIPT, new File(runnerShScript.getPath()));

    } else {
      final File runnerScriptName = new File(tempWorkspceFldr, uniqueName + "\\run_matlab_command.bat");
      args.add(runnerScriptName.getPath());
      args.add(matlabCommand);
      copyFileToWorkspace(MatlabConstants.BAT_RUNNER_SCRIPT, new File(runnerScriptName.getPath()));
    }
    return args;
  }

  public File getFilePathForUniqueFolder(String uniqueTmpFldrName) throws IOException, InterruptedException {
    File tmpDir = new File(getWorkspace(), MatlabConstants.TEMP_MATLAB_FOLDER_NAME);
    tmpDir.mkdir();
    File genscriptLocation = new File(tmpDir, uniqueTmpFldrName);
    genscriptLocation.mkdir();
    genscriptLocation.setExecutable(true);
    return genscriptLocation;
  }

  public String getExecutable() {
    if (isWindows()) {
      return "cmd.exe";
    }
    return "/bin/bash";
  }

  public Boolean isWindows() {
    return getRunner().getBuild().getAgentConfiguration().getSystemInfo().isWindows();
  }


  public File getWorkspace() {
    return getRunner().getWorkingDirectory();
  }

  public void logMessage(String msg) {
    getLogger().progressMessage(msg);
  }

  public static void copyFileToWorkspace(String sourceFile, File targetWorkspace) throws IOException {
    InputStream is = MatlabService.class.getClassLoader().getResourceAsStream(sourceFile);
    java.nio.file.Files.copy(is, targetWorkspace.toPath(), REPLACE_EXISTING);
    targetWorkspace.setExecutable(true);
    IOUtils.closeQuietly(is);
  }
}
