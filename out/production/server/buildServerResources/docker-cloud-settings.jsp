
<%@ page import="run.var.teamcity.cloud.docker.util.DockerCloudUtils" %>
<%@ page import="run.var.teamcity.cloud.docker.web.DockerCloudCheckConnectivityController" %>
<%@ page import="run.var.teamcity.cloud.docker.web.ContainerTestsController" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<!-- Disable IDEA warnings about unused variables. -->
<%--@elvariable id="resPath" type="java.lang.String"--%>
<c:set var="paramName" value="<%=DockerCloudUtils.IMAGES_PARAM%>"/>

</table>

<script type="text/javascript">
    <jsp:include page="/js/bs/blocks.js"/>
    <jsp:include page="/js/bs/blocksWithHeader.js"/>
</script>


<div class="dockerCloudSettings">

<h2 class="noBorder section-header">Docker Connection Settings</h2>

<script type="text/javascript">
    BS.LoadStyleSheetDynamically("<c:url value='${resPath}docker-cloud.css'/>");
</script>

<table class="runnerFormTable">
    <tbody>
    <tr>
        <th>Docker instance:&nbsp;<l:star/></th>
        <td>
            <p>
                <props:radioButtonProperty name="<%=DockerCloudUtils.USE_DEFAULT_SOCKET_PARAM%>" id="dockerCloudUseLocalInstance"
                                           value="true"/>
                <label for="dockerCloudUseLocalInstance">Use local Docker instance</label>
            </p>
            <p>
                <props:radioButtonProperty name="<%=DockerCloudUtils.USE_DEFAULT_SOCKET_PARAM%>" id="dockerCloudUseCustomInstance"
                                           value="false"/>
                <label for="dockerCloudUseCustomInstance">Use custom Docker instance URL</label>
            </p>
            <span class="error" id="error_<%=DockerCloudUtils.USE_DEFAULT_SOCKET_PARAM%>"></span>
            <p>
                <label for="dockerCloudDockerAddress">Address:&nbsp;<span id="addressStar"><l:star/></span>&nbsp;</label><props:textProperty name="<%=DockerCloudUtils.INSTANCE_URI%>" id="dockerCloudDockerAddress"
                                                                                                                                             className="longField"/>
                <a href="#" class="btn" id="dockerCloudCheckConnectionBtn">Check connection</a>
            </p>
            <span class="error" id="error_<%=DockerCloudUtils.INSTANCE_URI%>"></span>
            <span id="dockerCloudCheckConnectionBtnError" class="error"></span>
            <div class="hidden" id="dockerCloudCheckConnectionLoader"><i class="icon-refresh icon-spin"></i>&nbsp;Connecting to Docker instance...</div>
        </td>
    </tr>
</table>
<div id="dockerCloudCheckConnectionSuccess" class="successMessage hidden"></div>

<h2 class="noBorder section-header">Agent Images</h2>

<props:hiddenProperty name="<%=DockerCloudUtils.IMAGES_PARAM%>"/>
    <props:hiddenProperty name="run.var.teamcity.docker.cloud.tested_image"/>

<table class="settings" style="width: 75%; margin-left: 25%">
    <thead>
    <tr>
        <th class="name" style="width: 30%;">Profile</th>
        <th class="name" style="width: 30%;">Image name</th>
        <th class="name center" style="width: 15%;">Max Instance #</th>
        <th class="name center" style="width: 15%;">Delete on exit</th>
        <th class="dockerCloudCtrlCell" style="width: 10%;"></th>
    </tr>
    </thead>
    <tbody id="dockerCloudImagesTable">

    </tbody>
</table>

</div>

<bs:dialog dialogId="DockerCloudImageDialog" title="Add Image" closeCommand="BS.DockerImageDialog.close()"
           titleId="DockerImageDialogTitle">
    <div id="dockerCloudImageTabContainer" class="simpleTabs"></div>

    <div class="dockerCloudSettings" id="dockerCloudImageContainer">
        <div id="dockerCloudImageTab_general">
            <table class="dockerCloudSettings runnerFormTable">
                <tr>
                    <th><label for="dockerCloudImage_Profile">Profile name:&nbsp;<l:star/></label></th>
                    <td>
                        <input type="text" id="dockerCloudImage_Profile" class="longField"/>
                        <span class="error" id="dockerCloudImage_Profile_error"></span>
    <span class="smallNote">
      Docker image name to be started.
    </span>
                    </td>
                </tr>
                <tr>
                    <th><label for="dockerCloudImage_Image">Docker image:&nbsp;<l:star/></label></th>
                    <td>
                        <input type="text" id="dockerCloudImage_Image" class="longField"/>
                        <span class="error" id="dockerCloudImage_Image_error"></span>
    <span class="smallNote">
      Docker image name to be started.
    </span>
                    </td>
                </tr>
                <tr>
                    <th><label for="dockerCloudImage_Image">Maximum instance count:&nbsp;</label></th>
                    <td>
                        <input type="text" id="dockerCloudImage_MaxInstanceCount" class="longField"/>
                        <span class="error" id="dockerCloudImage_MaxInstanceCount_error"></span>
    <span class="smallNote">
      Docker image name to be started.
    </span>
                    </td>
                </tr>
                <tr>
                    <th>Management:</th>
                    <td>
                        <p>
                            <input type="checkbox" id="dockerCloudImage_RmOnExit"/>
                            <label for="dockerCloudImage_RmOnExit">Delete container on when cloud agent is stopped</label>
                        </p>
                        <p>
                            <input type="checkbox" id="dockerCloudImage_BindAgentProps"/>
                            <label for="dockerCloudImage_BindAgentProps">Bind agent properties file</label>
                            <span class="smallNoteAttention" id="dockerCloudImage_BindAgentProps_warning"></span>
                        </p>
                    </td>
                </tr>
                <tr>
                    <th><label for="dockerCloudImage_AgentHome">Agent home directory&nbsp;<l:star/></label></th>
                    <td>
                        <input type="text" id="dockerCloudImage_AgentHome" class="longField"/>
                        <span class="error" id="dockerCloudImage_AgentHome_error"></span>
                    </td>
                </tr>
                <tr>
                    <th><label for="dockerCloudImage_User">User:</label></th>
                    <td><input type="text" id="dockerCloudImage_User"/></td>
                </tr>
                <tr>
                    <th>
                        <label for="dockerCloudImage_WorkingDir">Working directory:</label>
                    </th>
                    <td>
                        <input type="text" id="dockerCloudImage_WorkingDir" class="longField"/>
                    </td>
                </tr>
            </table>
            <h4>Command:</h4>
            <div class="dockerCloudSimpleTables">
                <table class="settings">
                    <thead>
                    <tr><th class="name" style="width: 82%;">Entrypoint executable / arguments</th><th class="dockerCloudCtrlCell"></th></tr>
                    </thead>
                    <tbody id="dockerCloudImage_Entrypoint">
                    </tbody>
                </table>
                <table class="settings">
                    <thead>
                    <tr><th class="name" style="width: 82%;"> Command executable / arguments</th><th class="dockerCloudCtrlCell"></th></tr>
                    </thead>
                    <tbody id="dockerCloudImage_Cmd">
                    </tbody>
                </table>
            </div>
        </div>
        <div id="dockerCloudImageTab_run">
            <table class="dockerCloudSettings runnerFormTable">
                <tr>
                    <th><label for="dockerCloudImage_StopSignal">Stop signal:</label></th>
                    <td><input type="text" id="dockerCloudImage_StopSignal" data-bind="value: StopSignal" /></td>
                </tr>
            </table>
            <h4>Volumes:</h4>
            <table class="settings">
                <thead>
                <tr><th class="name" style="width: 35%">Host directory</th><th class="name" style="width: 35%">Container directory&nbsp;<l:star/></th><th class="name center" style="width: 20%;">Read only</th><th class="dockerCloudCtrlCell"></th></tr>
                </thead>
                <tbody id="dockerCloudImage_Volumes">
                </tbody>
            </table>
            <h4>Environment variables:</h4>
            <table class="settings">
                <thead>
                <tr><th class="name" style="width: 45%;">Name&nbsp;<l:star/></th><th class="name" style="width: 45%;">Value</th><th class="dockerCloudCtrlCell"></th></tr>
                </thead>
                <tbody id="dockerCloudImage_Env">
                </tbody>
            </table>
            <h4>Labels:</h4>
            <table class="settings">
                <thead>
                <tr><th class="name" style="width: 45%;">Key&nbsp;<l:star/></th><th class="name" style="width: 45%;">Value</th><th class="dockerCloudCtrlCell"></th></tr>
                </thead>
                <tbody id="dockerCloudImage_Labels">
                </tbody>
            </table>
        </div>
        <div id="dockerCloudImageTab_privileges">
            <table class="dockerCloudSettings runnerFormTable">
                <tr>
                    <th>
                        Privileged:
                    </th>
                    <td>
                        <input type="checkbox" id="dockerCloudImage_Privileged"/>
                        <label for="dockerCloudImage_Privileged">Extended privileges</label>
                    </td>
                </tr>

                <tr>
                    <th><label for="dockerCloudImage_CgroupParent">Cgroup parent:</label></th>
                    <td>
                        <input type="text" id="dockerCloudImage_CgroupParent"/>
                    </td>
                </tr>
            </table>
            <h4>Kernel capabilities:</h4>
            <div class="dockerCloudSimpleTables">
                <table class="settings">
                    <thead>
                    <tr><th class="name" style="width: 82%;">Added capabilities</th><th class="dockerCloudCtrlCell"></th></tr>
                    </thead>
                    <tbody id="dockerCloudImage_CapAdd">
                    </tbody>
                </table>
                <table class="settings">
                    <thead>
                    <tr><th class="name" style="width: 82%;"> Dropped capabilities</th><th class="dockerCloudCtrlCell"></th></tr>
                    </thead>
                    <tbody id="dockerCloudImage_CapDrop">
                    </tbody>
                </table>
            </div>
        </div>

    <div id="dockerCloudImageTab_network">
        <table class="dockerCloudSettings runnerFormTable">
            <tr>
                <th><label for="dockerCloudImage_Hostname">Hostname:</label></th>
                <td>
                    <input type="text" id="dockerCloudImage_Hostname"/>
                </td>
            </tr>
            <tr>
                <th><label for="dockerCloudImage_Domainname">Domain name:</label></th>
                <td>
                    <input type="text" id="dockerCloudImage_Domainname"/>
                </td>
            </tr>
            <tr>
                <th><label for="dockerCloudImage_NetworkMode">Network mode:</label></th>
                <td>
                    <table class="dockerCloudSubtable">
                        <tr>
                            <td>
                                <select id="dockerCloudImage_NetworkMode">
                                    <option value="default">Default</option>
                                    <option value="bridge">Bridge</option>
                                    <option value="host">Host</option>
                                    <option value="container">Container:</option>
                                    <option value="custom">Custom:</option>
                                    <option value="none">None</option>
                                </select>
                            </td>
                            <td>
                                <input type="text" id="dockerCloudImage_NetworkContainer" class="mediumField"/>
                                <input type="text" id="dockerCloudImage_NetworkCustom" class="mediumField"/>
                            </td>
                        </tr>
                        <tr>
                            <td></td>
                            <td>
                                <span class="error" id="dockerCloudImage_NetworkContainer_error"></span>
                                <span class="error" id="dockerCloudImage_NetworkCustom_error"></span>
                            </td>
                        </tr>
                    </table>
        </table>
        <h4>Exposed ports:</h4>
        <table class="settings">
            <thead>
            <tr><th class="name center" style="width: 32%">Host IP</th><th class="name center" style="width: 20%">Host port</th><th class="name center" style="width: 20%">Container Port&nbsp;<l:star/></th><th class="name center" style="width: 20%">Protocol</th><th class="dockerCloudCtrlCell"></th></tr>
            </thead>
            <tbody id="dockerCloudImage_Ports">
            </tbody>
        </table>
        <h4>DNS:</h4>
        <div class="dockerCloudSimpleTables">
            <table class="settings">
                <thead>
                <tr><th class="name" style="width: 82%;">Server Address&nbsp;<l:star/></th><th class="dockerCloudCtrlCell"></th></tr>
                </thead>
                <tbody id="dockerCloudImage_Dns">
                </tbody>
            </table>
            <table class="settings">
                <thead>
                <tr><th class="name" style="width: 82%;">Search domains&nbsp;<l:star/></th><th class="dockerCloudCtrlCell"></th></tr>
                </thead>
                <tbody id="dockerCloudImage_DnsSearch">
                </tbody>
            </table>
        </div>
        <h4>Extra hosts:</h4>
        <table class="settings">
            <thead>
            <tr><th class="name" style="width: 47%;">Name&nbsp;<l:star/></th><th class="name" style="width: 47%;">IP Address&nbsp;<l:star/></th><th class="dockerCloudCtrlCell"></th></tr>
            </thead>
            <tbody id="dockerCloudImage_ExtraHosts">
            </tbody>
        </table>
        <h4>Link container:</h4>
        <table class="settings">
            <thead>
            <tr><th class="name" style="width: 47%;">Container&nbsp;<l:star/></th><th class="name" style="width: 47%;">Alias&nbsp;<l:star/></th><th class="dockerCloudCtrlCell"></th></tr>
            </thead>
            <tbody id="dockerCloudImage_Links">
            </tbody>
        </table>
    </div>

    <div id="dockerCloudImageTab_resources">
        <table class="dockerCloudSettings runnerFormTable">
            <tr>
                <th><label for="dockerCloudImage_Memory">Memory:</label></th>
                <td>
                    <input type="text" class="textField" id="dockerCloudImage_Memory"/>
                    <select id="dockerCloudImage_MemoryUnit">
                        <option value="bytes" selected="selected">bytes</option>
                        <option value="KiB">KiB</option>
                        <option value="MiB">MiB</option>
                        <option value="GiB">GiB</option>
                    </select>
                    <span class="error" id="dockerCloudImage_Memory_error"></span>
                </td>
            </tr>
            <tr>
                <th>Swap:</th>
                <td>
                    <p>
                        <input type="checkbox" id="dockerCloudImage_MemorySwapUnlimited"/>
                        <label for="dockerCloudImage_MemorySwapUnlimited">Unlimited</label>
                    </p>
                    <p>
                        <input type="text" class="textField" id="dockerCloudImage_MemorySwap"/>
                        <select id="dockerCloudImage_MemorySwapUnit">
                            <option value="bytes" selected="selected">bytes</option>
                            <option value="KiB">KiB</option>
                            <option value="MiB">MiB</option>
                            <option value="GiB">GiB</option>
                        </select>
                        <span class="error" id="dockerCloudImage_Swap_error"></span>
                    </p>
                </td>
            </tr>
            <tr>
                <th><label for="dockerCloudImage_CpusetCpus">cpuset - CPUs:</label></th>
                <td>
                    <input type="text" id="dockerCloudImage_CpusetCpus" class="textField"/>
                </td>
            </tr>
            <tr>
                <th><label for="dockerCloudImage_CpusetMems">cpuset - MEMs:</label></th>
                <td>
                    <input type="text" class="textField" id="dockerCloudImage_CpusetMems"/>
                </td>
            </tr>
            <tr>
                <th><label for="dockerCloudImage_CpuShares">CPU Shares:</label></th>
                <td>
                    <input type="text" class="textField" id="dockerCloudImage_CpuShares"/>
                </td>
            </tr>
            <tr>
                <th><label for="dockerCloudImage_CpuPeriod">CPU Period:</label></th>
                <td>
                    <input type="text" class="textField" id="dockerCloudImage_CpuPeriod"/>
                </td>
            </tr>
            <tr>
                <th><label for="dockerCloudImage_BlkioWeight">Bulk IO weight:</label></th>
                <td>
                    <input type="text" id="dockerCloudImage_BlkioWeight" class="textField"/>
                </td>
            </tr>
        </table>

        <h4>Ulimit:</h4>
        <table class="settings">
            <thead>
            <tr><th class="name" style="width: 32%">Name&nbsp;<l:star/></th><th class="name" style="width: 31%">Soft limit&nbsp;<l:star/></th><th class="name" style="width: 31%">Hard limit&nbsp;<l:star/></th><th class="dockerCloudCtrlCell"></th></tr>
            </thead>
            <tbody id="dockerCloudImage_Ulimits">

            </tbody>
        </table>
    </div>

    <div id="dockerCloudImageTab_advanced">
        <table class="dockerCloudSettings runnerFormTable">
            <tr>
                <th>OOM killer</th>
                <td>
                    <input type="checkbox" id="dockerCloudImage_OomKillDisable" data-bind="checked: oom_kill_disable"/>
                    <label for="dockerCloudImage_OomKillDisable">Disable OOM killer</label>
                </td>
            </tr>
            <tr>
                <th><label for="dockerCloudImage_LogType">Logging drivers:</label></th>
                <td>
                    <input id="dockerCloudImage_LogType" type="text">
                </td>
            </tr>
        </table>
        <h4>Logging options:</h4>
        <table class="settings">
            <thead>
            <tr><th class="name" style="width: 47%;">Option Key&nbsp;<l:star/></th><th class="name" style="width: 47%;">Option Value</th><th class="dockerCloudCtrlCell"></th></tr>
            </thead>
            <tbody id="dockerCloudImage_LogConfig">
            </tbody>
        </table>
        <h4>Devices:</h4>
        <table class="settings">
            <thead>
            <tr><th class="name" style="width: 32%;">Host path&nbsp;<l:star/></th><th class="name" style="width: 32%;">Container path&nbsp;<l:star/></th><th class="name" style="width: 30%;">CGroup permissions&nbsp;<l:star/></th><th class="dockerCloudCtrlCell"></th></tr>
            </thead>
            <tbody id="dockerCloudImage_Devices">
            </tbody>
        </table>
    </div>

    </div>
    <div class="popupSaveButtonsBlock">
        <input type="button" class="btn" id="dockerTestImageButton" value="Test container"/>
        <input type="button" class="btn btn_primary" id="dockerAddImageButton" value="Add"/>
        <input type="button" class="btn" id="dockerCancelAddImageButton" value="Cancel"/>
    </div>
</bs:dialog>

<bs:dialog dialogId="DockerTestContainerDialog" title="Test Container"
           closeCommand="BS.DockerTestContainerDialog.close()">
    <div>
        <p>
            This test will create a container using the provided settings and detect the TeamCity agent as soon as it
            connects. The container will be automatically discarded on completion.
            <input type="button" class="btn" id="dockerStartImageTest" value="Start"/>
        </p>
        <div>
            <p><label for="dockerTestContainerOutput">Test output</label></p>
            <textarea id="dockerTestContainerOutput"
                      readonly="readonly"></textarea>
        </div>
    </div>
</bs:dialog>
<script type="text/javascript">
    $j.ajax({
        url: "<c:url value="${resPath}docker-cloud.js"/>",
        dataType: "script",
        success: function() {
            BS.Clouds.Docker.init('<%=DockerCloudUtils.DOCKER_DEFAULT_SOCKET_URI%>',
                    '${resPath}<%=DockerCloudCheckConnectivityController.PATH%>', '${resPath}<%=ContainerTestsController.PATH%>',
                    '<%=DockerCloudUtils.IMAGES_PARAM%>');
            /* Register callbacks to react on DOM changes.
            $j(document).ready (BS.Clouds.Docker._refreshSettingsState);

            Ajax.Responders.register({
                        onComplete: BS.Clouds.Docker._refreshSettingsState
                    }
            ); */
        },
        cache: true
    });
</script>

<table class="runnerFormTable">