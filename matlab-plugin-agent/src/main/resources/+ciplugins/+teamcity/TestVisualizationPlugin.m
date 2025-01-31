classdef TestVisualizationPlugin < matlab.unittest.plugins.TestRunnerPlugin

%   Copyright 2025 The MathWorks, Inc.

    methods (Access=protected)

        function runTestClass(plugin, pluginData)
            disp("##teamcity[blockOpened name ='" + pluginData.Name + "']");

            % Invoke the super class method
            runTestClass@matlab.unittest.plugins.TestRunnerPlugin(plugin, pluginData);

            disp("##teamcity[blockClosed name ='" + pluginData.Name + "']");
        end        

        function runTest(plugin, pluginData)
            classAndTestName = strrep(pluginData.Name,"/",".");
            
            fprintf("##teamcity[testStarted name='%s' captureStandardOutput='true']", classAndTestName)

            % Invoke super class method to run the test
            runTest@matlab.unittest.plugins.TestRunnerPlugin(plugin, pluginData);

            result = pluginData.TestResult;

            if result.Failed
                fprintf("##teamcity[testFailed name='%s' status='ERROR']", classAndTestName);
            elseif result.Incomplete
                fprintf("##teamcity[testIgnored name='%s']", classAndTestName);
            end    

            fprintf("##teamcity[testFinished name='%s' duration='%f']", classAndTestName, result.Duration)
        end

    end
end