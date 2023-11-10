package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildAgentSystemInfo;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildParametersMap;

import org.apache.commons.io.FileUtils;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import org.mockito.Mockito;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MatlabCommandRunnerTest {
    BuildRunnerContext context;
    AgentRunningBuild agent;
    BuildAgentConfiguration agentConfig;
    BuildAgentSystemInfo agentSysInfo;
    BuildParametersMap buildParamsMap;

    MatlabCommandRunner runner;

    File currDir;
    File tempDir;

    Map<String, String> params;

    @BeforeTest
    public void mockSetUp() throws RunBuildException {
        String systemTempFolder = System.getProperty("java.io.tmpdir");

        currDir = new File(systemTempFolder, "tmpBuildProjectWorkspace");
        currDir.mkdir();

        params = new HashMap<>();
        params.put("MatlabPathKey", "/path/to/matlab");

        Map<String, String> envVars = new HashMap<>();
        envVars.put("Path", "");

        // Mock out BuildRunnerContext
        context = mock(BuildRunnerContext.class);
        agent = mock(AgentRunningBuild.class);
        agentConfig = mock(BuildAgentConfiguration.class);
        agentSysInfo = mock(BuildAgentSystemInfo.class);
        buildParamsMap = mock(BuildParametersMap.class);

        when(context.getWorkingDirectory()).thenReturn(currDir);
        when(context.getRunnerParameters()).thenReturn(params);
        when(context.getBuild()).thenReturn(agent);
        when(context.getBuildParameters()).thenReturn(buildParamsMap);
        when(agent.getAgentConfiguration()).thenReturn(agentConfig);
        when(agentConfig.getSystemInfo()).thenReturn(agentSysInfo);
        when(buildParamsMap.getEnvironmentVariables()).thenReturn(envVars);

        if (System.getProperty("os.name").startsWith("Windows")) {
            when(agentSysInfo.isWindows()).thenReturn(true);
            when(agentSysInfo.isMac()).thenReturn(false);
        } else {
            when(agentSysInfo.isWindows()).thenReturn(false);
            when(agentSysInfo.isMac()).thenReturn(false);
        }

        runner = new MatlabCommandRunner();
        runner.setRunnerContext(context);
    }

    @AfterTest
    public void testTearDown() throws IOException {
        FileUtils.deleteDirectory(currDir);
    }

    // temp folder creation
    @Test
    public void verifyTempFolderCreation() {
        // Create temp directory
        runner.createUniqueFolder();

        File expected = new File(currDir, ".matlab" + File.separator + runner.getTempDirectory().getName());

        Assert.assertTrue(expected.exists());
    }

    // executable creation
    @Test
    public void verifyExecutionCreation() throws IOException {
        // Set temp dir
        runner.setTempDirectory(currDir);

        // Copy executable
        String actual = runner.copyExecutable();
        
        // If windows
        File expected;
        if (runner.isWindows()) {
            expected = new File(currDir, "run-matlab-command.exe");
        } else {
            expected = new File(currDir, "run-matlab-command");
        }

        Assert.assertTrue(expected.exists());
    }

    // matlab script creation + command generation
    @Test
    public void verifyScriptCreated() throws IOException {
        runner.setTempDirectory(currDir);

        String command = "disp(\"Hello world\")";

        List<String> expectedCommand = new ArrayList<String>();
        expectedCommand.add("addpath('" + currDir.getPath().replaceAll("'", ";;") + "');" + "matlab_" + currDir.getName());
        List<String> actualCommand = runner.generateCommand(command);

        Assert.assertEquals(actualCommand, expectedCommand);

        File expectedFile = new File(currDir, "matlab_" + currDir.getName() + ".m");

        Assert.assertTrue(expectedFile.exists());

        String contents = FileUtils.readFileToString(expectedFile);

        Assert.assertEquals(contents, "cd '" + currDir.getPath() + "';\n" + command);
    }

    // verify MATLAB added to PATH
    @Test
    public void matlabIsAddedToPath() throws IOException {
        runner.createUniqueFolder();

        runner.createCommand("blank");

        Mockito.verify(context).addEnvironmentVariable("Path", "/path/to/matlab" + File.separator + "bin" + File.pathSeparator);
    }
        
    // copyFileToWorkspace test
    @Test
    public void verifyCopyFileToWorkspace() throws IOException {
        runner.createUniqueFolder();

        File tempFile = new File(runner.getTempDirectory(), "tempFile");

        Assert.assertFalse(tempFile.exists());

        runner.copyFileToWorkspace("glnxa64/run-matlab-command", tempFile);

        Assert.assertTrue(tempFile.exists());
    }
    
    // cleanUp test
    @Test
    public void verifyTempDirectoryIsRemoved() {
        runner.createUniqueFolder();

        File temp = runner.getTempDirectory();

        Assert.assertTrue(temp.exists());

        runner.cleanUp(mock(BuildProgressLogger.class));

        Assert.assertFalse(temp.exists());
    }
}
