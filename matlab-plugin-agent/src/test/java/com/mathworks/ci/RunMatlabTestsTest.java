package com.mathworks.ci;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.RunBuildException;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RunMatlabTestsTest {
    RunMatlabTestsService service;
    static File currDir;
    static String uniqueName = "tempFile";

    static Map<String, String> envMaps = new HashMap<>();
    List<String> bashCommands =  new ArrayList<String>();

    boolean isWindows;
    String bashScriptFileName = "run_matlab_command.sh";

    static String genscriptArgs = "'Test','Strict',false";

    @BeforeTest
    public void testSetUp() throws RunBuildException, IOException {
        service = spy(RunMatlabTestsService.class);

        currDir = Files.createTempDirectory("projectDir").toFile();

        File matlabFolderInWorkspace = new File(currDir,".matlab");
        File tmpFolderInWorkspace = new File(matlabFolderInWorkspace, uniqueName);

        envMaps.put("MatlabPathKey", "/path/to/matlab");
        envMaps.put("logOutputDetail", "Default");
        envMaps.put("logLoggingLevel", "Default");

        isWindows = System.getProperty("os.name").startsWith("Windows");
        Mockito.doReturn(isWindows).when(service).isWindows();

        if(isWindows) {
            bashScriptFileName = "run_matlab_command.bat";
        }

        //Path to run_matlab_command.bat script
        bashCommands.add(currDir.getAbsolutePath()+File.separator +".matlab"+ File.separator + "tempFile" +File.separator + bashScriptFileName);
        //Arguments to bat script file
        bashCommands.add("addpath('" + tmpFolderInWorkspace.getAbsolutePath() + "'); " +
                                   "runner_tempFile,"+
                                   "delete('" + ".matlab" + File.separator + "tempFile" + File.separator + "runner_tempFile.m" +"')," +
                                   "runnerScript," +
                                   "rmpath(tmpDir)," +
                                   "rmdir(tmpDir,'s')");

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
        Assert.assertTrue(message.contains(currDir.getAbsolutePath()));
    }

    // Makes a private method accessible to test
    public Method getAccessibleMethod(String methodName) throws NoSuchMethodException {
        Method method = RunMatlabTestsService.class.getDeclaredMethod(methodName, null);
        method.setAccessible(true);
        return method;
    }

    @Test(description="Validate generated bash commands")
    public void verifyGeneratedBashCommands() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        Mockito.doReturn(isWindows).when(service).isWindows();

        Method getBashCommands = getAccessibleMethod("getBashCommands");
        getBashCommands.setAccessible(true);

        // Verify '.matlab' and a temp folder are created
        Assert.assertEquals(getBashCommands.invoke(service, null), bashCommands);
        File matlabFolderInWorkspace = new File(currDir,".matlab");
        Assert.assertTrue(matlabFolderInWorkspace.exists());

        File tmpFolderInWorkspace = new File(matlabFolderInWorkspace, uniqueName);
        Assert.assertTrue(tmpFolderInWorkspace.exists());

        //Verify MATLAB runner file and genscript zip file are present
        File matlabScriptFile = new File(tmpFolderInWorkspace, "runner_"+uniqueName+".m");
        Assert.assertTrue(matlabScriptFile.exists());

        File genscriptZipFile = new File(tmpFolderInWorkspace, "matlab-script-generator.zip");
        Assert.assertTrue(genscriptZipFile.exists());
    }

    // Expected genscript arguments with all possible inputs
    @DataProvider(name = "Data with all user inputs")
    public static Object[][] dataWithAllInput(){
        Map<String, String> envMapsWithAllUserInputs = new HashMap<>(envMaps);
        envMapsWithAllUserInputs.put("filterTestFolderByName", "test");
        envMapsWithAllUserInputs.put("sourceFolders", "src");
        envMapsWithAllUserInputs.put("filterTestByTag", "Tag");
        envMapsWithAllUserInputs.put("runTestParallel", "true");
        envMapsWithAllUserInputs.put("strict", "true");
        envMapsWithAllUserInputs.put("logLoggingLevel", "Terse");
        envMapsWithAllUserInputs.put("logOutputDetail", "Terse");
        envMapsWithAllUserInputs.put("pdfTestArtifact", "pdfReport");
        envMapsWithAllUserInputs.put("htmlTestArtifact", "htmlReport.zip");
        envMapsWithAllUserInputs.put("tapTestArtifact", "tapReport.tap");
        envMapsWithAllUserInputs.put("junitArtifact", "junitReport.tap");
        envMapsWithAllUserInputs.put("htmlCoverage", "htmlCoverage.zip");

        String expectedGenscriptArgs = "'Test','SelectByFolder',{'test'},'SourceFolder',{'src'},'SelectByTag','Tag'," +
                "'UseParallel',true,'Strict',true,'OutputDetail','Terse','LoggingLevel','Terse'," +
                "'PDFTestReport','pdfReport','HTMLTestReport','.matlab/tempFile/htmlReport'," +
                "'TAPTestResults','tapReport.tap','JUnitTestResults','junitReport.tap'," +
                "'HTMLCodeCoverage','.matlab/tempFile/htmlCoverage'";

        String genscriptZipLocation = new File(new File(currDir, ".matlab"), uniqueName).getAbsolutePath() + File.separator + "matlab-script-generator.zip";
        String expectedGeneratedScript = MatlabConstants.TEST_RUNNER_SCRIPT.replace("${ZIP_FILE}", genscriptZipLocation)
                                        .replace("${PARAMS}", expectedGenscriptArgs);
        return new Object[][] {{envMapsWithAllUserInputs, expectedGeneratedScript}};
    }

    @Test(dataProvider  = "Data with all user inputs")
    public void verifyContentOfMATLABScript(Map<String, String> envMapsWithAllUserInputs, String expectedScript) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        Mockito.doReturn(envMapsWithAllUserInputs).when(service).getUserInputs();
        getAccessibleMethod("getBashCommands").invoke(service, null);

        File matlabFolderInWorkspace = new File(currDir,".matlab");
        File tmpFolderInWorkspace = new File(matlabFolderInWorkspace, uniqueName);
        File matlabScriptFile = new File(tmpFolderInWorkspace, "runner_"+uniqueName+".m");
        String matlabScriptFileContent = FileUtils.readFileToString(matlabScriptFile);
        Assert.assertEquals(matlabScriptFileContent, expectedScript);
    }

    // Cleanup process to verify the temp folder is deleted
    @Test(dependsOnMethods = { "verifyGeneratedBashCommands" })
    public void cleanUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Mockito.doReturn(envMaps).when(service).getUserInputs();
        Method cleanUp = getAccessibleMethod("cleanUp");
        File matlabFolderInWorkspace = new File(currDir,".matlab");

        File tmpFolderInWorkspace = new File(matlabFolderInWorkspace, uniqueName);
        cleanUp.invoke(service, null);

        Assert.assertTrue(matlabFolderInWorkspace.exists());
        Assert.assertFalse(tmpFolderInWorkspace.exists());
    }

    // Test setup to put MATLAB files in the workspace before cleanUp process
    public void reportSetUp(String fileName) throws IOException, URISyntaxException {
        File reportZipFIle = new File(currDir.getAbsolutePath() + File.separator + ".matlab" + File.separator + uniqueName +File.separator + fileName);
        copyDirectory(new File(getClass().getClassLoader().getResource("TestData").toURI()), reportZipFIle);
    }
    // Verifies the content of the generated zip file
    private void verifyZipFoldersContent(File reportFolder) throws ZipException {
        File extractFolder = new File(reportFolder.getParent(), "extractedData");
        new ZipFile(reportFolder).extractAll(extractFolder.getPath());
        Assert.assertTrue(new File(extractFolder, "add.m").exists());
        Assert.assertTrue(new File(extractFolder, "test/testCalc.m").exists());
    }

    // Test to verify zip report is generated from MATLAB HTML report
    @Test(dependsOnMethods = { "verifyGeneratedBashCommands" })
    public void cleanUpWithHTMLReport() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, URISyntaxException {
        reportSetUp("coverage");

        Map<String, String> envMapsWithHTMLReport = new HashMap<>(envMaps);
        envMapsWithHTMLReport.put("htmlTestArtifact", "coverage.zip");

        Mockito.doReturn(envMapsWithHTMLReport).when(service).getUserInputs();
        Method cleanUp = getAccessibleMethod("cleanUp");
        cleanUp.invoke(service,null);

        File reportFolder = new File(currDir, envMapsWithHTMLReport.get("htmlTestArtifact"));
        Assert.assertTrue(reportFolder.exists());
        verifyZipFoldersContent(reportFolder);
    }

    // Test to verify zip report is generated from MATLAB HTML reports
    @Test(dependsOnMethods = { "verifyGeneratedBashCommands" })
    public void cleanUpWithHTMLReportInFolder() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, URISyntaxException {
        reportSetUp("coverage");

        Map<String, String> envMapsWithHTMLReportWithFolder = new HashMap<>(envMaps);
        envMapsWithHTMLReportWithFolder.put("htmlTestArtifact", "reportFolder/coverage.zip");

        Mockito.doReturn(envMapsWithHTMLReportWithFolder).when(service).getUserInputs();
        Method cleanUp = getAccessibleMethod("cleanUp");
        cleanUp.invoke(service,null);

        File reportFile = new File(currDir, envMapsWithHTMLReportWithFolder.get("htmlTestArtifact"));
        Assert.assertTrue(reportFile.exists());
        verifyZipFoldersContent(reportFile);
    }

    // Test to verify zip report is generated from MATLAB HTML code cov reports
    @Test(dependsOnMethods = { "verifyGeneratedBashCommands" })
    public void verifyHTMLCodeCovCleanup() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, URISyntaxException {
        reportSetUp("codecoverage");

        Map<String, String> envMapsWithHTMLCodeCov = new HashMap<>(envMaps);
        envMapsWithHTMLCodeCov.put("htmlCoverage", "codecoverage.zip");

        Mockito.doReturn(envMapsWithHTMLCodeCov).when(service).getUserInputs();
        Method cleanUp = getAccessibleMethod("cleanUp");
        cleanUp.invoke(service,null);


        File reportFile = new File(currDir, envMapsWithHTMLCodeCov.get("htmlCoverage"));
        Assert.assertTrue(reportFile.exists());
        verifyZipFoldersContent(reportFile);
    }

    // Test to verify zip report is generated from MATLAB HTML code cov reports
    @Test(dependsOnMethods = { "verifyGeneratedBashCommands" })
    public void verifyHTMLCodeCovCleanupInFolder() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, URISyntaxException {
        reportSetUp("codecoverage");

        Map<String, String> envMapsWithHTMLCodecovWithFolder = new HashMap<>(envMaps);
        envMapsWithHTMLCodecovWithFolder.put("htmlCoverage", "codecovFolder/codecoverage.zip");

        Mockito.doReturn(envMapsWithHTMLCodecovWithFolder).when(service).getUserInputs();
        Method cleanUp = getAccessibleMethod("cleanUp");
        cleanUp.invoke(service,null);

        File reportFile = new File(currDir, envMapsWithHTMLCodecovWithFolder.get("htmlCoverage"));
        Assert.assertTrue(reportFile.exists());
        verifyZipFoldersContent(reportFile);
    }

    @DataProvider(name = "Expected Genscript arguments for user inputs")
    public static Object[][] genscriptData() {
        // Input for Filter test by folder
        Map<String, String> envMapsWithMultipleTestFolder = new HashMap<>(envMaps);
        envMapsWithMultipleTestFolder.put("filterTestFolderByName", "testFolder1;testFolder2");
        String genScriptArgsWithTestFolder = "'Test'," + "'SelectByFolder',{'testFolder1','testFolder2'}," +"'Strict',false";

        // Input for Source folder
        Map<String, String> envMapsWithSrcFolder = new HashMap<>(envMaps);
        envMapsWithSrcFolder.put("sourceFolders", "srcFolder1;srcFolder2");
        String genScriptArgsWithSrcFolder = "'Test'," + "'SourceFolder',{'srcFolder1','srcFolder2'}" + "," + "'Strict',false";

        // Input for filter test by tag
        Map<String, String> envMapsWithTag = new HashMap<>(envMaps);
        envMapsWithTag.put("filterTestByTag", "Tag");
        String genScriptArgsWithTag = "'Test'," + "'SelectByTag','Tag'," + "'Strict',false";

        // Input for Run tests in parallel
        Map<String, String> envMapsWithRunInParallel = new HashMap<>(envMaps);
        envMapsWithRunInParallel.put("runTestParallel", "true");
        String genScriptArgsWithRunInParallel = "'Test'," + "'UseParallel',true," + "'Strict',false";

        // Input to run in Strict mode
        Map<String, String> envMapsWithStrict = new HashMap<>(envMaps);
        envMapsWithStrict.put("strict", "true");
        String genScriptArgsWithStrict = "'Test'," +"'Strict',true";

        // Input to specify logging level
        Map<String, String> envMapsWithLoggingLevel = new HashMap<>(envMaps);
        envMapsWithLoggingLevel.put("logLoggingLevel", "Terse");
        String genScriptArgsWithLogginglevel = genscriptArgs + ",'LoggingLevel','Terse'";

        // Input to specify output level
        Map<String, String> envMapsWithOutputLevel = new HashMap<>(envMaps);
        envMapsWithOutputLevel.put("logOutputDetail", "Terse");
        String genScriptArgsWithOutputLevel = genscriptArgs + ",'OutputDetail','Terse'";

        // Input to generate PDF artifact
        Map<String, String> envMapsWithPDFReport = new HashMap<>(envMaps);
        envMapsWithPDFReport.put("pdfTestArtifact", "pdfReport");
        String genScriptArgsWithPDFReport = genscriptArgs + ",'PDFTestReport','" + "pdfReport" +"'";

        // Input to generate HTML report
        Map<String, String> envMapsWithHTMLReport = new HashMap<>(envMaps);
        envMapsWithHTMLReport.put("htmlTestArtifact", "htmlReport.zip");
        String genScriptArgsWithHTMLReport = genscriptArgs + ",'HTMLTestReport','" + ".matlab/" + uniqueName + "/htmlReport" +"'";

        // Input to generate Tap artifact
        Map<String, String> envMapsWithTapReport = new HashMap<>(envMaps);
        envMapsWithTapReport.put("tapTestArtifact", "tapReport.tap");
        String genScriptArgsWithTapReport = genscriptArgs + ",'TAPTestResults','tapReport.tap'";

        // Input to generate JUnit artifact
        Map<String, String> envMapsWithJUnitReport = new HashMap<>(envMaps);
        envMapsWithJUnitReport.put("junitArtifact", "junitReport.tap");
        String genScriptArgsWithJUnitReport = genscriptArgs + ",'JUnitTestResults','junitReport.tap'";

        // Input to generate HTML code coverage
        Map<String, String> envMapsWithHTMLCodeCov = new HashMap<>(envMaps);
        envMapsWithHTMLCodeCov.put("htmlCoverage", "htmlCoverage.zip");
        String genScriptArgsWithHTMLCodeCov = genscriptArgs + ",'HTMLCodeCoverage','" + ".matlab/"+ uniqueName + "/htmlCoverage" +"'";
        return new Object[][] {{envMaps, genscriptArgs},
                {envMapsWithMultipleTestFolder, genScriptArgsWithTestFolder},
                {envMapsWithSrcFolder, genScriptArgsWithSrcFolder}, {envMapsWithTag, genScriptArgsWithTag},
                {envMapsWithRunInParallel, genScriptArgsWithRunInParallel}, {envMapsWithStrict, genScriptArgsWithStrict},
                {envMapsWithLoggingLevel, genScriptArgsWithLogginglevel}, {envMapsWithOutputLevel, genScriptArgsWithOutputLevel},
                {envMapsWithPDFReport, genScriptArgsWithPDFReport},{envMapsWithHTMLReport, genScriptArgsWithHTMLReport}, {envMapsWithTapReport, genScriptArgsWithTapReport},
                {envMapsWithJUnitReport, genScriptArgsWithJUnitReport}, {envMapsWithHTMLCodeCov, genScriptArgsWithHTMLCodeCov}};
    }

    @Test(dataProvider  = "Expected Genscript arguments for user inputs")
    public void verifyGenscriptArguments(Map<String, String> envMap, String expectedGenscriptArgs)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Mockito.doReturn(envMap).when(service).getUserInputs();
        service.setUniqueTmpFldrName(uniqueName);

        Method getGenScriptParametersForTests = getAccessibleMethod("getGenScriptParametersForTests");
        getGenScriptParametersForTests.setAccessible(true);
        String actualGenscriptArgs = (String) getGenScriptParametersForTests.invoke(service,null);
        Assert.assertEquals(actualGenscriptArgs, expectedGenscriptArgs);
    }
}
