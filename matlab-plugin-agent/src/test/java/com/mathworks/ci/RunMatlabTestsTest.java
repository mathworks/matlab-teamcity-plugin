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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

public class RunMatlabTestsTest {
    RunMatlabTestsService service;
    File currDir;
    String uniqueName = "tempFile";

    Map<String, String> envMaps = new HashMap<>();
    List<String> bashCommands =  new ArrayList<String>();

    boolean isWindows;
    String bashScriptFileName = "run_matlab_command.sh";

    @BeforeTest
    public void testSetUp() throws RunBuildException {
        service = spy(RunMatlabTestsService.class);

        String systemTempFolder = System.getProperty("java.io.tmpdir");
        currDir = new File(systemTempFolder, "tmpCommandProjectWorkspace");
        currDir.mkdir();

        File matlabFolderInWorkspace = new File(currDir,".matlab");
        File tmpFolderInWorkspace = new File(matlabFolderInWorkspace, uniqueName);

        envMaps.put("MatlabPathKey", "/path/to/matlab");
        envMaps.put("PATH", "path1;path2");
        envMaps.put("logOutputDetail", "Default");
        envMaps.put("logLoggingLevel", "Default");
        envMaps.put("htmlCoverage", "coverage.zip");
        envMaps.put("htmlTestArtifact", "coverage1.zip");
        envMaps.put("junitArtifact", "tapreport.tap");



        isWindows = System.getProperty("os.name").startsWith("Windows");
        Mockito.doReturn(isWindows).when(service).isWindows();

        if(isWindows) {
            bashCommands.add("/C");
            bashScriptFileName = "run_matlab_command.bat";
        }

        //Path to run_matlab_command.bat script
        bashCommands.add(currDir.getAbsolutePath()+File.separator +".matlab"+ File.separator + "tempFile" +File.separator + bashScriptFileName);
        //Arguments to bat script file
        bashCommands.add("addpath('" + tmpFolderInWorkspace.getAbsolutePath() + "'); " +
                                   "runner_tempFile,"+
                                   "delete('" + ".matlab" + File.separator + "tempFile" + File.separator + "runner_tempFile.m" +"')," +
                                   "runnerScript," +
                                   "rmdir(tmpDir,'s')");

        Mockito.doReturn(envMaps).when(service).getEnVars();
        Mockito.doReturn(currDir).when(service).getProjectDir();
        Mockito.doReturn(envMaps).when(service).getRunParameters();

        Mockito.doReturn("tempFile").when(service).getUniqueNameForRunnerFile();
        doAnswer((msg) -> {
            verifyMsgToUser(msg.getArgument(0));
            return null;
        }).when(service).showMessageToUser(anyString());
        doAnswer((path) -> {
            service.getUpdatedPath(path.getArgument(0));
            return null;
        }).when(service).addToPath(any());

    }

    @AfterTest
    public void testTearDown() throws IOException {
        FileUtils.deleteDirectory(currDir);
    }

    private void verifyMsgToUser(String message) {
        Assert.assertTrue(message.contains("Generating MATLAB script with content:\n"));
        Assert.assertTrue(message.contains(currDir.getAbsolutePath()));
    }

    @Test(description="Validate generated bash commands")
    public void verifyGeneratedBashCommands() throws RunBuildException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        Mockito.doReturn(isWindows).when(service).isWindows();

        Method getBashCommands = RunMatlabTestsService.class.getDeclaredMethod("getBashCommands", null);
        getBashCommands.setAccessible(true);

        Assert.assertEquals(getBashCommands.invoke(service, null), bashCommands);
    }

    public String getExpectedMATLABScriptCOntent(){
        String expectedFileContent = "";
        return expectedFileContent;
    }

    @Test(dependsOnMethods = { "verifyGeneratedBashCommands" })
    public void verifyContentOfMATLABScriptFile() throws IOException {
        File matlabFolderInWorkspace = new File(currDir,".matlab");
        Assert.assertTrue(matlabFolderInWorkspace.exists());

        File tmpFolderInWorkspace = new File(matlabFolderInWorkspace, uniqueName);
        Assert.assertTrue(tmpFolderInWorkspace.exists());

        File matlabScriptFile = new File(tmpFolderInWorkspace, "runner_"+uniqueName+".m");
        Assert.assertTrue(matlabScriptFile.exists());

        File genscriptZipFile = new File(tmpFolderInWorkspace, "matlab-script-generator.zip");
        Assert.assertTrue(genscriptZipFile.exists());

        String matlabScriptFileContent = Files.readString(matlabScriptFile.toPath());
//        Assert.assertTrue(matlabScriptFileContent.contains("matlabCommandByUser"));
//        Assert.assertTrue(matlabScriptFileContent.contains("cd '"+currDir.getAbsolutePath() +"'"));
    }

    @Test(dependsOnMethods = { "verifyContentOfMATLABScriptFile" })
    public void cleanUp(){

    }
}
