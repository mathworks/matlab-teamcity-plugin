package com.mathworks.ci;

import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

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
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RunMatlabTestsTest {
    public MatlabCommandRunner runner;

    RunMatlabTestsService service;

    static File currDir;
    static File tempDir; 

    static Map<String, String> envMap = new HashMap<>();
    static String genscriptArgs = "'Test','Strict',false";

    @BeforeTest
    public void testSetUp() throws RunBuildException, IOException {
        runner = Mockito.mock(MatlabCommandRunner.class);
        service = spy(new RunMatlabTestsService(runner));

        currDir = Files.createTempDirectory("projectDir").toFile();

        File matlabFolderInWorkspace = new File(currDir,".matlab");
        tempDir = new File(matlabFolderInWorkspace, "tempDir");

        Mockito.doReturn(currDir).when(service).getWorkspace();

        envMap.put("MatlabPathKey", "/path/to/matlab");
        envMap.put("logOutputDetail", "Default");
        envMap.put("logLoggingLevel", "Default");

        // Stub out inherited jetbrains methods
        doReturn(null).when(service).getContext();
        doReturn(null).when(service).logger();

        when(runner.getTempDirectory()).thenReturn(tempDir);
    }

    @BeforeMethod
    public void setUpDefaultEnvMap() {
        Mockito.doReturn(envMap).when(service).getUserInputs();
    }

    @AfterTest
    public void testTearDown() throws IOException {
        FileUtils.deleteDirectory(currDir);
    }

    // Makes a private method accessible to test
    public Method getAccessibleMethod(String methodName, Class<?>... paramTypes) throws NoSuchMethodException {
        Method method = RunMatlabTestsService.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method;
    }

    // No arguments case
    @Test
    public void checkBaseCase() throws IOException, RunBuildException {
        String genscriptZipLocation = new File(tempDir, "matlab-script-generator.zip").getAbsolutePath();

        service.makeProgramCommandLine();

        ArgumentCaptor<String> matlabCommand = ArgumentCaptor.forClass(String.class);
        Mockito.verify(runner).createCommand(matlabCommand.capture());

        String expected = MatlabConstants.TEST_RUNNER_SCRIPT.replace("${PARAMS}", genscriptArgs)
            .replace("${ZIP_FILE}", genscriptZipLocation);
        
        Assert.assertEquals(expected, matlabCommand.getValue());
    }

    // Expected genscript arguments with all possible inputs
    @DataProvider(name = "Data with all user inputs")
    public static Object[][] dataWithAllInput(){
        Map<String, String> envMapsWithAllUserInputs = new HashMap<>();
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
                "'PDFTestReport','pdfReport','HTMLTestReport','.matlab/tempDir/htmlReport'," +
                "'TAPTestResults','tapReport.tap','JUnitTestResults','junitReport.tap'," +
                "'HTMLCodeCoverage','.matlab/tempDir/htmlCoverage'";

        String genscriptZipLocation = new File(tempDir, "matlab-script-generator.zip").getAbsolutePath();
        String expectedGeneratedScript = MatlabConstants.TEST_RUNNER_SCRIPT.replace("${ZIP_FILE}", genscriptZipLocation)
                                        .replace("${PARAMS}", expectedGenscriptArgs);
        return new Object[][] {{envMapsWithAllUserInputs, expectedGeneratedScript}};
    }

    @Test(dataProvider = "Data with all user inputs")
    public void verifyContentOfMATLABScript(Map<String, String> envMapsWithAllUserInputs, String expectedScript) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, RunBuildException {
        Mockito.doReturn(envMapsWithAllUserInputs).when(service).getUserInputs();

        service.makeProgramCommandLine();

        ArgumentCaptor<String> matlabCommand = ArgumentCaptor.forClass(String.class);
        Mockito.verify(runner, Mockito.times(2)).createCommand(matlabCommand.capture());

        Assert.assertEquals(matlabCommand.getValue(), expectedScript);
    }

    // Cleanup process to verify the command runner cleanup is called
    // Does this add value?
    @Test
    public void cleanUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method cleanUp = getAccessibleMethod("cleanUp", null);

        cleanUp.invoke(service, null);

        // cleanUp on runner is called
        Mockito.verify(runner).cleanUp(Mockito.any());
    }

    // Test setup to put MATLAB files in the workspace before cleanUp process
    public void reportSetUp(String fileName) throws IOException {
        File reportZipFIle = new File(tempDir, fileName);
        copyDirectory(new File(getClass().getClassLoader().getResource("TestData").getFile()), reportZipFIle);
    }
    // Verifies the content of the generated zip file
    private void verifyZipFoldersContent(File reportFolder) throws ZipException {
        File extractFolder = new File(reportFolder.getParent(), "extractedData");
        new ZipFile(reportFolder).extractAll(extractFolder.getPath());
        Assert.assertTrue(new File(extractFolder, "add.m").exists());
        Assert.assertTrue(new File(extractFolder, "test/testCalc.m").exists());
    }

    // Test to verify zip report is generated from MATLAB HTML report
    @Test()
    public void cleanUpWithHTMLReport() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        reportSetUp("coverage");

        Map<String, String> envMapsWithHTMLReport = new HashMap<>();
        envMapsWithHTMLReport.put("htmlTestArtifact", "coverage.zip");

        Mockito.doReturn(envMapsWithHTMLReport).when(service).getUserInputs();
        Method cleanUp = getAccessibleMethod("cleanUp", null);
        cleanUp.invoke(service,null);

        File reportFolder = new File(currDir, envMapsWithHTMLReport.get("htmlTestArtifact"));
        Assert.assertTrue(reportFolder.exists());
        verifyZipFoldersContent(reportFolder);
    }

    // Test to verify zip report is generated from MATLAB HTML reports
    @Test
    public void cleanUpWithHTMLReportInFolder() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        reportSetUp("coverage");

        Map<String, String> envMapsWithHTMLReportWithFolder = new HashMap<>();
        envMapsWithHTMLReportWithFolder.put("htmlTestArtifact", "reportFolder/coverage.zip");

        Mockito.doReturn(envMapsWithHTMLReportWithFolder).when(service).getUserInputs();
        Method cleanUp = getAccessibleMethod("cleanUp", null);
        cleanUp.invoke(service,null);

        File reportFile = new File(currDir, envMapsWithHTMLReportWithFolder.get("htmlTestArtifact"));
        Assert.assertTrue(reportFile.exists());
        verifyZipFoldersContent(reportFile);
    }

    // Test to verify zip report is generated from MATLAB HTML code cov reports
    @Test
    public void verifyHTMLCodeCovCleanup() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        reportSetUp("codecoverage");

        Map<String, String> envMapsWithHTMLCodeCov = new HashMap<>();
        envMapsWithHTMLCodeCov.put("htmlCoverage", "codecoverage.zip");

        Mockito.doReturn(envMapsWithHTMLCodeCov).when(service).getUserInputs();
        Method cleanUp = getAccessibleMethod("cleanUp", null);
        cleanUp.invoke(service,null);

        File reportFile = new File(currDir, envMapsWithHTMLCodeCov.get("htmlCoverage"));
        Assert.assertTrue(reportFile.exists());
        verifyZipFoldersContent(reportFile);
    }

    // Test to verify zip report is generated from MATLAB HTML code cov reports
    @Test
    public void verifyHTMLCodeCovCleanupInFolder() throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        reportSetUp("codecoverage");

        Map<String, String> envMapsWithHTMLCodecovWithFolder = new HashMap<>();
        envMapsWithHTMLCodecovWithFolder.put("htmlCoverage", "codecovFolder/codecoverage.zip");

        Mockito.doReturn(envMapsWithHTMLCodecovWithFolder).when(service).getUserInputs();
        Method cleanUp = getAccessibleMethod("cleanUp", null);
        cleanUp.invoke(service,null);

        File reportFile = new File(currDir, envMapsWithHTMLCodecovWithFolder.get("htmlCoverage"));
        Assert.assertTrue(reportFile.exists());
        verifyZipFoldersContent(reportFile);
    }

    @DataProvider(name = "Expected Genscript arguments for user inputs")
    public static Object[][] genscriptData() {
        // Input for Filter test by folder
        Map<String, String> envMapsWithMultipleTestFolder = new HashMap<>(envMap);
        envMapsWithMultipleTestFolder.put("filterTestFolderByName", "testFolder1;testFolder2");
        String genScriptArgsWithTestFolder = "'Test'," + "'SelectByFolder',{'testFolder1','testFolder2'}," +"'Strict',false";

        // Input for Source folder
        Map<String, String> envMapsWithSrcFolder = new HashMap<>(envMap);
        envMapsWithSrcFolder.put("sourceFolders", "srcFolder1;srcFolder2");
        String genScriptArgsWithSrcFolder = "'Test'," + "'SourceFolder',{'srcFolder1','srcFolder2'}" + "," + "'Strict',false";

        // Input for filter test by tag
        Map<String, String> envMapsWithTag = new HashMap<>(envMap);
        envMapsWithTag.put("filterTestByTag", "Tag");
        String genScriptArgsWithTag = "'Test'," + "'SelectByTag','Tag'," + "'Strict',false";

        // Input for Run tests in parallel
        Map<String, String> envMapsWithRunInParallel = new HashMap<>(envMap);
        envMapsWithRunInParallel.put("runTestParallel", "true");
        String genScriptArgsWithRunInParallel = "'Test'," + "'UseParallel',true," + "'Strict',false";

        // Input to run in Strict mode
        Map<String, String> envMapsWithStrict = new HashMap<>(envMap);
        envMapsWithStrict.put("strict", "true");
        String genScriptArgsWithStrict = "'Test'," +"'Strict',true";

        // Input to specify logging level
        Map<String, String> envMapsWithLoggingLevel = new HashMap<>(envMap);
        envMapsWithLoggingLevel.put("logLoggingLevel", "Terse");
        String genScriptArgsWithLogginglevel = genscriptArgs + ",'LoggingLevel','Terse'";

        // Input to specify output level
        Map<String, String> envMapsWithOutputLevel = new HashMap<>(envMap);
        envMapsWithOutputLevel.put("logOutputDetail", "Terse");
        String genScriptArgsWithOutputLevel = genscriptArgs + ",'OutputDetail','Terse'";

        // Input to generate PDF artifact
        Map<String, String> envMapsWithPDFReport = new HashMap<>(envMap);
        envMapsWithPDFReport.put("pdfTestArtifact", "pdfReport");
        String genScriptArgsWithPDFReport = genscriptArgs + ",'PDFTestReport','" + "pdfReport" +"'";

        // Input to generate HTML report
        Map<String, String> envMapsWithHTMLReport = new HashMap<>(envMap);
        envMapsWithHTMLReport.put("htmlTestArtifact", "htmlReport.zip");
        String genScriptArgsWithHTMLReport = genscriptArgs + ",'HTMLTestReport','" + ".matlab/" + tempDir.getName() + "/htmlReport" +"'";

        // Input to generate Tap artifact
        Map<String, String> envMapsWithTapReport = new HashMap<>(envMap);
        envMapsWithTapReport.put("tapTestArtifact", "tapReport.tap");
        String genScriptArgsWithTapReport = genscriptArgs + ",'TAPTestResults','tapReport.tap'";

        // Input to generate JUnit artifact
        Map<String, String> envMapsWithJUnitReport = new HashMap<>(envMap);
        envMapsWithJUnitReport.put("junitArtifact", "junitReport.tap");
        String genScriptArgsWithJUnitReport = genscriptArgs + ",'JUnitTestResults','junitReport.tap'";

        // Input to generate HTML code coverage
        Map<String, String> envMapsWithHTMLCodeCov = new HashMap<>(envMap);
        envMapsWithHTMLCodeCov.put("htmlCoverage", "htmlCoverage.zip");
        String genScriptArgsWithHTMLCodeCov = genscriptArgs + ",'HTMLCodeCoverage','" + ".matlab/"+ tempDir.getName() + "/htmlCoverage" +"'";
        return new Object[][] {{envMap, genscriptArgs},
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

        doReturn(envMap).when(service).getUserInputs();
        Method getGenScriptParametersForTests = getAccessibleMethod("getGenScriptParametersForTests", String.class);
        getGenScriptParametersForTests.setAccessible(true);
        String actualGenscriptArgs = (String) getGenScriptParametersForTests.invoke(service, tempDir.getName());
        Assert.assertEquals(actualGenscriptArgs, expectedGenscriptArgs);
    }
}
