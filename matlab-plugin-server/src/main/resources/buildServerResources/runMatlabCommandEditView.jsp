<%@ page import="com.mathworks.ci.MatlabConstants" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.admin.projects.RunnerPropertiesBean"/>
<c:set var="command" value="<%=MatlabConstants.MATLAB_COMMAND%>"/>
<c:set var="matlabRoot" value="<%=MatlabConstants.MATLAB_PATH%>"/>

<l:settingsGroup title="Specify MATLAB Executable">
    <tr>
        <th ><label for="${matlabRoot}">MATLAB root:<l:star/><a id="" class="helpIcon actionIconWrapper" onclick="BS.Util.showHelp(event, 'https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md', {width: 0, height: 0}); return false" style="" href="https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md" title="View help" showdiscardchangesmessage="false"><span class="actionIconWrapper"><span class="svg-icon actionIcon actionIconHelp"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><path d="M7.27 12h1.51v-1.45H7.27zm2.65-7.42A3.36 3.36 0 0 0 7.9 4a2.82 2.82 0 0 0-1.55.41 2.49 2.49 0 0 0-1.07 2.17h1.55a1.55 1.55 0 0 1 .26-.86.93.93 0 0 1 .91-.41 1 1 0 0 1 .87.33 1.26 1.26 0 0 1 .24.75 1.17 1.17 0 0 1-.24.61 1.34 1.34 0 0 1-.32.31l-.39.31a2.19 2.19 0 0 0-.71.8 4.39 4.39 0 0 0-.18 1.25h1.46a2.22 2.22 0 0 1 .07-.63 1.18 1.18 0 0 1 .41-.57l.38-.29a3.66 3.66 0 0 0 .78-.74 1.93 1.93 0 0 0 .35-1.18 2 2 0 0 0-.8-1.68zm3-1.53A7 7 0 1 0 13 13a7 7 0 0 0 0-9.95zM12 12a5.6 5.6 0 0 1-8 0 5.61 5.61 0 0 1 0-8 5.6 5.6 0 0 1 8 0 5.61 5.61 0 0 1 0 8z"></path></svg></span></span></a></label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${matlabRoot}" size="56" maxlength="260" />
                <span class="smallNote">Specify the path to the MATLAB executable.</span>
                <span class="error" id="error_${matlabRoot}"></span>
            </div>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Run MATLAB Command">
    <tr>
        <th >
            <label for="${command}">Command:<l:star/></label>
            <a id="" class="helpIcon actionIconWrapper" onclick="BS.Util.showHelp(event, 'https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md', {width: 0, height: 0}); return false" style="" href="https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md" title="View help" showdiscardchangesmessage="false"><span class="actionIconWrapper"><span class="svg-icon actionIcon actionIconHelp"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><path d="M7.27 12h1.51v-1.45H7.27zm2.65-7.42A3.36 3.36 0 0 0 7.9 4a2.82 2.82 0 0 0-1.55.41 2.49 2.49 0 0 0-1.07 2.17h1.55a1.55 1.55 0 0 1 .26-.86.93.93 0 0 1 .91-.41 1 1 0 0 1 .87.33 1.26 1.26 0 0 1 .24.75 1.17 1.17 0 0 1-.24.61 1.34 1.34 0 0 1-.32.31l-.39.31a2.19 2.19 0 0 0-.71.8 4.39 4.39 0 0 0-.18 1.25h1.46a2.22 2.22 0 0 1 .07-.63 1.18 1.18 0 0 1 .41-.57l.38-.29a3.66 3.66 0 0 0 .78-.74 1.93 1.93 0 0 0 .35-1.18 2 2 0 0 0-.8-1.68zm3-1.53A7 7 0 1 0 13 13a7 7 0 0 0 0-9.95zM12 12a5.6 5.6 0 0 1-8 0 5.61 5.61 0 0 1 0-8 5.6 5.6 0 0 1 8 0 5.61 5.61 0 0 1 0 8z"></path></svg></span></span></a>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${command}" size="56" maxlength="100" />
                <span class="error" id="error_${command}"></span>
                <span class="smallNote">Specify the MATLAB command to execute.</span>
            </div>
        </td>
    </tr>
</l:settingsGroup>