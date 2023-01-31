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
<c:set var="coberturaCodeCov" value="<%=MatlabConstants.COBERTURA_CODE_COV_REPORT%>"/>


<l:settingsGroup title="Specify MATLAB">
    <tr>
        <th ><label for="${matlabPathId}">Path to MATLAB executable: <l:star/></label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${matlabPathId}" size="56" maxlength="100" />
                <span class="smallNote">Enter path to matlab executable specific to the node.</span>
                <span class="error" id="error_${matlabPathId}"></span>
            </div>
        </td>
    </tr>
</l:settingsGroup>

<l:settingsGroup title="Run MATLAB Tests">
    <tr>
        <th >
            <label for="${sourceFolder}">Source folder: </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${sourceFolder}" size="56" maxlength="100" />
                <span class="error" id="error_${sourceFolder}"></span>
                <span class="smallNote">Enter semi-colon seperated folder names to be added on MATLAB search path.</span>
            </div>
        </td>
    </tr>
    <tr>
        <br/>
        <th> Filter Tests </th>
        <br/>
    </tr>
    <tr>
        <th >
            <label for="${testByFolder}">By folder: </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${testByFolder}" size="56" maxlength="100" />
                <span class="error" id="error_${testByFolder}"></span>
                <span class="smallNote">Enter semi-colon seperated folder names to generate test suites from.</span>
            </div>
        </td>
    </tr>

    <tr>
        <th >
            <label for="${testByTag}">By tag: </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${testByTag}" size="56" maxlength="100" />
                <span class="error" id="error_${testByTag}"></span>
                <span class="smallNote">Specify tag to select specific test elements.</span>
            </div>
        </td>
    </tr>

    <tr>
        <br/>
        <th> Test Customization </th>
        <br/>
    </tr>
    <tr>
      <td>
         <props:checkboxProperty name="${runParallel}"/>
         <b><label for="${runParallel}" className="longField">Use parallel</label></b>
      </td>
    </tr>

     <tr>
          <td>
             <props:checkboxProperty name="${strict}"/>
             <b><label for="${strict}" className="longField">Strict</label></b>
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
              <props:option id="verbose" value="Verbose">Verbose</props:option>
            </props:selectProperty>
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
               <props:option id="verbose" value="Verbose">Verbose</props:option>
             </props:selectProperty>
         </td>
      </tr>

    <tr>
        <br/>
        <th > Generate Test Artifacts </th>
        <br/>
    </tr>

    <tr>
        <th >
            <label for="${pdfReport}">PDF report: </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${pdfReport}" size="56" maxlength="100" />
                <span class="error" id="error_${pdfReport}"></span>
                <span class="smallNote">Enter file path to generate PDF Report.</span>
            </div>
        </td>
    </tr>

    <tr>
        <th >
            <label for="${htmlReport}">HTML test report: </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${htmlReport}" size="56" maxlength="100" />
                <span class="error" id="error_${htmlReport}"></span>
                <span class="smallNote">Enter file path to generate HTML test report.</span>
            </div>
        </td>
    </tr>

    <tr>
         <th >
             <label for="${junitReport}">JUnit-style test results: </label>
         </th>
         <td>
                <div class="posRel">
                    <props:textProperty name="${junitReport}" size="56" maxlength="100" />
                    <span class="error" id="error_${htmlReport}"></span>
                    <span class="smallNote">Enter file path to generate JUnit test report.</span>
                </div>
         </td>
    </tr>

    <tr>
          <th >
                 <label for="${coberturaCodeCov}">Cobertura code coverage: </label>
          </th>
         <td>
                    <div class="posRel">
                        <props:textProperty name="${coberturaCodeCov}" size="56" maxlength="100" />
                        <span class="error" id="error_${coberturaCodeCov}"></span>
                        <span class="smallNote">Enter file path to generate Cobertura code coverage report.</span>
                    </div>
         </td>
    </tr>

    <tr>
        <th >
            <label for="${tapReport}">TAP test results: </label>
        </th>
        <td>
            <div class="posRel">
                <props:textProperty name="${tapReport}" size="56" maxlength="100" />
                <span class="error" id="error_${tapReport}"></span>
                <span class="smallNote">Enter file path to generate TAP result Report.</span>
            </div>
        </td>
    </tr>
</l:settingsGroup>