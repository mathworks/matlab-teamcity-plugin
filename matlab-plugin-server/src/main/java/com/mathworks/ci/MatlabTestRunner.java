package com.mathworks.ci;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MatlabTestRunner extends RunType {

    private final PluginDescriptor descriptor;

    public MatlabTestRunner(RunTypeRegistry registry, PluginDescriptor descriptor){
        registry.registerRunType(this);
        this.descriptor = descriptor;
    }

    @NotNull
    @Override
    public String getType() {
        return MatlabConstants.TEST_RUNNER_TYPE;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return MatlabConstants.TEST_RUNNER_NAME;
    }

    @NotNull
    @Override
    public String getDescription() {
        return MatlabConstants.TEST_RUNNER_DESCRIPTION;
    }

    @Nullable
    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return null;
    }

    @Nullable
    @Override
    public String getEditRunnerParamsJspFilePath() {
        return descriptor.getPluginResourcesPath("runMatlabTestEditView.jsp");
    }

    @Nullable
    @Override
    public String getViewRunnerParamsJspFilePath() {
        return descriptor.getPluginResourcesPath("runMatlabTestsPropertiesView.jsp");
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return new HashMap<>();
    }
}