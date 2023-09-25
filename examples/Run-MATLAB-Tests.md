# Run MATLAB Tests with TeamCity

This example shows how to run a suite of MATLAB&reg; unit tests with TeamCity&reg;. The example demonstrates how to:

* Create a build configuration to run MATLAB tests hosted in a remote repository and generate test and coverage artifacts.
* Run the build and examine the test results and the generated artifacts.

The build runs the tests in the Times Table App MATLAB project (which requires R2019a or later). You can create a working copy of the project files and open the project in MATLAB by running a statement in the Command Window. The statement to run depends on your MATLAB release:

R2023a and Earlier                 | Starting in R2023b
-----------------------------------| ------------------------------------------------
`matlab.project.example.timesTable`| `openExample("matlab/TimesTableProjectExample")`

For more information about the Times Table App project, see [Explore an Example Project](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html).

## Prerequisites
To follow the steps in this example:

* TeamCity Server and the plugin for MATLAB must be installed. For more information, see [Install and Start TeamCity Server](https://www.jetbrains.com/help/teamcity/install-and-start-teamcity-server.html) and [Installing Additional Plugins](https://www.jetbrains.com/help/teamcity/installing-additional-plugins.html).
* MATLAB must be installed on your build agent.
* The Times Table App project must be under source control. For example, you can create a new repository for the project using your GitHub&reg; account. For more information, see [Use Source Control with Projects](https://www.mathworks.com/help/matlab/matlab_prog/use-source-control-with-projects.html).

## Create Build Configuration to Run MATLAB Tests
Create a build configuration by following these steps:

1. In your TeamCity UI, click **Create project** or **New project**. Then, in the build configuration creation wizard, select **From a repository URL** and specify the repository for your project. Provide the required information and click **Proceed**. TeamCity creates a project, build configuration, and VCS (version control settings) root for you. For more information on how to create build configurations in TeamCity, see [Creating and Editing Build Configurations](https://www.jetbrains.com/help/teamcity/creating-and-editing-build-configurations.html).

![create_project](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/5cadf769-4530-46e5-baee-96dd726b3529)

2. To run MATLAB tests as part of your build, add the **Run MATLAB Tests** build step by clicking **Build Steps** on the navigation bar and then **Add build step** on the build configuration page. You can access the step by typing `MATLAB` in the **New Build Step** box. 

![add_build_step](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/6b29bc9b-c1b5-4f4f-b813-b44ba6f0090d)

3. In the **Run MATLAB Tests** step configuration interface, specify the full path to the root folder of your preferred MATLAB version in the **MATLAB root** box. Then, specify the artifacts to generate in the working directory. In this example, the plugin uses MATLAB R2023a to run the tests and generate JUnit-style test results, a PDF test report, and an HTML code coverage report. (To publish the JUnit-style test results, you need to add the [XML Report Processing](https://www.jetbrains.com/help/teamcity/xml-report-processing.html) build feature to your build configuration.) For more information about the build steps provided by the plugin, see [Plugin Configuration Guide](../CONFIGDOC.md).

![run_matlab_tests](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/a6b4bd16-9184-4467-acc5-1010169b0d12)

4. Because your build produces artifacts, you must specify their paths at the build-configuration level. To specify the paths, click **General Settings** on the navigation bar and then populate the **Artifact paths** box. The pattern `matlab-artifacts/**/*` in this example matches all the artifacts in the `matlab-artifacts` folder and any of its subfolders.

![artifact_paths](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/5cb56bbf-9373-4c91-aa95-35fa763087e9)

## Run Tests and Inspect Artifacts
Now that your build configuration is complete, you can run a build. At the upper-right corner of the page, click **Run**. In this example, the build succeeds because all of the tests in the Times Table App project pass.

To access the test and coverage artifacts, select the **Artifacts** and **Code Coverage** tabs on the build results page. You can also create custom tabs to view the build artifacts. For more information, see [Including Third-Party Reports in the Build Results](https://www.jetbrains.com/help/teamcity/including-third-party-reports-in-the-build-results.html). 

![build_results](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/5d69c1d8-022d-4616-a99b-ca3cad25bed2)

## See Also
* [Plugin Configuration Guide](../CONFIGDOC.md)<br/>
* [Explore an Example Project (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html)
