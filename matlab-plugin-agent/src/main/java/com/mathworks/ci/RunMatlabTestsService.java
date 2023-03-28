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
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

public class RunMatlabTestsService extends MatlabService {
  private String uniqueTmpFldrName;

  public void setUniqueTmpFldrName(String uniqueTmpFldrName) {
    this.uniqueTmpFldrName = uniqueTmpFldrName;
  }

  @NotNull
  @Override
  public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    String matlabPath = getRunnerParameters().get(MatlabConstants.MATLAB_ROOT);
    setRunner(getRunnerContext());

    //Add MATLAB into PATH Variable
    addToPath(matlabPath);
    return new SimpleProgramCommandLine(getRunner(), getExecutable(), getBashCommands());
  }

  private List<String> getBashCommands(){
    setUniqueTmpFldrName(getUniqueNameForRunnerFile());

    try {
      //Copy Genscript in workspace
      File genscriptLocation = getFilePathForUniqueFolder(uniqueTmpFldrName);
      copyFileToWorkspace(MatlabConstants.MATLAB_SCRIPT_GENERATOR, new File(genscriptLocation, MatlabConstants.MATLAB_SCRIPT_GENERATOR));

      //Prepare workspace with temp script
      prepareTmpFldr(genscriptLocation, getRunnerScript(MatlabConstants.TEST_RUNNER_SCRIPT, getGenScriptParametersForTests()));
      return getBashCommandsToRunMatlabCommand(constructCommandForTest(genscriptLocation), uniqueTmpFldrName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String constructCommandForTest(File genscriptLocation) {
    final String matlabScriptName = "runner_" + genscriptLocation.getName();
    final String runCommand =
        "addpath('" + genscriptLocation.getAbsolutePath().replaceAll("'", "''") + "'); " + matlabScriptName + ",delete('.matlab" +
            File.separator + genscriptLocation.getName() + File.separator + matlabScriptName + ".m'),runnerScript,rmdir(tmpDir,'s')";
    return runCommand;
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

    args.add("'Test'");
    final String filterByTests = getEnVars().get(MatlabConstants.FILTER_TEST);
    if (filterByTests != null) {
      args.add("'SelectByFolder'," + getCellArray(filterByTests));
    }

    final String sourceFolders = getEnVars().get(MatlabConstants.SOURCE_FOLDER);
    if (sourceFolders != null) {
      args.add("'SourceFolder'," + getCellArray(sourceFolders));
    }

    final String filterByTag = getEnVars().get(MatlabConstants.FILTER_TAG);
    if (filterByTag != null) {
      args.add("'SelectByTag','" + filterByTag + "'");
    }

    final String runParallelTests = getEnVars().get(MatlabConstants.RUN_PARALLEL) == null ? "false"
        : getEnVars().get(MatlabConstants.RUN_PARALLEL);
    if (runParallelTests.equalsIgnoreCase("true") && runParallelTests != null) {
      args.add("'UseParallel'," + Boolean.valueOf(runParallelTests) + "");
    }

    final String useStrict = getEnVars().get(MatlabConstants.STRICT) == null ? "false" : "true";
    if (useStrict.equalsIgnoreCase(useStrict)) {
      args.add("'Strict'," + Boolean.valueOf(useStrict));
    }

    String outputDetail = getEnVars().get(MatlabConstants.OUTPUT_DETAIL);
    if (!outputDetail.equalsIgnoreCase("default")) {
      args.add("'OutputDetail','" + outputDetail + "'");
    }

    String loggingLevel = getEnVars().get(MatlabConstants.LOGGING_LEVEL);
    if (!loggingLevel.equalsIgnoreCase("default")) {
      args.add("'LoggingLevel','" + loggingLevel + "'");
    }

    final String pdfReport = getEnVars().get(MatlabConstants.PDF_REPORT);
    if (pdfReport != null) {
      args.add("'PDFTestReport','" + pdfReport + "'");
    }

    final String htmlReport = getEnVars().get(MatlabConstants.HTML_REPORT);
    if (htmlReport != null) {
      File reportFile = new File(htmlReport);
      args.add("'HTMLTestReport','" + ".matlab/" + uniqueTmpFldrName + "/" + FilenameUtils.removeExtension(reportFile.getName()) + "'");
    }

    final String tapReport = getEnVars().get(MatlabConstants.TAP_REPORT);
    if (tapReport != null) {
      args.add("'TAPTestResults','" + tapReport + "'");
    }

    final String junitReport = getEnVars().get(MatlabConstants.JUNIT_REPORT);
    if (junitReport != null) {
      args.add("'JUnitTestResults','" + junitReport + "'");
    }

    final String htmlCodeCoverage = getEnVars().get(MatlabConstants.HTML_CODE_COV_REPORT);
    if (htmlCodeCoverage != null) {
      File reportFile = new File(htmlCodeCoverage);
      args.add("'HTMLCodeCoverage','" + ".matlab/" + uniqueTmpFldrName + "/" + FilenameUtils.removeExtension(reportFile.getName()) + "'");
    }

    return String.join(",", args);
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

  private void cleanUp() throws RunBuildException {
    File tempFolder = new File(getProjectDir(), ".matlab/" + uniqueTmpFldrName);
    try {

      final String htmlReport = getEnVars().get(MatlabConstants.HTML_REPORT);
      if (htmlReport != null) {
        File reportFolder = new File(getProjectDir(), htmlReport);
        if (reportFolder.getParentFile() != null) {

          // Create folders to keep .zip files
          reportFolder.getParentFile().mkdirs();
          zipFolder(new File(tempFolder, FilenameUtils.removeExtension(reportFolder.getName())), reportFolder);
        } else {
          zipFolder(new File(tempFolder, FilenameUtils.removeExtension(reportFolder.getName())), reportFolder);
        }
      }

      final String htmlCoverage = getEnVars().get(MatlabConstants.HTML_CODE_COV_REPORT);
      if (htmlCoverage != null) {
        File reportFolder = new File(getProjectDir(), htmlCoverage);
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
    } catch (Exception e) {
      throw new RunBuildException(e);
    }
  }

  /**
   * Cleanup the temporary folders
   *
   * @throws RunBuildException
   */
  @Override
  public void afterProcessFinished() throws RunBuildException {
    cleanUp();
    super.afterProcessFinished();
  }
}
