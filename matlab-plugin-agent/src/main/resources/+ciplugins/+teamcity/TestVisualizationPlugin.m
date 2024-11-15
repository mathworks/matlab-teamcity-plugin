classdef TestVisualizationPlugin < matlab.unittest.plugins.TestRunnerPlugin

%   Copyright 2024 The MathWorks, Inc.

    methods (Access=protected)

        % Write about covered edgecases as well for function based tests instead of class based tests

            function runTest(plugin, pluginData)
                classAndTestName = strrep(pluginData.Name,"/",".");
                % fprintf("##teamcity[testStarted name='%s' captureStandardOutput='true']",pluginData.Name)
                % fprintf("##teamcity[testStarted name='%s' captureStandardOutput='true']",pluginData.TestSuite.TestClass + "." + pluginData.TestSuite.ProcedureName)
                % In the above method -> Test class becomes null for function based tests
                fprintf("##teamcity[testStarted name='%s' captureStandardOutput='true']", classAndTestName)

                % Invoke super class method to run the test
                runTest@matlab.unittest.plugins.TestRunnerPlugin(plugin, pluginData);

                result = pluginData.TestResult;

                if result.Failed

                    % display(result.Details.DiagnosticRecord.Report);
                    
                    % Not using fprintf as it treats / as a backslash escape character, which we recieve in the error logs like  In L:\Projects\matlab-teamcity-plugin. (errors out)
                    % fprintf(result.Details.DiagnosticRecord.Report);

                    % fprintf("##teamcity[testFailed name='%s' errorDetails='%s' status='ERROR']", pluginData.Name, result.Details.DiagnosticRecord.Report); % possibly include error details here
                    % fprintf("##teamcity[testFailed name='%s' status='ERROR']", pluginData.Name);
                    fprintf("##teamcity[testFailed name='%s' status='ERROR']", classAndTestName);
                elseif result.Incomplete
                    % fprintf("##teamcity[testIgnored name='%s']", pluginData.Name);
                    fprintf("##teamcity[testIgnored name='%s']", classAndTestName);
                end    

                % fprintf("##teamcity[testFinished name='%s' duration='%f']", pluginData.Name, result.Duration)
                fprintf("##teamcity[testFinished name='%s' duration='%f']", classAndTestName, result.Duration)
            end

            % pluginData.TestResult.Passed | pluginData.TestResult.Failed | pluginData.TestResult.Incomplete

            % function runTestSuite(plugin, pluginData)
            %     fprintf("##teamcity[testSuiteStarted name='%s']","suiteName")

            %     groupNumber = pluginData.Group;
            %     totalGroups = pluginData.NumGroups;
            %     suiteSize = numel(pluginData.TestSuite);
            %     fprintf('### Running %d tests in Group %d of %d\n', suiteSize, groupNumber, totalGroups);
                
            %     % Invoke the super class method
            %     runTestSuite@matlab.unittest.plugins.TestRunnerPlugin(plugin, pluginData)
                
            %     fprintf("##teamcity[testSuiteFinished name='%s']","suiteName")
            %     % pluginData.TestSuite.TestClass
            % end
            
            function runSession(plugin, pluginData)
                           % Introspect into pluginData to get TestSuite size
              suiteSize = numel(pluginData.TestSuite);
                fprintf('### Running a total of %d tests\n', suiteSize);
                 fprintf('test run session started\n');
                %  fprintf("##teamcity[testRetrySupport enabled='true']");
                % Invoke the super class method
                runSession@matlab.unittest.plugins.TestRunnerPlugin(plugin, pluginData)
                fprintf("##teamcity[testRetrySupport enabled='true']");
                fprintf('test run session ended');
            end

        end
end