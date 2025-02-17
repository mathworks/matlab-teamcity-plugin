package com.mathworks.ci;

public interface MatlabConstants {

  String TEST_RUNNER_TYPE = "matlabTestRunner";
  String TEST_RUNNER_NAME = "Run MATLAB Tests";
  String TEST_RUNNER_DESCRIPTION = "Run the tests in a MATLAB project.";
  String MATLAB_COMMAND = "matlabCommand";
  String MATLAB_TASKS = "matlabTasks";
  String BUILD_RUNNER_TYPE = "matlabBuildRunner";
  String MATLAB_PATH = "MatlabPathKey"; // What's the difference between this and root?
  String STARTUP_OPTIONS = "StartupOptions";
  String BUILD_OPTIONS = "BuildOptions";
  String COMMAND_RUNNER_TYPE = "matlabCommandRunner";
  String COMMAND_RUNNER_NAME = "Run MATLAB Command";
  String BUILD_RUNNER_NAME = "Run MATLAB Build";
  String COMMAND_RUNNER_DESCRIPTION = "Run MATLAB scripts, functions, and statements.";
  String BUILD_RUNNER_DESCRIPTION = "Run MATLAB build tasks.";
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
  String JUNIT_REPORT = "junitArtifact";
  String HTML_CODE_COV_REPORT = "htmlCoverage";
  String TEMP_MATLAB_FOLDER_NAME = ".matlab";
  // Matlab Runner files
  static final String RUN_EXE_WIN = "win64/run-matlab-command.exe";
  static final String RUN_EXE_MACA = "maca64/run-matlab-command";
  static final String RUN_EXE_MACI = "maci64/run-matlab-command";
  static final String RUN_EXE_LINUX = "glnxa64/run-matlab-command";
  static final String MATLAB_SCRIPT_GENERATOR = "matlab-script-generator.zip";
  
  // MATLAB default plugin paths
  static final String DEFAULT_PLUGIN = "+ciplugins/+teamcity/getDefaultPlugins.m";
  static final String BUILD_VISUALIZATION_PLUGIN = "+ciplugins/+teamcity/BuildVisualizationPlugin.m";
  static final String TEST_VISUALIZATION_PLUGIN = "+ciplugins/+teamcity/TestVisualizationPlugin.m";
  static final String TEST_VISUALIZATION_PLUGIN_SERVICE = "+matlab/+unittest/+internal/+services/+plugins/TestVisualizationPluginService.m";

  //Test runner file prefix
  static final String MATLAB_TEST_RUNNER_FILE_PREFIX = "runner_";

  static final String NEW_LINE = System.getProperty("line.separator");

  //MATLAB Runner Script
  static final String TEST_RUNNER_SCRIPT = String.join(NEW_LINE,
      "addpath('${TEMPFOLDER}');",
      "testScript = genscript(${PARAMS});",
      "disp('Running MATLAB script with content:');",
      "disp(testScript.Contents);",
      "fprintf('___________________________________\\n\\n');",
      "run(testScript);");
}

