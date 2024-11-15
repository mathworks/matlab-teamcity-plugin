classdef TestVisualizationPluginService < matlab.buildtool.internal.services.ciplugins.CITestRunnerPluginService
    % Copyright 2024 The MathWorks, Inc.
 
    methods
        function plugins = providePlugins(~, ~)
            plugins = ciplugins.teamcity.TestVisualizationPlugin();
        end
    end
end