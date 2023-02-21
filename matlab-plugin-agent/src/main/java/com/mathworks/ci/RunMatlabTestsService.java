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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.BuildServiceAdapter;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

public class RunMatlabTestsService extends BuildServiceAdapter {

  private String uniqueTmpFldrName;

  @NotNull
  @Override
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    final BuildRunnerContext runner = getRunnerContext();
    final SimpleProgramCommandLine cmdExecutor;

    String matlabPath = getRunnerParameters().get(MatlabConstants.MATLAB_ROOT);

    //Add MATLAB into PATH Variable
    MatlabTaskUtils.addToPath(getRunnerContext(), matlabPath);
    uniqueTmpFldrName = MatlabTaskUtils.getUniqueNameForRunnerFile();

    try {
      //Copy Genscript in workspace
      File genscriptLocation = getFilePathForUniqueFolder(getRunnerContext(), uniqueTmpFldrName);
      MatlabTaskUtils.copyFileToWorkspace(MatlabConstants.MATLAB_SCRIPT_GENERATOR,
          new File(genscriptLocation, MatlabConstants.MATLAB_SCRIPT_GENERATOR));

      //Prepare workspace with temp script
      prepareTmpFldr(genscriptLocation, getRunnerScript(MatlabConstants.TEST_RUNNER_SCRIPT, getGenScriptParametersForTests()));
      cmdExecutor = MatlabTaskUtils.getProcessToRunMatlabCommand(runner, constructCommandForTest(genscriptLocation), uniqueTmpFldrName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return cmdExecutor;
  }

  private String constructCommandForTest(File genscriptLocation) {
    final String matlabScriptName = "runner_" + genscriptLocation.getName();
    final String runCommand =
        "addpath('" + genscriptLocation.getAbsolutePath().replaceAll("'", "''") + "'); " + matlabScriptName + ",delete('.matlab/"
            + genscriptLocation.getName() + "/" + matlabScriptName + ".m'),runnerScript,rmdir(tmpDir,'s')";
    return runCommand;
  }

  private File getFilePathForUniqueFolder(BuildRunnerContext runner, String uniqueTmpFldrName) throws IOException, InterruptedException {
    File tmpDir = new File(runner.getWorkingDirectory(), MatlabConstants.TEMP_MATLAB_FOLDER_NAME);
    tmpDir.mkdir();
    File genscriptlocation = new File(tmpDir, uniqueTmpFldrName);
    genscriptlocation.mkdir();
    genscriptlocation.setExecutable(true);
    return genscriptlocation;
  }

  // This method prepares the temp folder by coping all helper files in it.
  private void prepareTmpFldr(File tmpFldr, String runnerScript) throws IOException, InterruptedException {
    // genscript is copied
    File zipFileLocation = new File(tmpFldr, MatlabConstants.MATLAB_SCRIPT_GENERATOR);
    runnerScript = replaceZipPlaceholder(runnerScript, zipFileLocation.getPath());

    // Write MATLAB scratch file in temp folder.
    File scriptFile = new File(tmpFldr, MatlabConstants.MATLAB_TEST_RUNNER_FILE_PREFIX + tmpFldr.getName() + ".m");
    FileUtils.writeStringToFile(scriptFile, runnerScript);
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

  private String getGenScriptParametersForTests() {
    final List<String> args = new ArrayList<String>();
    final BuildRunnerContext runner = getRunnerContext();
    String outputDetail = "default";
    String loggingLevel = "default";

    args.add("'Test'");
    final String filterByTests = runner.getRunnerParameters().get(MatlabConstants.FILTER_TEST);
    if (filterByTests != null) {
      args.add("'SelectByFolder'," + getCellarray(filterByTests));
    }

    final String sourceFolders = runner.getRunnerParameters().get(MatlabConstants.SOURCE_FOLDER);
    if (sourceFolders != null) {
      args.add("'SourceFolder'," + getCellarray(sourceFolders));
    }

    final String filterByTag = runner.getRunnerParameters().get(MatlabConstants.FILTER_TAG);
    if (filterByTag != null) {
      args.add("'SelectByTag','" + filterByTag + "'");
    }

    final String runParallelTests = runner.getRunnerParameters().get(MatlabConstants.RUN_PARALLEL) == null ? "false"
        : runner.getRunnerParameters().get(MatlabConstants.RUN_PARALLEL);
    if (runParallelTests.equalsIgnoreCase("true") && runParallelTests != null) {
      args.add("'UseParallel'," + Boolean.valueOf(runParallelTests) + "");
    }

    final String useStrict = runner.getRunnerParameters().get(MatlabConstants.STRICT) == null ? "false" : "true";
    if (useStrict.equalsIgnoreCase(useStrict)) {
      args.add("'Strict'," + Boolean.valueOf(useStrict));
    }

    outputDetail = runner.getRunnerParameters().get(MatlabConstants.OUTPUT_DETAIL);
    if (!outputDetail.equalsIgnoreCase("default")) {
      args.add("'OutputDetail','" + outputDetail + "'");
    }

    loggingLevel = runner.getRunnerParameters().get(MatlabConstants.LOGGING_LEVEL);
    if (!loggingLevel.equalsIgnoreCase("default")) {
      args.add("'LoggingLevel','" + loggingLevel + "'");
    }

    final String pdfReport = runner.getRunnerParameters().get(MatlabConstants.PDF_REPORT);
    if (pdfReport != null) {
      args.add("'PDFTestReport','" + pdfReport + "'");
    }

    final String htmlReport = runner.getRunnerParameters().get(MatlabConstants.HTML_REPORT);
    if (htmlReport != null) {
      File reportFile = new File(htmlReport);
      args.add("'HTMLTestReport','" + ".matlab/" + uniqueTmpFldrName + "/" + FilenameUtils.removeExtension(reportFile.getName()) + "'");
    }

    final String tapReport = runner.getRunnerParameters().get(MatlabConstants.TAP_REPORT);
    if (tapReport != null) {
      args.add("'TAPTestResults','" + tapReport + "'");
    }

    final String junitReport = runner.getRunnerParameters().get(MatlabConstants.JUNIT_REPORT);
    if (junitReport != null) {
      args.add("'JUnitTestResults','" + junitReport + "'");
    }

    final String htmlCodeCoverage = runner.getRunnerParameters().get(MatlabConstants.HTML_CODE_COV_REPORT);
    if (htmlCodeCoverage != null) {
      File reportFile = new File(htmlCodeCoverage);
      args.add("'HTMLCodeCoverage','" + ".matlab/" + uniqueTmpFldrName + "/" + FilenameUtils.removeExtension(reportFile.getName()) + "'");
    }

    return String.join(",", args);
  }

  private String getCellarray(String folders) {
    final String[] folderNames = folders.split(";");
    return getCellArrayFrmList(Arrays.asList(folderNames));
  }

  public String getCellArrayFrmList(List<String> listOfStr) {
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
   *
   * @throws RunBuildException
   */
  @Override
  public void afterProcessFinished() throws RunBuildException {
    File tempFolder = new File(getRunnerContext().getWorkingDirectory(), ".matlab/" + uniqueTmpFldrName);
    try {

      final String htmlReport = getRunnerContext().getRunnerParameters().get(MatlabConstants.HTML_REPORT);
      if (htmlReport != null) {
        File reportFolder = new File(getRunnerContext().getWorkingDirectory(), htmlReport);
        if (reportFolder.getParentFile() != null) {

          // Create folders to keep .zip files
          reportFolder.getParentFile().mkdirs();
          zipFolder(new File(tempFolder, FilenameUtils.removeExtension(reportFolder.getName())), reportFolder);
        } else {
          zipFolder(new File(tempFolder, FilenameUtils.removeExtension(reportFolder.getName())), reportFolder);
        }
      }

      final String htmlCoverage = getRunnerContext().getRunnerParameters().get(MatlabConstants.HTML_CODE_COV_REPORT);
      if (htmlCoverage != null) {
        File reportFolder = new File(getRunnerContext().getWorkingDirectory(), htmlCoverage);
        if (reportFolder.getParentFile() != null) {

          // Create folders to keep .zip files
          reportFolder.getParentFile().mkdirs();
          zipFolder(new File(tempFolder, FilenameUtils.removeExtension(reportFolder.getName())), reportFolder);
        } else {
          zipFolder(new File(tempFolder, FilenameUtils.removeExtension(reportFolder.getName())), reportFolder);
        }
      }
      // Delete all resource files used
      FileUtils.deleteDirectory(tempFolder);
      super.afterProcessFinished();
    } catch (Exception e) {
      throw new RunBuildException(e);
    }
  }
}
