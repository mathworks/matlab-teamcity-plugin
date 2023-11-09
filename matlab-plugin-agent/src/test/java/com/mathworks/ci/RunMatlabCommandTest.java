package com.mathworks.ci;

import jetbrains.buildServer.RunBuildException;
import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.RunBuildException;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.mockito.Mock;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class RunMatlabCommandTest {
    public MatlabCommandRunner runner;

    RunMatlabCommandService service;
    File currDir;

    @BeforeTest
    public void mockSetUp() throws RunBuildException {
        runner = mock(MatlabCommandRunner.class);
        service = spy(new RunMatlabCommandService(runner));

        String systemTempFolder = System.getProperty("java.io.tmpdir");

        currDir = new File(systemTempFolder, "tmpBuildProjectWorkspace");
        currDir.mkdir();

        // Stub out inherited jetbrains methods
        doReturn(null).when(service).getContext();
        doReturn(null).when(service).logger();
    }

    @AfterTest
    public void testTearDown() throws IOException {
        FileUtils.deleteDirectory(currDir);
    }

    //Used to make the private methods accessible to test the methods
    public Method getAccessibleMethod(String methodName) throws NoSuchMethodException {
        Method method = RunMatlabBuildService.class.getDeclaredMethod(methodName, null);
        method.setAccessible(true);
        return method;
    }

    @Test(description="Validate specific command")
    public void verifySpecificCommand() throws RunBuildException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        Map<String, String> envMap = new HashMap<>();
        envMap.put("matlabCommand", "disp(\"Hello world!\")");

        doReturn(envMap).when(service).getUserInputs();

        service.makeProgramCommandLine();

        ArgumentCaptor<String> matlabCommand = ArgumentCaptor.forClass(String.class);
        Mockito.verify(runner).createCommand(matlabCommand.capture());

        assertEquals("disp(\"Hello world!\")", matlabCommand.getValue());
    }
}
