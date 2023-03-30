package com.mathworks.ci;

import jetbrains.buildServer.RunBuildException;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class RunMatlabBuildTest {
    RunMatlabBuildService service;
    static File currDir;

    static String uniqueName = "tempFile";

    Map<String, String> envMaps = new HashMap<>();
    List<String> bashCommands =  new ArrayList<String>();
    boolean isWindows;
    String bashScriptFileName = "run_matlab_command.sh";

    @BeforeTest
    public void mockSetUp() throws RunBuildException {
        service = spy(RunMatlabBuildService.class);

        String systemTempFolder = System.getProperty("java.io.tmpdir");

        currDir = new File(systemTempFolder, "tmpBuildProjectWorkspace");
        currDir.mkdir();
        envMaps.put("MatlabPathKey", "/path/to/matlab");
        envMaps.put("PATH", "path1;path2");
        envMaps.put("matlabTasks", "buildToolTask");

        isWindows = System.getProperty("os.name").startsWith("Windows");
        Mockito.doReturn(isWindows).when(service).isWindows();

        if(isWindows) {
            bashCommands.add("/C");
            bashScriptFileName = "run_matlab_command.bat";
        }
        //Path to run_matlab_command.bat script
        bashCommands.add(currDir.getAbsolutePath()+File.separator +".matlab"+ File.separator + "tempFile" +File.separator + bashScriptFileName);
        //Arguments to bat script file
        bashCommands.add("cd .matlab"+File.separator+uniqueName+",build_"+uniqueName);

        Mockito.doReturn(envMaps).when(service).getUserInputs();
        Mockito.doReturn(currDir).when(service).getWorkspace();

        Mockito.doReturn("tempFile").when(service).getUniqueNameForRunnerFile();
        doAnswer((msg) -> {
            verifyMsgToUser(msg.getArgument(0));
            return null;
        }).when(service).logMessage(anyString());
    }

    @AfterTest
    public void testTearDown() throws IOException {
        FileUtils.deleteDirectory(currDir);
    }

    private void verifyMsgToUser(String message) {
        Assert.assertTrue(message.contains("Generating MATLAB script with content:\n"));
        Assert.assertTrue(message.contains("buildToolTask"));
        Assert.assertTrue(message.contains(currDir.getAbsolutePath()));
    }

    //Used to make the private methods accessible to test the methods
    public Method getAccessibleMethod(String methodName) throws NoSuchMethodException {
        Method method = RunMatlabBuildService.class.getDeclaredMethod(methodName, null);
        method.setAccessible(true);
        return method;
    }

    // Verify the bash commands are generated correctly
    @Test(description="Validate generated bash commands")
    public void verifyGeneratedBashCommands() throws RunBuildException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getBashCommands = RunMatlabBuildService.class.getDeclaredMethod("getBashCommands", null);
        getBashCommands.setAccessible(true);

        Assert.assertEquals(getBashCommands.invoke(service, null), bashCommands);

    }

    // Verify the MATLAB script content
    @Test(dependsOnMethods = { "verifyGeneratedBashCommands" })
    public void verifyContentOfMATLABScriptFile() throws IOException {
        File matlabFolderInWorkspace = new File(currDir,".matlab");
        Assert.assertTrue(matlabFolderInWorkspace.exists());

        File tmpFolderInWorkspace = new File(matlabFolderInWorkspace, uniqueName);
        Assert.assertTrue(tmpFolderInWorkspace.exists());

        File matlabScriptFile = new File(tmpFolderInWorkspace, "build_"+uniqueName+".m");
        Assert.assertTrue(matlabScriptFile.exists());

        String matlabScriptFileContent = FileUtils.readFileToString(matlabScriptFile);
        Assert.assertTrue(matlabScriptFileContent.contains("buildToolTask"));
        Assert.assertTrue(matlabScriptFileContent.contains("cd '"+currDir.getAbsolutePath() +"'"));
    }

    // Verify cleanup process removes the temp folder
    @Test(dependsOnMethods = { "verifyContentOfMATLABScriptFile" })
    public void verifyCleanUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method cleanUp = getAccessibleMethod("cleanUp");
        File matlabFolderInWorkspace = new File(currDir,".matlab");

        File tmpFolderInWorkspace = new File(matlabFolderInWorkspace, uniqueName);
        cleanUp.invoke(service, null);

        Assert.assertTrue(matlabFolderInWorkspace.exists());
        Assert.assertFalse(tmpFolderInWorkspace.exists());
    }
}
