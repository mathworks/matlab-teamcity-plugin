package com.mathworks.ci;

import jetbrains.buildServer.RunBuildException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.management.Descriptor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.spy;

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
    public void testSetUp(){
        service = spy(RunMatlabCommandService.class);

        envMaps.put("MatlabPathKey", "/path/to/matlab");
        envMaps.put("PATH", "path1;path2");
        envMaps.put("matlabCommand", "matlabCommandByUser");


        bashCommandsForWindows.add("/C");
        //Path to run_matlab_command.bat script
        bashCommandsForWindows.add(currDir.getAbsolutePath()+File.separator +".matlab"+ File.separator + "tempFile" +File.separator + "run_matlab_command.bat");
        //Arguments to bat script file
        bashCommandsForWindows.add("cd .matlab\\tempFile,cmd_tempFile");

        //Path to run_matlab_command.sh script
        bashCommandsForLinux.add(currDir.getAbsolutePath()+File.separator +".matlab"+ File.separator + "tempFile" +File.separator +"run_matlab_command.sh");
        //Arguments to sh script file
        bashCommandsForLinux.add("cd .matlab/"+uniqueName+",cmd_"+uniqueName);

        Mockito.doReturn(envMaps).when(service).getEnVars();
        Mockito.doReturn(currDir).when(service).getProjectDir();

        Mockito.doReturn("tempFile").when(service).getUniqueNameForRunnerFile();
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
}
