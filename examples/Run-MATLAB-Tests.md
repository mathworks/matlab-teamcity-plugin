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

![create_project_90_full](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/63995bd2-b0a7-4962-b7fe-62fceced2ea3)

2. To run MATLAB tests as part of your build, add the **Run MATLAB Tests** build step by clicking **Build Steps** on the navigation bar and then **Add build step** on the build configuration page. You can access the step by typing `MATLAB` in the **New Build Step** box. 

![add_build_step](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/cdb590b5-46a7-4be4-9130-5f940af1f232)

3. In the **Run MATLAB Tests** step configuration interface, specify the full path to the root folder of your preferred MATLAB version in the **MATLAB root** box. Then, specify the artifacts to generate in the working directory. In this example, the plugin uses MATLAB R2023b to run the tests and generate JUnit-style test results, a PDF test report, and an HTML code coverage report. (To publish the JUnit-style test results, you need to add the [XML Report Processing](https://www.jetbrains.com/help/teamcity/xml-report-processing.html) build feature to your build configuration.) For more information about the build steps provided by the plugin, see [Plugin Configuration Guide](../CONFIGDOC.md).

![run_matlab_tests](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/8291e1ea-fb61-46bc-bef7-261816f60111)

4. Because your build produces artifacts, you must specify their paths at the build-configuration level. To specify the paths, click **General Settings** on the navigation bar and then populate the **Artifact paths** box. The pattern `matlab-artifacts/**/*` in this example matches all the artifacts in the `matlab-artifacts` folder and any of its subfolders.

![artifact_paths](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/6c64fd79-47bd-4650-a22d-ff15a87603e1)

## Run Tests and Inspect Artifacts
Now that your build configuration is complete, you can run a build. At the upper-right corner of the page, click **Run**. In this example, the build succeeds because all of the tests in the Times Table App project pass.

To access the test and coverage artifacts, select the **Artifacts** and **Code Coverage** tabs on the build results page. You can also create custom tabs to view the build artifacts. For more information, see [Including Third-Party Reports in the Build Results](https://www.jetbrains.com/help/teamcity/including-third-party-reports-in-the-build-results.html). 

![build_results](https://github.com/mathworks/matlab-teamcity-plugin/assets/48831250/6d72b781-d99b-4554-85b3-e22f51e3eb29)

## See Also
* [Plugin Configuration Guide](../CONFIGDOC.md)<br/>
* [Explore an Example Project (MATLAB)](https://www.mathworks.com/help/matlab/matlab_prog/explore-an-example-project.html)
