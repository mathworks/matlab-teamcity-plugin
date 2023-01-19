package com.mathworks.ci;

public interface MatlabConstants {

  String TEST_RUNNER_TYPE = "matlabTestRunner";
  String TEST_RUNNER_NAME = "Run MATLAB Tests";
  String TEST_RUNNER_DESCRIPTION = "Runs all MATLAB tests within current workspace";
  String MATLAB_COMMAND = "matlabCommand";
  String MATLAB_TASKS = "matlabTasks";
  String BUILD_RUNNER_TYPE = "matlabBuildRunner";
  String MATLAB_PATH = "MatlabPathKey";
  String MATLAB_ROOT = "MatlabRoot";
  String COMMAND_RUNNER_TYPE = "matlabCommandRunner";
  String COMMAND_RUNNER_NAME = "Run MATLAB Command";
  String BUILD_RUNNER_NAME = "Run MATLAB Build";
  String COMMAND_RUNNER_DESCRIPTION = "Runs specific MATLAB command or MATLAB script";
  String BUILD_RUNNER_DESCRIPTION = "Runs MATLAB build task(s)";
  String FILTER_TEST = "filterTestFolderByName";
  String SOURCE_FOLDER = "sourceFolders";
  String FILTER_TAG = "filterTestByTag";
  String RUN_PARALLEL = "runTestParallel";
  String STRICT = "strict";
  String OUTPUT_DETAIL = "logOutputDetail";
  String LOGGING_LEVEL = "logLoggingLevel";
  String PDF_REPORT = "pdfTestArtifact";
  String HTML_REPORT = "htmlTestArtifact";
  String TAP_REPORT = "tapTestArtifact";
  String TEMP_MATLAB_FOLDER_NAME = ".matlab";
  // Matlab Runner files
  // Matlab Runner files
  static final String BAT_RUNNER_SCRIPT = "run_matlab_command.bat";
  static final String SHELL_RUNNER_SCRIPT = "run_matlab_command.sh";
  static final String MATLAB_SCRIPT_GENERATOR = "matlab-script-generator.zip";
  //Test runner file prefix
  static final String MATLAB_TEST_RUNNER_FILE_PREFIX = "runner_";

  //Temporary MATLAB folder name in workspace


  static final String NEW_LINE = System.getProperty("line.separator");

  //MATLAB Runner Script
  static final String TEST_RUNNER_SCRIPT = String.join(NEW_LINE,
      "tmpDir=tempname;",
      "mkdir(tmpDir);",
      "addpath(tmpDir);",
      "workspace = getenv('teamcity.build.workingDir');",
      "zipURL='${ZIP_FILE}';",
      "unzip(zipURL,tmpDir);",
      "testScript = genscript(${PARAMS});",
      "disp('Running MATLAB script with content:');",
      "disp(testScript.Contents);",
      "testScript.writeToFile(fullfile(tmpDir,'runnerScript.m'));",
      "fprintf('___________________________________\\n\\n');");
}

