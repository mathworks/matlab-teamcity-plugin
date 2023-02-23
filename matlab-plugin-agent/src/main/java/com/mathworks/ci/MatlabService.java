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
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
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

  public void setRunner(BuildRunnerContext runner){
    this.runner = runner;
  }

  public void addToPath(String matlabPath) {
    Map<String, String> envVar = getRunner().getBuildParameters().getEnvironmentVariables();
    for (String name : envVar.keySet()) {
      if (name.equalsIgnoreCase("Path")) {
        String path = envVar.get(name);
        if (runner.getBuild().getAgentConfiguration().getSystemInfo().isWindows()) {
          path = matlabPath + File.separator + "bin;" + path;
        } else {
          path = matlabPath + File.separator + "bin:" + path;
        }
        runner.addEnvironmentVariable(name, path);
      }
    }
  }

  public String getUniqueNameForRunnerFile() {
    return RandomStringUtils.randomAlphanumeric(8);
  }

  public SimpleProgramCommandLine getProcessToRunMatlabCommand(String matlabCommand, String uniqueName)
      throws IOException, InterruptedException {
    File tempWorkspceFldr = new File(getRunner().getWorkingDirectory() + "/" + MatlabConstants.TEMP_MATLAB_FOLDER_NAME);
    if (getRunner().getBuild().getAgentConfiguration().getSystemInfo().isUnix() || getRunner().getBuild().getAgentConfiguration().getSystemInfo().isMac()) {
      final File runnerShScript = new File(tempWorkspceFldr, uniqueName + "/run_matlab_command.sh");
      final List<String> args = new ArrayList<String>();

      args.add(runnerShScript.getPath());
      args.add(matlabCommand);

      copyFileToWorkspace(MatlabConstants.SHELL_RUNNER_SCRIPT, new File(runnerShScript.getPath()));
      return new SimpleProgramCommandLine(getRunner(), "/bin/bash", args);

    } else {
      final File runnerScriptName = new File(tempWorkspceFldr, uniqueName + "\\run_matlab_command.bat");
      final List<String> args = new ArrayList<String>();
      args.add("/C");
      args.add(runnerScriptName.getPath());
      args.add(matlabCommand);
      copyFileToWorkspace(MatlabConstants.BAT_RUNNER_SCRIPT, new File(runnerScriptName.getPath()));
      return new SimpleProgramCommandLine(getRunner(), "cmd.exe", args);
    }

  }

  public static void copyFileToWorkspace(String sourceFile, File targetWorkspace) throws IOException {
    InputStream is = MatlabService.class.getClassLoader().getResourceAsStream(sourceFile);
    java.nio.file.Files.copy(is, targetWorkspace.toPath(), REPLACE_EXISTING);
    targetWorkspace.setExecutable(true);
    IOUtils.closeQuietly(is);
  }
}
