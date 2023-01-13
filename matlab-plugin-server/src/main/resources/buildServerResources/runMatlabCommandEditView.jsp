<%@ page import="com.mathworks.ci.MatlabConstants" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.admin.projects.RunnerPropertiesBean"/>
<c:set var="matlabCommand" value="<%=MatlabConstants.MATLAB_COMMAND%>"/>
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

<l:settingsGroup title="Run MATLAB Command">
    <tr>
        <th >
            <label for="${matlabCommand}">MATLAB Command </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${matlabCommand}" size="56" maxlength="100" />
                <span class="error" id="error_${matlabCommand}"></span>
                <span class="smallNote">Enter valid MATLAB command or script to execute.</span>
            </div>
        </td>
    </tr>
</l:settingsGroup>