package com.mathworks.ci;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
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
import net.lingala.zip4j.ZipFile;

public class MatlabCommandRunner {
    private File tempDirectory;
    private Map<String,String> additionalEnvVars;

    public MatlabCommandRunner() {
        this.additionalEnvVars = new HashMap<String,String>();
    }

    public void setTempDirectory(File dir) {
        this.tempDirectory = dir;
    }

    public File getTempDirectory() {
        return this.tempDirectory;
    }

    // Main entry point
    public ProgramCommandLine createCommand(BuildRunnerContext runnerContext, String matlabCommand) throws IOException {
        // Get executable
        final String executable = copyExecutable(runnerContext);

        // Generate command
        final List<String> command = generateCommandArgs(runnerContext, matlabCommand);

        String matlabPath = runnerContext.getRunnerParameters().get(MatlabConstants.MATLAB_PATH);

        //Add MATLAB to PATH Variable
        addToPath(runnerContext, matlabPath);

        //Add custom environment variables
        for (Map.Entry<String, String> envVar : additionalEnvVars.entrySet()) {
            String key = envVar.getKey();
            String value = envVar.getValue();

            runnerContext.addEnvironmentVariable(key, value);
        }

        return new SimpleProgramCommandLine(runnerContext, executable, command); 
    }

    // Determines script location + final command to pass to run-matlab-command
    private List<String> generateCommandArgs(BuildRunnerContext runnerContext, String command) throws IOException {
        List<String> commandArgs = new ArrayList<String>();

        final File uniqueCommandFile = new File(this.tempDirectory, "matlab_" + this.tempDirectory.getName());
        String commandToExecute = "setenv('MW_ORIG_WORKING_FOLDER', cd('"
            + this.tempDirectory.getAbsolutePath()
            + "'));" + uniqueCommandFile.getName();

        // Create script
        createMatlabScriptByName(runnerContext, command, uniqueCommandFile.getName());
        commandArgs.add(commandToExecute);

        // Handle startup options
        String opts = runnerContext.getRunnerParameters().get(MatlabConstants.STARTUP_OPTIONS);
        if (opts != null) {
            commandArgs.addAll(Arrays.asList(opts.split(" ")));
        }
        commandArgs.removeIf(s -> s.isEmpty());

        return commandArgs;
    }

    // Creates script file at given location with command as content
    private void createMatlabScriptByName(BuildRunnerContext runnerContext, String command, String uniqueScriptName) throws IOException {
        final File matlabCommandFile = 
            new File(this.tempDirectory, uniqueScriptName + ".m");
        final String matlabCommandFileContent =
            "cd(getenv('MW_ORIG_WORKING_FOLDER'));\n" + command;

        FileUtils.writeStringToFile(matlabCommandFile, matlabCommandFileContent);
    }

    // Copy executable to the correct location based on platform
    private String copyExecutable(BuildRunnerContext runnerContext) throws IOException {
        String executable;
        if (isWindows(runnerContext)) {
            final File runnerExe = new File(this.tempDirectory, "run-matlab-command.exe");

            executable = runnerExe.getPath();

            copyFileToWorkspace(MatlabConstants.RUN_EXE_WIN, new File(runnerExe.getPath()));
        } else if (isMac(runnerContext)) {
            final File runnerExe = new File(this.tempDirectory, "run-matlab-command");

            executable = runnerExe.getPath();

            if (System.getProperty("os.arch").equals("aarch64")) {
                copyFileToWorkspace(MatlabConstants.RUN_EXE_MACA, new File(runnerExe.getPath()));
            } else {
                copyFileToWorkspace(MatlabConstants.RUN_EXE_MACI, new File(runnerExe.getPath()));
            }
        } else {
            final File runnerExe = new File(this.tempDirectory, "run-matlab-command");

            executable = runnerExe.getPath();

            copyFileToWorkspace(MatlabConstants.RUN_EXE_LINUX, new File(runnerExe.getPath()));
        }

        return executable;
    }

    public void unzipToTempDir(String zipName) throws IOException {
        // Copy zip to tempDirectory
        File zipFileLocation = new File(tempDirectory, zipName);
        copyFileToWorkspace(zipName, zipFileLocation);

        // Unzip
        ZipFile zipFile = new ZipFile(zipFileLocation);
        zipFile.extractAll(tempDirectory.toString());
    }
    
    public void addEnvironmentVariable(String key, String value) {
        additionalEnvVars.put(key, value);
    }

    public void copyBuildPluginsToTemp() throws IOException{
        copyPluginsToTempDir(MatlabConstants.DEFAULT_PLUGIN);
        copyPluginsToTempDir(MatlabConstants.BUILD_VISUALIZATION_PLUGIN);
    }

    public void copyTestPluginsToTemp() throws IOException{
        copyPluginsToTempDir(MatlabConstants.TEST_VISUALIZATION_PLUGIN);
        copyPluginsToTempDir(MatlabConstants.TEST_VISUALIZATION_PLUGIN_SERVICE);
    }

    public void copyPluginsToTempDir(String pluginName) throws IOException{
        File pluginFileLocation = new File(this.tempDirectory, pluginName);

        // Ensure the directory structure exists
        File parentDir = pluginFileLocation.getParentFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directories: " + parentDir);
            }
        }

        copyFileToWorkspace(pluginName, pluginFileLocation);
    }

    public void copyFileToWorkspace(String sourceFile, File targetFile) throws IOException {
        InputStream is = MatlabCommandRunner.class.getClassLoader().getResourceAsStream(sourceFile);
        java.nio.file.Files.copy(is, targetFile.toPath(), REPLACE_EXISTING);

        targetFile.setReadable(true, true);
        targetFile.setWritable(true);
        targetFile.setExecutable(true);

        IOUtils.closeQuietly(is);
    }

    public void createUniqueFolder(BuildRunnerContext runnerContext) {
        final String uniqueTmpFldrName = getUniqueNameForRunnerFile().replaceAll("-", "_");

        File tmpDir = new File(runnerContext.getWorkingDirectory(), MatlabConstants.TEMP_MATLAB_FOLDER_NAME);
        tmpDir.mkdir();
        File genscriptLocation = new File(tmpDir, uniqueTmpFldrName);
        genscriptLocation.mkdir();
        genscriptLocation.setExecutable(true);
        setTempDirectory(genscriptLocation);
    }

    private void addToPath(BuildRunnerContext runnerContext, String matlabPath) {
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

    private String getUniqueNameForRunnerFile() {
        return RandomStringUtils.randomAlphanumeric(8);
    }

    private Boolean isWindows(BuildRunnerContext runnerContext) {
        return runnerContext.getBuild().getAgentConfiguration().getSystemInfo().isWindows();
    }

    private Boolean isMac(BuildRunnerContext runnerContext) {
        return runnerContext.getBuild().getAgentConfiguration().getSystemInfo().isMac();
    }

    /**
     * Cleanup the temporary folders
     */
    public void cleanUp(BuildProgressLogger logger) {
        try {
            FileUtils.cleanDirectory(this.tempDirectory);
            FileUtils.deleteDirectory(this.tempDirectory);
        } catch (Exception e) {
            logger.exception(e);
        }
    }
}
