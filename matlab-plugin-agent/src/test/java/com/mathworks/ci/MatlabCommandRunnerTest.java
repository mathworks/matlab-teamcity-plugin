package com.mathworks.ci;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

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
import org.testng.annotations.BeforeMethod;
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

    Boolean isWindows;

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
        when(context.getBuild()).thenReturn(agent);
        when(context.getBuildParameters()).thenReturn(buildParamsMap);
        when(agent.getAgentConfiguration()).thenReturn(agentConfig);
        when(agentConfig.getSystemInfo()).thenReturn(agentSysInfo);
        when(buildParamsMap.getEnvironmentVariables()).thenReturn(envVars);

        if (System.getProperty("os.name").startsWith("Windows")) {
            when(agentSysInfo.isWindows()).thenReturn(true);
            when(agentSysInfo.isMac()).thenReturn(false);
            isWindows = true;
        } else {
            when(agentSysInfo.isWindows()).thenReturn(false);
            when(agentSysInfo.isMac()).thenReturn(false);
            isWindows = false;
        }

        runner = new MatlabCommandRunner();
    }

    @BeforeMethod
    public void insertStubs() {
        when(context.getRunnerParameters()).thenReturn(params);
    }

    @AfterTest
    public void testTearDown() throws IOException {
        FileUtils.deleteDirectory(currDir);
    }

    // Makes a private method accessible to test
    public Method getAccessibleMethod(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = MatlabCommandRunner.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method;
    }

    // temp folder creation
    @Test
    public void verifyTempFolderCreation() {
        // Create temp directory
        runner.createUniqueFolder(context);

        File expected = new File(currDir, ".matlab" + File.separator + runner.getTempDirectory().getName());

        Assert.assertTrue(expected.exists());
    }

    // executable creation
    @Test
    public void verifyExecutableCreation() throws IOException {
        // Set temp dir
        runner.setTempDirectory(currDir);

        // Copy executable
        runner.createCommand(context, "dummy");

        
        // If windows
        File expected;
        File actual;
        if (isWindows) {
            expected = new File(currDir, "run-matlab-command.exe");
            actual = new File(runner.getTempDirectory(), "run-matlab-command.exe");
        } else {
            expected = new File(currDir, "run-matlab-command");
            actual = new File(runner.getTempDirectory(), "run-matlab-command");
        }

        Assert.assertTrue(expected.exists());
        Assert.assertEquals(actual.getPath(), expected.getPath());
    }

    // matlab script creation + command generation
    @Test
    public void verifyScriptCreated() throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        runner.setTempDirectory(currDir);

        String command = "disp(\"Hello world\")";

        List<String> expectedCommand = new ArrayList<String>();
        expectedCommand.add("setenv('MW_ORIG_WORKING_FOLDER', cd('" 
                + currDir.getPath().replaceAll("'", ";;") 
                + "'));" + "matlab_" + currDir.getName());
        
        Method generateCommandArgs = getAccessibleMethod("generateCommandArgs", BuildRunnerContext.class, String.class);

        Object actualCommand = generateCommandArgs.invoke(runner, context, command);
        Assert.assertEquals(actualCommand, expectedCommand);

        File expectedFile = new File(currDir, "matlab_" + currDir.getName() + ".m");
        Assert.assertTrue(expectedFile.exists());

        String contents = FileUtils.readFileToString(expectedFile);
        Assert.assertEquals(contents, "cd(getenv('MW_ORIG_WORKING_FOLDER'));\naddpath('" + currDir.getPath() + "');\n" + command);
    }

    // startup options test
    @Test
    public void startupOptionsAdded () throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String options = "-nojvm -logfile file.log";

        Map<String, String> params = new HashMap<String, String>();
        params.put(MatlabConstants.STARTUP_OPTIONS, options);

        when(context.getRunnerParameters()).thenReturn(params);

        Method generateCommandArgs = getAccessibleMethod("generateCommandArgs", BuildRunnerContext.class, String.class);

        Object o = generateCommandArgs.invoke(runner, context, "blank");
        List<String> actualCommand = ((List<String>) o);

        System.err.println(actualCommand.toString());

        Assert.assertEquals(actualCommand.get(1), "-nojvm");
        Assert.assertEquals(actualCommand.get(2), "-logfile");
        Assert.assertEquals(actualCommand.get(3), "file.log");

    }

    // verify MATLAB added to PATH
    @Test
    public void matlabIsAddedToPath() throws IOException {
        runner.createUniqueFolder(context);

        runner.createCommand(context, "blank");

        Mockito.verify(context).addEnvironmentVariable("Path", "/path/to/matlab" + File.separator + "bin" + File.pathSeparator);
    }
        
    // copyFileToWorkspace test
    @Test
    public void verifyCopyFileToWorkspace() throws IOException {
        runner.createUniqueFolder(context);

        File tempFile = new File(runner.getTempDirectory(), "tempFile");
        Assert.assertFalse(tempFile.exists());

        runner.copyFileToWorkspace("glnxa64/run-matlab-command", tempFile);
        Assert.assertTrue(tempFile.exists());
    }
    
    // cleanUp test
    @Test
    public void verifyTempDirectoryIsRemoved() {
        runner.createUniqueFolder(context);

        File temp = runner.getTempDirectory();
        Assert.assertTrue(temp.exists());

        runner.cleanUp(mock(BuildProgressLogger.class));
        Assert.assertFalse(temp.exists());
    }
}
