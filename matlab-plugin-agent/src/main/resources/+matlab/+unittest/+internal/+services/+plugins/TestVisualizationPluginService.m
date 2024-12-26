classdef TestVisualizationPluginService < matlab.buildtool.internal.services.ciplugins.CITestRunnerPluginService
    % Copyright 2025 The MathWorks, Inc.
 
    methods
        function plugins = providePlugins(~, ~)
            plugins = ciplugins.teamcity.TestVisualizationPlugin();
        end
    end
end