package com.mathworks.ci;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.BuildServiceAdapter;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

public class RunMatlabTestsService extends BuildServiceAdapter {
    private MatlabCommandRunner runner;

    public RunMatlabTestsService(MatlabCommandRunner runner) {
        this.runner = runner;
    }

    public RunMatlabTestsService() {
        this(new MatlabCommandRunner());
    }

    @NotNull
    @Override
    public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
        // Set up runner - can't be done at construction time since we don't have access to the context
        runner.setRunnerContext(getContext());
        runner.createUniqueFolder();

        // Move genscript to temp directory
        File genscriptLocation = new File(runner.getTempDirectory(), MatlabConstants.MATLAB_SCRIPT_GENERATOR);

        // Prepare command
        String runnerScript = getRunnerScript(MatlabConstants.TEST_RUNNER_SCRIPT, getGenScriptParametersForTests(runner.getTempDirectory().getName()));
        runnerScript = replaceZipPlaceholder(runnerScript, genscriptLocation.getPath());

        ProgramCommandLine value;
        try {
            runner.copyFileToWorkspace(MatlabConstants.MATLAB_SCRIPT_GENERATOR, genscriptLocation);
            value = runner.createCommand(runnerScript);
        } catch (Exception e) {
            throw new RunBuildException(e);
        }

        // Create command
        return value;
    }

    //This method replaces the placeholder with genscript's zip file location URL in temp folder
    private String replaceZipPlaceholder(String script, String url) {
        script = script.replace("${ZIP_FILE}", url.replaceAll("'", "''"));
        return script;
    }

    //To get therunner script
    private String getRunnerScript(String script, String params) {
        script = script.replace("${PARAMS}", params);
        return script;
    }

    private String getGenScriptParametersForTests(String tempDir) {
        final List<String> args = new ArrayList<String>();

        args.add("'Test'");
        final String filterByTests = getUserInputs().get(MatlabConstants.FILTER_TEST);
        if (filterByTests != null) {
            args.add("'SelectByFolder'," + getCellArray(filterByTests));
        }

        final String sourceFolders = getUserInputs().get(MatlabConstants.SOURCE_FOLDER);
        if (sourceFolders != null) {
            args.add("'SourceFolder'," + getCellArray(sourceFolders));
        }

        final String filterByTag = getUserInputs().get(MatlabConstants.FILTER_TAG);
        if (filterByTag != null) {
            args.add("'SelectByTag','" + filterByTag + "'");
        }

        final String runParallelTests = getUserInputs().get(MatlabConstants.RUN_PARALLEL) == null ? "false"
            : getUserInputs().get(MatlabConstants.RUN_PARALLEL);
        if (runParallelTests.equalsIgnoreCase("true") && runParallelTests != null) {
            args.add("'UseParallel'," + Boolean.valueOf(runParallelTests) + "");
        }

        final String useStrict = getUserInputs().get(MatlabConstants.STRICT) == null ? "false" : "true";
        if (useStrict.equalsIgnoreCase(useStrict)) {
            args.add("'Strict'," + Boolean.valueOf(useStrict));
        }

        String outputDetail = getUserInputs().get(MatlabConstants.OUTPUT_DETAIL);
        if (!outputDetail.equalsIgnoreCase("default")) {
            args.add("'OutputDetail','" + outputDetail + "'");
        }

        String loggingLevel = getUserInputs().get(MatlabConstants.LOGGING_LEVEL);
        if (!loggingLevel.equalsIgnoreCase("default")) {
            args.add("'LoggingLevel','" + loggingLevel + "'");
        }

        final String pdfReport = getUserInputs().get(MatlabConstants.PDF_REPORT);
        if (pdfReport != null) {
            args.add("'PDFTestReport','" + pdfReport + "'");
        }

        final String htmlReport = getUserInputs().get(MatlabConstants.HTML_REPORT);
        if (htmlReport != null) {
            File reportFile = new File(htmlReport);
            args.add("'HTMLTestReport','" + ".matlab/" +tempDir + "/" + FilenameUtils.removeExtension(reportFile.getName()) + "'");
        }

        final String tapReport = getUserInputs().get(MatlabConstants.TAP_REPORT);
        if (tapReport != null) {
            args.add("'TAPTestResults','" + tapReport + "'");
        }

        final String junitReport = getUserInputs().get(MatlabConstants.JUNIT_REPORT);
        if (junitReport != null) {
            args.add("'JUnitTestResults','" + junitReport + "'");
        }

        final String htmlCodeCoverage = getUserInputs().get(MatlabConstants.HTML_CODE_COV_REPORT);
        if (htmlCodeCoverage != null) {
            File reportFile = new File(htmlCodeCoverage);
            args.add("'HTMLCodeCoverage','" + ".matlab/" +tempDir + "/" + FilenameUtils.removeExtension(reportFile.getName()) + "'");
        }

        return String.join(",", args);
    }

    // Testing stub
    public Map<String, String> getUserInputs() {
        return getRunnerContext().getRunnerParameters();
    }

    public File getWorkspace() {
        return getRunnerContext().getWorkingDirectory();
    }

    public BuildRunnerContext getContext() {
        return getRunnerContext();
    }

    public BuildProgressLogger logger() {
        return getLogger();
    }

    private String getCellArray(String folders) {
        final String[] folderNames = folders.split(";");
        return getCellArrayFrmList(Arrays.asList(folderNames));
    }

    private String getCellArrayFrmList(List<String> listOfStr) {
        // Ignore empty string values in the list
        Predicate<String> isEmpty = String::isEmpty;
        Predicate<String> isNotEmpty = isEmpty.negate();
        List<String> filteredListOfStr = listOfStr.stream().filter(isNotEmpty).collect(Collectors.toList());

        // Escape apostrophe for MATLAB
        filteredListOfStr.replaceAll(val -> "'" + val.replaceAll("'", "''") + "'");
        return "{" + String.join(",", filteredListOfStr) + "}";
    }

    /**
     * Zip util
     */

    private void zipFolder(File sourceFolderPath, File zipPath) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipPath.toPath().toFile()));
        Files.walkFileTree(sourceFolderPath.toPath(), new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                zip.putNextEntry(new ZipEntry(sourceFolderPath.toPath().relativize(file).toString()));
                Files.copy(file, zip);
                zip.closeEntry();
                return FileVisitResult.CONTINUE;
            }
        });
        zip.close();
    }

    /**
     * Cleanup the temporary folders
     */
    public void cleanUp() throws RunBuildException {
        try {

            final String htmlReport = getUserInputs().get(MatlabConstants.HTML_REPORT);
            if (htmlReport != null) {
                File reportFolder = new File(getWorkspace(), htmlReport);
                if (reportFolder.getParentFile() != null) {

                    // Create folders to keep .zip files
                    reportFolder.getParentFile().mkdirs();
                    zipFolder(new File(runner.getTempDirectory(), FilenameUtils.removeExtension(reportFolder.getName())), reportFolder);
                } else {
                    zipFolder(new File(runner.getTempDirectory(), FilenameUtils.removeExtension(reportFolder.getName())), reportFolder);
                }
            }

            final String htmlCoverage = getUserInputs().get(MatlabConstants.HTML_CODE_COV_REPORT);
            if (htmlCoverage != null) {
                File reportFolder = new File(getWorkspace(), htmlCoverage);
                if (reportFolder.getParentFile() != null) {

                    // Create folders to keep .zip files
                    reportFolder.getParentFile().mkdirs();
                    zipFolder(new File(runner.getTempDirectory(), FilenameUtils.removeExtension(reportFolder.getName())), reportFolder);
                } else {
                    zipFolder(new File(runner.getTempDirectory(), FilenameUtils.removeExtension(reportFolder.getName())), reportFolder);
                }
            }
            // Delete all resource files used
            // runner.cleanUp(logger());
        } catch (Exception e) {
            throw new RunBuildException(e);
        }
    }

    /**
     * Executes cleanup activities after the build 
     */
    @Override
    public void afterProcessFinished() throws RunBuildException {
        cleanUp();
        super.afterProcessFinished();
    }
}
