package com.mathworks.ci;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.RunBuildException;
import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class RunMatlabCommandTest {

    RunMatlabCommandService service;
    static File currDir;

    static {
        try {
            currDir = Files.createTempDirectory("projectDir").toFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String uniqueName = "tempFile";

    Map<String, String> envMaps = new HashMap<>();
    List<String> bashCommandsForWindows =  new ArrayList<String>();
    List<String> bashCommandsForLinux = new ArrayList<String>();

    @BeforeTest
    public void testSetUp() throws RunBuildException {
        service = spy(RunMatlabCommandService.class);

        envMaps.put("MatlabPathKey", "/path/to/matlab");
        envMaps.put("PATH", "path1;path2");
        envMaps.put("matlabCommand", "matlabCommandByUser");


        bashCommandsForWindows.add("/C");
        //Path to run_matlab_command.bat script
        bashCommandsForWindows.add(currDir.getAbsolutePath()+File.separator +".matlab"+ File.separator + "tempFile" +File.separator + "run_matlab_command.bat");
        //Arguments to bat script file
        bashCommandsForWindows.add("cd .matlab"+File.separator+uniqueName+",cmd_"+uniqueName);

        //Path to run_matlab_command.sh script
        bashCommandsForLinux.add(currDir.getAbsolutePath()+File.separator +".matlab"+ File.separator + "tempFile" +File.separator +"run_matlab_command.sh");
        //Arguments to sh script file
        bashCommandsForLinux.add("cd .matlab/"+uniqueName+",cmd_"+uniqueName);

        Mockito.doReturn(envMaps).when(service).getEnVars();
        Mockito.doReturn(currDir).when(service).getProjectDir();

        Mockito.doReturn("tempFile").when(service).getUniqueNameForRunnerFile();
        doAnswer((msg) -> {
            verifyMsgToUser(msg.getArgument(0));
            return null;
        }).when(service).showMessageToUser(anyString());
    }

    private void verifyMsgToUser(String message) {
        Assert.assertTrue(message.contains("Generating MATLAB script with content:\n"));
        Assert.assertTrue(message.contains("matlabCommandByUser"));
        Assert.assertTrue(message.contains(currDir.getAbsolutePath()));
    }

    @Test(description="Validate generated bash commands")
    public void verifyGeneratedBashCommands() throws RunBuildException {
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        Mockito.doReturn(isWindows).when(service).isWindows();
        if(isWindows){
            Assert.assertEquals(service.getBashCommands(), bashCommandsForWindows);
        }
        else{
            Assert.assertEquals(service.getBashCommands(), bashCommandsForLinux);
        }
    }

    @Test(dependsOnMethods = { "verifyGeneratedBashCommands" })
    public void verifyContentOfMATLABScriptFile() throws IOException {
        File matlabFolderInWorkspace = new File(currDir,".matlab");
        Assert.assertTrue(matlabFolderInWorkspace.exists());

        File tmpFolderInWorkspace = new File(matlabFolderInWorkspace, uniqueName);
        Assert.assertTrue(tmpFolderInWorkspace.exists());

        File matlabScriptFile = new File(tmpFolderInWorkspace, "cmd_"+uniqueName+".m");
        Assert.assertTrue(matlabScriptFile.exists());

        String matlabScriptFileContent = FileUtils.readFileToString(matlabScriptFile);
        Assert.assertTrue(matlabScriptFileContent.contains("matlabCommandByUser"));
        Assert.assertTrue(matlabScriptFileContent.contains("cd '"+currDir.getAbsolutePath() +"'"));
    }

    @Test(dependsOnMethods = { "verifyContentOfMATLABScriptFile" })
    public void verifyCleanUp() {
        File matlabFolderInWorkspace = new File(currDir,".matlab");

        File tmpFolderInWorkspace = new File(matlabFolderInWorkspace, uniqueName);
        service.cleanUp();

        Assert.assertTrue(matlabFolderInWorkspace.exists());
        Assert.assertFalse(tmpFolderInWorkspace.exists());
    }
}
