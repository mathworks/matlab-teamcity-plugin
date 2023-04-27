<%@ page import="com.mathworks.ci.MatlabConstants" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.admin.projects.RunnerPropertiesBean"/>
<c:set var="matlabPathId" value="<%=MatlabConstants.MATLAB_ROOT%>"/>
<c:set var="sourceFolder" value="<%=MatlabConstants.SOURCE_FOLDER%>"/>
<c:set var="testByFolder" value="<%=MatlabConstants.FILTER_TEST%>"/>
<c:set var="testByTag" value="<%=MatlabConstants.FILTER_TAG%>"/>
<c:set var="runParallel" value="<%=MatlabConstants.RUN_PARALLEL%>"/>
<c:set var="strict" value="<%=MatlabConstants.STRICT%>"/>
<c:set var="outputDetail" value="<%=MatlabConstants.OUTPUT_DETAIL%>"/>
<c:set var="loggingLevel" value="<%=MatlabConstants.LOGGING_LEVEL%>"/>
<c:set var="pdfReport" value="<%=MatlabConstants.PDF_REPORT%>"/>
<c:set var="htmlReport" value="<%=MatlabConstants.HTML_REPORT%>"/>
<c:set var="tapReport" value="<%=MatlabConstants.TAP_REPORT%>"/>
<c:set var="junitReport" value="<%=MatlabConstants.JUNIT_REPORT%>"/>
<c:set var="htmlCodeCov" value="<%=MatlabConstants.HTML_CODE_COV_REPORT%>"/>


<l:settingsGroup title="Specify MATLAB Executable">
    <tr>
        <th ><label for="${matlabPathId}">MATLAB root:<l:star/><a id="" class="helpIcon actionIconWrapper" onclick="BS.Util.showHelp(event, 'https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md', {width: 0, height: 0}); return false" style="" href="https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md" title="View help" showdiscardchangesmessage="false"><span class="actionIconWrapper"><span class="svg-icon actionIcon actionIconHelp"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><path d="M7.27 12h1.51v-1.45H7.27zm2.65-7.42A3.36 3.36 0 0 0 7.9 4a2.82 2.82 0 0 0-1.55.41 2.49 2.49 0 0 0-1.07 2.17h1.55a1.55 1.55 0 0 1 .26-.86.93.93 0 0 1 .91-.41 1 1 0 0 1 .87.33 1.26 1.26 0 0 1 .24.75 1.17 1.17 0 0 1-.24.61 1.34 1.34 0 0 1-.32.31l-.39.31a2.19 2.19 0 0 0-.71.8 4.39 4.39 0 0 0-.18 1.25h1.46a2.22 2.22 0 0 1 .07-.63 1.18 1.18 0 0 1 .41-.57l.38-.29a3.66 3.66 0 0 0 .78-.74 1.93 1.93 0 0 0 .35-1.18 2 2 0 0 0-.8-1.68zm3-1.53A7 7 0 1 0 13 13a7 7 0 0 0 0-9.95zM12 12a5.6 5.6 0 0 1-8 0 5.61 5.61 0 0 1 0-8 5.6 5.6 0 0 1 8 0 5.61 5.61 0 0 1 0 8z"></path></svg></span></span></a>
        </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${matlabPathId}" size="56" />
                <span class="smallNote">Specify the path to the MATLAB root folder</span>
                <span class="error" id="error_${matlabPathId}"></span>
            </div>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Run MATLAB Tests">
    <tr>
        <th >
            <label for="${sourceFolder}">Source folder: </label><a id="" class="helpIcon actionIconWrapper" onclick="BS.Util.showHelp(event, 'https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md', {width: 0, height: 0}); return false" style="" href="https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md" title="View help" showdiscardchangesmessage="false"><span class="actionIconWrapper"><span class="svg-icon actionIcon actionIconHelp"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><path d="M7.27 12h1.51v-1.45H7.27zm2.65-7.42A3.36 3.36 0 0 0 7.9 4a2.82 2.82 0 0 0-1.55.41 2.49 2.49 0 0 0-1.07 2.17h1.55a1.55 1.55 0 0 1 .26-.86.93.93 0 0 1 .91-.41 1 1 0 0 1 .87.33 1.26 1.26 0 0 1 .24.75 1.17 1.17 0 0 1-.24.61 1.34 1.34 0 0 1-.32.31l-.39.31a2.19 2.19 0 0 0-.71.8 4.39 4.39 0 0 0-.18 1.25h1.46a2.22 2.22 0 0 1 .07-.63 1.18 1.18 0 0 1 .41-.57l.38-.29a3.66 3.66 0 0 0 .78-.74 1.93 1.93 0 0 0 .35-1.18 2 2 0 0 0-.8-1.68zm3-1.53A7 7 0 1 0 13 13a7 7 0 0 0 0-9.95zM12 12a5.6 5.6 0 0 1-8 0 5.61 5.61 0 0 1 0-8 5.6 5.6 0 0 1 8 0 5.61 5.61 0 0 1 0 8z"></path></svg></span></span></a>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${sourceFolder}" size="56" />
                <span class="error" id="error_${sourceFolder}"></span>
                <span class="smallNote">Specify one or more source folders.</span>
            </div>
        </td>
    </tr>
    <tr>
        <br/>
        <th> Filter Tests <a id="" class="helpIcon actionIconWrapper" onclick="BS.Util.showHelp(event, 'https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md', {width: 0, height: 0}); return false" style="" href="https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md" title="View help" showdiscardchangesmessage="false"><span class="actionIconWrapper"><span class="svg-icon actionIcon actionIconHelp"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><path d="M7.27 12h1.51v-1.45H7.27zm2.65-7.42A3.36 3.36 0 0 0 7.9 4a2.82 2.82 0 0 0-1.55.41 2.49 2.49 0 0 0-1.07 2.17h1.55a1.55 1.55 0 0 1 .26-.86.93.93 0 0 1 .91-.41 1 1 0 0 1 .87.33 1.26 1.26 0 0 1 .24.75 1.17 1.17 0 0 1-.24.61 1.34 1.34 0 0 1-.32.31l-.39.31a2.19 2.19 0 0 0-.71.8 4.39 4.39 0 0 0-.18 1.25h1.46a2.22 2.22 0 0 1 .07-.63 1.18 1.18 0 0 1 .41-.57l.38-.29a3.66 3.66 0 0 0 .78-.74 1.93 1.93 0 0 0 .35-1.18 2 2 0 0 0-.8-1.68zm3-1.53A7 7 0 1 0 13 13a7 7 0 0 0 0-9.95zM12 12a5.6 5.6 0 0 1-8 0 5.61 5.61 0 0 1 0-8 5.6 5.6 0 0 1 8 0 5.61 5.61 0 0 1 0 8z"></path></svg></span></span></a></th>
        <br/>
    </tr>
    <tr>
        <th >
            <label for="${testByFolder}">By folder: </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${testByFolder}" size="56" />
                <span class="error" id="error_${testByFolder}"></span>
                <span class="smallNote">Specify one or more test folders.</span>
            </div>
        </td>
    </tr>

    <tr>
        <th >
            <label for="${testByTag}">By tag: </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${testByTag}" size="56" />
                <span class="error" id="error_${testByTag}"></span>
                <span class="smallNote">Specify a test tag.</span>
            </div>
        </td>
    </tr>

    <tr>
        <br/>
        <th> Customize Test Run <a id="" class="helpIcon actionIconWrapper" onclick="BS.Util.showHelp(event, 'https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md', {width: 0, height: 0}); return false" style="" href="https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md" title="View help" showdiscardchangesmessage="false"><span class="actionIconWrapper"><span class="svg-icon actionIcon actionIconHelp"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><path d="M7.27 12h1.51v-1.45H7.27zm2.65-7.42A3.36 3.36 0 0 0 7.9 4a2.82 2.82 0 0 0-1.55.41 2.49 2.49 0 0 0-1.07 2.17h1.55a1.55 1.55 0 0 1 .26-.86.93.93 0 0 1 .91-.41 1 1 0 0 1 .87.33 1.26 1.26 0 0 1 .24.75 1.17 1.17 0 0 1-.24.61 1.34 1.34 0 0 1-.32.31l-.39.31a2.19 2.19 0 0 0-.71.8 4.39 4.39 0 0 0-.18 1.25h1.46a2.22 2.22 0 0 1 .07-.63 1.18 1.18 0 0 1 .41-.57l.38-.29a3.66 3.66 0 0 0 .78-.74 1.93 1.93 0 0 0 .35-1.18 2 2 0 0 0-.8-1.68zm3-1.53A7 7 0 1 0 13 13a7 7 0 0 0 0-9.95zM12 12a5.6 5.6 0 0 1-8 0 5.61 5.61 0 0 1 0-8 5.6 5.6 0 0 1 8 0 5.61 5.61 0 0 1 0 8z"></path></svg></span></span></a></th>
        <br/>
    </tr>
     <tr>
          <td>
             <props:checkboxProperty name="${strict}"/>
             <b><label for="${strict}" className="longField">Strict</label></b>
          </td>
     </tr>
     <tr>
           <td>
              <props:checkboxProperty name="${runParallel}"/>
              <b><label for="${runParallel}" className="longField">Use parallel</label></b>
           </td>
         </tr>
      <tr>
         <td>
            <b><label for="${outputDetail}">Output detail:</label></b>
         </td>
         <td>
             <props:selectProperty name="${outputDetail}" className="longField">
               <props:option id="default" value="Default" selected="${true}">Default</props:option>
               <props:option id="none" value="None">None</props:option>
               <props:option id="terse" value="Terse">Terse</props:option>
               <props:option id="concise" value="Concise">Concise</props:option>
               <props:option id="detailed" value="Detailed">Detailed</props:option>
               <props:option id="verbose" value="Verbose">Verbose</props:option>
             </props:selectProperty>
         </td>
      </tr>
      <tr>
         <td>
            <b><label for="${loggingLevel}">Logging level:</label></b>
         </td>
          <td>
             <props:selectProperty name="${loggingLevel}" className="longField">
               <props:option id="default" value="Default" selected="${true}">Default</props:option>
               <props:option id="none" value="None">None</props:option>
               <props:option id="terse" value="Terse">Terse</props:option>
               <props:option id="concise" value="Concise">Concise</props:option>
               <props:option id="detailed" value="Detailed">Detailed</props:option>
               <props:option id="verbose" value="Verbose">Verbose</props:option>
             </props:selectProperty>
          </td>
      </tr>
      <tr><td> &nbsp;</td></tr>

    <tr>
        <br/>
        <th colspan="100"> Generate Test and Coverage Artifacts <a id="" class="helpIcon actionIconWrapper" onclick="BS.Util.showHelp(event, 'https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md', {width: 0, height: 0}); return false" style="" href="https://github.com/mathworks/matlab-teamcity-plugin/blob/main/README.md" title="View help" showdiscardchangesmessage="false"><span class="actionIconWrapper"><span class="svg-icon actionIcon actionIconHelp"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><path d="M7.27 12h1.51v-1.45H7.27zm2.65-7.42A3.36 3.36 0 0 0 7.9 4a2.82 2.82 0 0 0-1.55.41 2.49 2.49 0 0 0-1.07 2.17h1.55a1.55 1.55 0 0 1 .26-.86.93.93 0 0 1 .91-.41 1 1 0 0 1 .87.33 1.26 1.26 0 0 1 .24.75 1.17 1.17 0 0 1-.24.61 1.34 1.34 0 0 1-.32.31l-.39.31a2.19 2.19 0 0 0-.71.8 4.39 4.39 0 0 0-.18 1.25h1.46a2.22 2.22 0 0 1 .07-.63 1.18 1.18 0 0 1 .41-.57l.38-.29a3.66 3.66 0 0 0 .78-.74 1.93 1.93 0 0 0 .35-1.18 2 2 0 0 0-.8-1.68zm3-1.53A7 7 0 1 0 13 13a7 7 0 0 0 0-9.95zM12 12a5.6 5.6 0 0 1-8 0 5.61 5.61 0 0 1 0-8 5.6 5.6 0 0 1 8 0 5.61 5.61 0 0 1 0 8z"></path></svg></span></span></a></th>
        <br/>
    </tr>

    <tr>
            <th >
                <label for="${tapReport}">TAP test results: </label>
            </th>
            <td>
                <div class="posRel">
                    <props:textProperty name="${tapReport}" size="56" />
                    <span class="error" id="error_${tapReport}"></span>
                    <span class="smallNote">Specify a path relative to the working directory.</span>
                </div>
            </td>
    </tr>

    <tr>
        <th >
            <label for="${junitReport}">JUnit-style test results: </label>
        </th>
                 <td>
                        <div class="posRel">
                            <props:textProperty name="${junitReport}" size="56" />
                            <span class="error" id="error_${htmlReport}"></span>
                            <span class="smallNote">Specify a path relative to the working directory.</span>
                        </div>
                 </td>
    </tr>

    <tr>
        <th >
            <label for="${pdfReport}">PDF test report: </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${pdfReport}" size="56" />
                <span class="error" id="error_${pdfReport}"></span>
                <span class="smallNote">Specify a path relative to the working directory.</span>
            </div>
        </td>
    </tr>

    <tr>
        <th >
            <label for="${htmlReport}">HTML test report: </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${htmlReport}" size="56" />
                <span class="error" id="error_${htmlReport}"></span>
                <span class="smallNote">Specify a path relative to the working directory.</span>
            </div>
        </td>
    </tr>

     <tr>
        <th >
          <label for="${htmlCodeCov}">HTML code coverage report: </label>
        </th>
             <td>
                <div class="posRel">
                   <props:textProperty name="${htmlCodeCov}" size="56" />
                   <span class="error" id="error_${htmlCodeCov}"></span>
                   <span class="smallNote">Specify a path relative to the artifact root directory.</span>
                 </div>
             </td>
     </tr>
</l:settingsGroup>