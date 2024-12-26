classdef BuildVisualizationPlugin < matlab.buildtool.plugins.BuildRunnerPlugin
    
%   Copyright 2025 The MathWorks, Inc.
    
    methods (Access=protected)

        function runTaskGraph(plugin, pluginData)
            runTaskGraph@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);
        end

        function runTask(plugin, pluginData)
            disp("##teamcity[blockOpened name ='" + pluginData.TaskResults.Name + "']");
            disp("##teamcity[progressStart 'Running " + pluginData.TaskResults.Name + " Task']");
            
            runTask@matlab.buildtool.plugins.BuildRunnerPlugin(plugin, pluginData);
            
            disp("##teamcity[progressFinish 'Running " + pluginData.TaskResults.Name + " Task']");
            disp("##teamcity[blockClosed name ='" + pluginData.TaskResults.Name + "']");
        end

    end
end