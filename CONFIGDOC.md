# Plugin Configuration Guide
This guide shows how to configure the plugin so that you can build and test your MATLAB&reg; project as part of your TeamCity&reg; build. For example, you can automatically identify any code issues in your project, run tests and generate test and coverage artifacts, and package your files into a toolbox.

-  [Use MATLAB as Executable](#use-matlab-as-executable)
-  [Configure Build Steps](#configure-build-steps)
      -  [Run MATLAB Build](#run-matlab-build)
      -  [Run MATLAB Tests](#run-matlab-tests)
         - [Specify Source Folder](#specify-source-folder)
         - [Filter Tests](#filter-tests)
         - [Customize Test Run](#customize-test-run)
         - [Generate Test and Coverage Artifacts](#generate-test-and-coverage-artifacts)
      -  [Run MATLAB Command](#run-matlab-command)

## Use MATLAB as Executable
To run MATLAB code and Simulink&reg; models using this plugin, you must have MATLAB installed on your build agent. When you add any of the build steps supported by the plugin to your build configuration, specify a MATLAB executable to use for the step. To do this, specify the full path to the root folder of your preferred MATLAB version in the **MATLAB root** box of the build step configuration interface.

You can use the [`matlabroot`](https://www.mathworks.com/help/matlab/ref/matlabroot.html) function to return the full path to your MATLAB root folder. The path depends on the platform, MATLAB version, and installation location. This table shows examples of the root folder path on different platforms. 

   | Platform     | Path to MATLAB Root Folder      |
   |--------------|---------------------------------|
   | Windows&reg; | C:\Program Files\MATLAB\R2023b  |
   | Linux&reg;   | /usr/local/MATLAB/R2023b        |
   | macOS        | /Applications/MATLAB_R2023b.app |

You can also specify optional startup options for a MATLAB executable by populating the **Startup options** box of the build step configuration interface. For example, specify `-nojvm` to start MATLAB without the JVM&trade; software. If you specify more than one startup option, use a space to separate them. For more information about MATLAB startup options, see [Commonly Used Startup Options](https://www.mathworks.com/help/matlab/matlab_env/commonly-used-startup-options.html).

![matlab_executable_90_full](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/f621b4dc-6afa-47e0-aa78-f2170f8c0d9a)

> :information_source: **Note:** Using the **Startup options** box to specify the `-batch` or `-r` option is not supported.

## Configure Build Steps
The plugin provides you with three build steps: 

* To run a MATLAB build, use the [Run MATLAB Build](#run-matlab-build) step.
* To run MATLAB and Simulink tests and generate artifacts, use the [Run MATLAB Tests](#run-matlab-tests) step.
* To run a MATLAB script, function, or statement, use the [Run MATLAB Command](#run-matlab-command) step.

### Run MATLAB Build
The **Run MATLAB Build** step enables you to run a build using the [MATLAB build tool](https://www.mathworks.com/help/matlab/matlab_prog/overview-of-matlab-build-tool.html). You can use this step to run the tasks specified in a file named `buildfile.m` in the root of your repository. To use the **Run MATLAB Build** step, you need MATLAB R2022b or a later release.

To configure the **Run MATLAB Build** step, first specify the MATLAB executable and optional startup options to use for the step. Then, specify the tasks you want to execute in the **Tasks** box. If you specify more than one task, use a space to separate them. If you do not specify any tasks, the plugin runs the default tasks in `buildfile.m` as well as all the tasks on which they depend. For example, use MATLAB R2023b to run a task named `mytask` as well as all the tasks on which it depends.

![run_matlab_build](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/374739af-2672-4498-b560-03dd983f975d)

MATLAB exits with exit code 0 if the build runs successfully. Otherwise, MATLAB terminates with a nonzero exit code, which causes the TeamCity build to fail.

When you use this step, a file named `buildfile.m` must be in the root of your repository. For more information about the build tool, see [Create and Run Tasks Using Build Tool](https://www.mathworks.com/help/matlab/matlab_prog/create-and-run-tasks-using-build-tool.html).

### Run MATLAB Tests
The **Run MATLAB Tests** step enables you to run MATLAB and Simulink tests and generate artifacts such as JUnit-style test results and HTML code coverage reports. By default, the plugin includes any test files in your [MATLAB project](https://www.mathworks.com/help/matlab/projects.html) that have a `Test` label. If your build does not use a MATLAB project, or if it uses a MATLAB release before R2019a, then the plugin includes all tests in the root of your repository and in any of its subfolders. The TeamCity build fails if any of the included tests fails.

To configure the **Run MATLAB Tests** step, specify the MATLAB executable and optional startup options to use for the step. For example, use MATLAB R2023b to run the tests in your MATLAB project.

![run_matlab_tests](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/6dcb2a04-c996-4c23-abef-22e2620b8238)

You can customize the **Run MATLAB Tests** step by selecting options in the build step configuration interface. For example, you can add source folders to the MATLAB search path, control which tests to run, and generate various test and coverage artifacts. If you do not select any of the existing options, all the tests in your project run, and any test failure causes the build to fail.

#### Specify Source Folder
You can specify the location of your source code in the **Source folder** box. When you specify the location of a folder relative to the root of your repository, the plugin adds the specified folder and its subfolders to the top of the MATLAB search path. If you specify a source folder and then generate a code coverage report, the plugin uses only the source code in the specified folder and its subfolders to generate the report. 

If you specify more than one folder in the **Source folder** box, use a colon or semicolon to separate them.

![source_folder](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/631fa6b7-6abd-44a3-af71-0f0e96a5a7e2)

#### Filter Tests
By default, the **Run MATLAB Tests** step creates a test suite from all the tests in your MATLAB project. You can create a filtered test suite by including only the tests in specified folders, the tests with a specified tag, or both:

* To create a test suite from a folder containing test files, specify the location of the folder relative to the root of your repository in the **By folder** box. The plugin creates a test suite using only the tests in the specified folder and its subfolders. 

  If you specify more than one folder in the **By folder** box, use a colon or semicolon to separate them.

* To create a test suite by using a test tag, specify the tag in the **By tag** box. The plugin creates a test suite by including only the tests with the specified tag.

![filter_tests](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/af4032e5-ef0c-416a-80ba-fc1028b7afd4)

#### Customize Test Run
To customize your test run, select options in the **Customize Test Run** section:

* To apply strict checks when running the tests, select **Strict**. If you select this option, the plugin generates a qualification failure whenever a test issues a warning. Selecting **Strict** is the same as specifying the `Strict` name-value argument of the [`runtests`](https://www.mathworks.com/help/matlab/ref/runtests.html) function as `true`.
* To run tests in parallel, select **Use parallel**. Selecting **Use parallel** is the same as specifying the `UseParallel` name-value argument of `runtests` as `true`. You must have Parallel Computing Toolbox&trade; installed to use this option. If other selected options are not compatible with running tests in parallel, the plugin runs the tests in serial regardless of your selection.
* To control the amount of output detail displayed for your test run, select a value from the **Output detail** list. Selecting a value for this option is the same as specifying the `OutputDetail` name-value argument of `runtests` as that value. By default, the plugin displays failing and logged events at the `Detailed` level and test run progress at the `Concise` level.
* To include diagnostics logged by the [`log (TestCase)`](https://www.mathworks.com/help/matlab/ref/matlab.unittest.testcase.log.html) and [`log (Fixture)`](https://www.mathworks.com/help/matlab/ref/matlab.unittest.fixtures.fixture.log.html) methods at a specified verbosity level, select a value from the **Logging level** list. Selecting a value for this option is the same as specifying the `LoggingLevel` name-value argument of `runtests` as that value. By default, the plugin includes diagnostics logged at the `Terse` level. 

![customize_test_run](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/5a462131-f04b-4cb5-9fa8-c6cce3813f53)

#### Generate Test and Coverage Artifacts
To generate test and coverage artifacts, specify the paths to store the artifacts in the **Generate Test and Coverage Artifacts** section. 

For example, run your tests, and generate test results in JUnit-style XML format and a code coverage report in HTML format at the specified locations in your working directory.

![generate_artifacts](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/e7cbafca-acdc-4375-a4b6-ea8be1de0cc1)

Paths for HTML reports are subject to these requirements: 
* To generate an HTML test or coverage report, you must specify a path to a ZIP archive that contains `index.html` as the main file of the report. For example, to generate an HTML code coverage report, specify the path to a file named `coverage.zip` in the **HTML code coverage report** box.
* The ZIP archive of the code coverage report must be in the artifacts root directory. For more information, see [Importing Arbitrary Coverage Results to TeamCity](https://www.jetbrains.com/help/teamcity/importing-arbitrary-coverage-results-to-teamcity.html).

### Run MATLAB Command
The **Run MATLAB Command** step enables you to run MATLAB scripts, functions, and statements. You can use this step to customize your test run or add a step in MATLAB to your build.

To configure the **Run MATLAB Command** step, first specify the MATLAB executable and optional startup options to use for the step. Then, specify the MATLAB script, function, or statement you want to execute in the **Command** box. If you specify more than one script, function, or statement, use a comma or semicolon to separate them. If you want to run a script or function, do not specify the file extension. For example, use MATLAB R2023b to run a script named `myscript.m` in the root of your repository.

![run_matlab_command](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/f828b1e8-dd4f-4436-8a47-9804409b6f4f)

MATLAB exits with exit code 0 if the specified script, function, or statement executes successfully without error. Otherwise, MATLAB terminates with a nonzero exit code, which causes the TeamCity build to fail. To fail the build in certain conditions, use the [`assert`](https://www.mathworks.com/help/matlab/ref/assert.html) or [`error`](https://www.mathworks.com/help/matlab/ref/error.html) function.

When you use this step, all of the required files must be on the MATLAB search path. If your script or function is not in the root of your repository, you can use the [`addpath`](https://www.mathworks.com/help/matlab/ref/addpath.html), [`cd`](https://www.mathworks.com/help/matlab/ref/cd.html), or [`run`](https://www.mathworks.com/help/matlab/ref/run.html) function to ensure that it is on the path when invoked. For example, to run `myscript.m` in a folder named `myfolder` located in the root of the repository, you can specify the contents of the **Command** box like this:

`addpath("myfolder"), myscript`

## See Also
* [Run MATLAB Tests with TeamCity](./examples/Run-MATLAB-Tests.md)<br/>
* [Continuous Integration with MATLAB and Simulink](https://www.mathworks.com/solutions/continuous-integration.html)
* [Continuous Integration with MATLAB on CI Platforms](https://www.mathworks.com/help/matlab/matlab_prog/continuous-integration-with-matlab-on-ci-platforms.html)
