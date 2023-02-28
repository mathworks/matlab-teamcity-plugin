package com.mathworks.ci;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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

  public void setRunner(BuildRunnerContext runner){
    this.runner = runner;
  }

  public Map<String, String> getUpdatedPath(String matlabPath) {
    Map<String, String> pathMap = new HashMap<>();;
    Map<String, String> envVar = getRunParameters();

    for (String name : envVar.keySet()) {
      if (name.equalsIgnoreCase("Path")) {
        String path = envVar.get(name);
        if (isWindows()) {
          path = matlabPath + File.separator + "bin;" + path;
        } else {
          path = matlabPath + File.separator + "bin:" + path;
        }
        getLogger().progressMessage(path);
        getLogger().progressMessage(name);
        pathMap.put(name, path);
      }
    }
    return pathMap;
  }

  public Map<String, String> getRunParameters(){
    return getRunner().getBuildParameters().getEnvironmentVariables();
  }

  public Map<String, String> getEnVars(){
    return getRunnerParameters();
  }

  public void addToPath(String matlabPath){
    Map<String, String> path = getUpdatedPath(matlabPath);
    String name = path.entrySet().iterator().next().getKey();
    String value = path.entrySet().iterator().next().getValue();
    runner.addEnvironmentVariable(name, value);
  }

  public String getUniqueNameForRunnerFile() {
    return RandomStringUtils.randomAlphanumeric(8);
  }

  public List<String> getCommandsToRunMatlabCommand(String matlabCommand, String uniqueName)
      throws IOException, InterruptedException {
    final List<String> args = new ArrayList<String>();
    File tempWorkspceFldr = new File(getProjectDir() + "/" + MatlabConstants.TEMP_MATLAB_FOLDER_NAME);
    if (!isWindows()) {
      final File runnerShScript = new File(tempWorkspceFldr, uniqueName + "/run_matlab_command.sh");

      args.add(runnerShScript.getPath());
      args.add(matlabCommand);

      copyFileToWorkspace(MatlabConstants.SHELL_RUNNER_SCRIPT, new File(runnerShScript.getPath()));

    } else {
      final File runnerScriptName = new File(tempWorkspceFldr, uniqueName + "\\run_matlab_command.bat");

      args.add("/C");
      args.add(runnerScriptName.getPath());
      args.add(matlabCommand);
      copyFileToWorkspace(MatlabConstants.BAT_RUNNER_SCRIPT, new File(runnerScriptName.getPath()));

    }
    return args;
  }

  public String getExecutable(){
    if(isWindows()){
      return "cmd.exe";
    }
    return "/bin/bash";
  }

  public Boolean isWindows(){
    if(getRunner().getBuild().getAgentConfiguration().getSystemInfo().isWindows()){
      return true;
    }
    return false;
  }


  public File getProjectDir(){
    return getRunner().getWorkingDirectory();
  }

  public void showMessageToUser(String msg){
    getLogger().progressMessage(msg);
  }

  public static void copyFileToWorkspace(String sourceFile, File targetWorkspace) throws IOException {
    InputStream is = MatlabService.class.getClassLoader().getResourceAsStream(sourceFile);
    java.nio.file.Files.copy(is, targetWorkspace.toPath(), REPLACE_EXISTING);
    targetWorkspace.setExecutable(true);
    IOUtils.closeQuietly(is);
  }
}
