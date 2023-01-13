<%@ page import="com.mathworks.ci.MatlabConstants" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.admin.projects.RunnerPropertiesBean"/>
<c:set var="matlabTasks" value="<%=MatlabConstants.MATLAB_TASKS%>"/>
<c:set var="matlabRoot" value="<%=MatlabConstants.MATLAB_PATH%>"/>

<l:settingsGroup title="Specify MATLAB">
    <tr>
        <th ><label for="${matlabRoot}">Path to MATLAB Executable: <l:star/></label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${matlabRoot}" size="56" maxlength="100" />
                <span class="smallNote">Enter path to matlab executable specific to the node.</span>
                <span class="error" id="error_${matlabRoot}"></span>
            </div>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Tasks">
    <tr>
        <th >
            <label for="${matlabTasks}">Tasks </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${matlabTasks}" size="56" maxlength="100" />
                <span class="error" id="error_${matlabTasks}"></span>
                <span class="smallNote">Insert a space-separated list of tasks to run in the Tasks box. If not specified, the action runs the default tasks in buildfile.m as well as all the tasks on which they depend.</span>
            </div>
        </td>
    </tr>
</l:settingsGroup>