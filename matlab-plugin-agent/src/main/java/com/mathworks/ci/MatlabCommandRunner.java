package com.mathworks.ci;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

public class MatlabCommandRunner {
    private BuildRunnerContext runnerContext;
    private File tempDirectory;

    public void setRunnerContext(BuildRunnerContext context) {
        this.runnerContext = context;
    }

    public void setTempDirectory(File dir) {
        this.tempDirectory = dir;
    }

    public File getTempDirectory() {
        return tempDirectory;
    }

    // Main entry point
    public ProgramCommandLine createCommand(String matlabCommand) throws IOException {
        // Get executable
        final String executable = copyExecutable();

        // Generate command
        final List<String> command = generateCommand(matlabCommand);

        String matlabPath = runnerContext.getRunnerParameters().get(MatlabConstants.MATLAB_PATH);

        //Add MATLAB to PATH Variable
        addToPath(matlabPath);

        return new SimpleProgramCommandLine(runnerContext, executable, command); 
    }

    // Determines script location + final command to pass to run-matlab-command
    public List<String> generateCommand(String command) throws IOException {
        List<String> commandArgs = new ArrayList<String>();

        final File uniqueCommandFile = new File(tempDirectory, "matlab_" + tempDirectory.getName());
        String commandToExecute = "addpath('" + tempDirectory.getPath().replaceAll("'", ";;") + "');" + uniqueCommandFile.getName();


        // Create script
        createMatlabScriptByName(command, uniqueCommandFile.getName());
        commandArgs.add(commandToExecute);

        // Handle startup options
        String opts = runnerContext.getRunnerParameters().get(MatlabConstants.STARTUP_OPTIONS);
        if (opts != null) {
            commandArgs.addAll(Arrays.asList(opts.split(" ")));
        }

        return commandArgs;
    }

    // Creates script file at given location with command as content
    private void createMatlabScriptByName(String command, String uniqueScriptName) throws IOException {
        final File matlabCommandFile = 
            new File(tempDirectory, uniqueScriptName + ".m");
        final String matlabCommandFileContent =
            "cd '" + runnerContext.getWorkingDirectory().getAbsolutePath().replaceAll("'", "''") + "';\n" + command;

        FileUtils.writeStringToFile(matlabCommandFile, matlabCommandFileContent);
    }

    // Copy executable to the correct location based on platform
    public String copyExecutable() throws IOException {
        String executable;
        if (isWindows()) {
            final File runnerExe = new File(tempDirectory, "run-matlab-command.exe");

            executable = runnerExe.getPath();

            copyFileToWorkspace(MatlabConstants.RUN_EXE_WIN, new File(runnerExe.getPath()));
        } else if (isMac()) {
            final File runnerExe = new File(tempDirectory, "run-matlab-command");

            executable = runnerExe.getPath();

            copyFileToWorkspace(MatlabConstants.RUN_EXE_MAC, new File(runnerExe.getPath()));
        } else {
            final File runnerExe = new File(tempDirectory, "run-matlab-command");

            executable = runnerExe.getPath();

            copyFileToWorkspace(MatlabConstants.RUN_EXE_LINUX, new File(runnerExe.getPath()));
        }

        return executable;
    }

    public void copyFileToWorkspace(String sourceFile, File targetFile) throws IOException {
        InputStream is = MatlabCommandRunner.class.getClassLoader().getResourceAsStream(sourceFile);
        java.nio.file.Files.copy(is, targetFile.toPath(), REPLACE_EXISTING);
        targetFile.setExecutable(true);
        IOUtils.closeQuietly(is);
    }

    public void createUniqueFolder() {
        final String uniqueTmpFldrName = getUniqueNameForRunnerFile().replaceAll("-", "_");

        File tmpDir = new File(runnerContext.getWorkingDirectory(), MatlabConstants.TEMP_MATLAB_FOLDER_NAME);
        tmpDir.mkdir();
        File genscriptLocation = new File(tmpDir, uniqueTmpFldrName);
        genscriptLocation.mkdir();
        genscriptLocation.setExecutable(true);
        this.tempDirectory =  genscriptLocation;
    }

    public void addToPath(String matlabPath) {
        Map<String, String> envVar = runnerContext.getBuildParameters().getEnvironmentVariables();

        for (String name : envVar.keySet()) {
            if (name.equalsIgnoreCase("Path")) {
                String path = envVar.get(name);
                path = matlabPath + File.separator + "bin" + File.pathSeparator + path;
                runnerContext.addEnvironmentVariable(name, path);
                break;
            }
        }
    }

    public String getUniqueNameForRunnerFile() {
        return RandomStringUtils.randomAlphanumeric(8);
    }

    public Boolean isWindows() {
        return runnerContext.getBuild().getAgentConfiguration().getSystemInfo().isWindows();
    }

    public Boolean isMac() {
        return runnerContext.getBuild().getAgentConfiguration().getSystemInfo().isMac();
    }

    /**
     * Cleanup the temporary folders
     */
    public void cleanUp(BuildProgressLogger logger) {
        try {
            FileUtils.cleanDirectory(tempDirectory);
            FileUtils.deleteDirectory(tempDirectory);
        } catch (Exception e) {
            logger.exception(e);
        }
    }
}
