package com.mathworks.ci;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * This is Utility class containing common methods which are shared across the MATLAB tasks
 */
public class MatlabTaskUtils {

  public static void addToPath(BuildRunnerContext runner, String matlabPath) {
    Map<String, String> envVar = runner.getBuildParameters().getEnvironmentVariables();
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

  public static String getUniqueNameForRunnerFile() {
    return RandomStringUtils.randomAlphanumeric(8);
  }

  public static SimpleProgramCommandLine getProcessToRunMatlabCommand(@NotNull BuildRunnerContext runner, String matlabCommand, String uniqueName)
      throws IOException, InterruptedException {
    File tempWorkspceFldr = new File(runner.getWorkingDirectory() + "/" + MatlabConstants.TEMP_MATLAB_FOLDER_NAME);
    if (runner.getBuild().getAgentConfiguration().getSystemInfo().isUnix() || runner.getBuild().getAgentConfiguration().getSystemInfo().isMac()) {
      final File runnerShScript = new File(tempWorkspceFldr, uniqueName + "/run_matlab_command.sh");
      final List<String> args = new ArrayList<String>();

      args.add(runnerShScript.getPath());
      args.add(matlabCommand);

      copyFileInWorkspace(MatlabConstants.SHELL_RUNNER_SCRIPT, new File(runnerShScript.getPath()));
      return new SimpleProgramCommandLine(runner, "/bin/bash", args);

    } else {
      final File runnerScriptName = new File(tempWorkspceFldr, uniqueName + "\\run_matlab_command.bat");
      final List<String> args = new ArrayList<String>();
      args.add("/C");
      args.add(runnerScriptName.getPath());
      args.add(matlabCommand);
      copyFileInWorkspace(MatlabConstants.BAT_RUNNER_SCRIPT, new File(runnerScriptName.getPath()));
      return new SimpleProgramCommandLine(runner, "cmd.exe", args);
    }

  }

  public static void copyFileInWorkspace(String sourceFile, File targetWorkspace) throws IOException {
    InputStream is = MatlabTaskUtils.class.getClassLoader().getResourceAsStream(sourceFile);
    java.nio.file.Files.copy(is, targetWorkspace.toPath(), REPLACE_EXISTING);
    targetWorkspace.setExecutable(true);
    IOUtils.closeQuietly(is);
  }

}
