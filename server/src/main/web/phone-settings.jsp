<%@ page import="org.jivesoftware.util.JiveGlobals,
                 org.jivesoftware.util.ParamUtils,
                 org.jivesoftware.openfire.XMPPServer,
                 org.jivesoftware.openfire.container.PluginManager,
                 javax.servlet.http.HttpServletRequest,
                 java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Collection" %>
<%@ page import="org.jivesoftware.phone.*" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>
<%
    Logger Log = LoggerFactory.getLogger(getClass());

    // if we were not enabled before and we are now restart the plugin
    PluginManager pluginManager = XMPPServer.getInstance().getPluginManager();
    PhonePlugin plugin = (PhonePlugin) pluginManager.getPlugin("asterisk-im");
    if (plugin == null) {
        // Complain about not being able to get the plugin
        String msg = "Unable to acquire asterisk plugin instance!";
        Log.error(msg);
        throw new IllegalStateException(msg);
    }
//    String version = pluginManager.getVersion(plugin);
//    boolean updateAvailable = XMPPServer.getInstance().getUpdateManager().
//            getPluginUpdate(plugin.getName(), version) != null;

    String booleanParameter = request.getParameter("enabled");
    boolean enabled = ParamUtils.getBooleanParameter(request, "enabled", false);
    boolean isSave = request.getParameter("save") != null;
    HttpServletRequest req = null;
    boolean success = request.getParameter("success") != null;
    boolean isDelete = ParamUtils.getBooleanParameter(request, "deleteSuccess", false);
    boolean isCreate = request.getParameter("serverCreated") != null;
    boolean isEdit = request.getParameter("serverEdited") != null;

    Map<String, String> errors = new HashMap<String, String>();

    if (isSave) {

        // get displayed parameters from request
        req = request;
        Collection<PhoneOption> options = plugin.getOptions();

        // check parameters
        for (PhoneOption option : options) {
            String param = option.getParamName();
            String err = option.check(request.getParameter(param));
            if (err != null) {
                errors.put(param, err);
            }
        }

        // If there are no errors initialize the manager
        if (errors.size() == 0) {

            for (PhoneOption option : options) {
                String prop = option.getPropertyName();
                String param = option.getParamName();
                String val = request.getParameter(param);
                if (option.isPassword() && "passwordtf".equals(val)) {
                    continue;
                }
                JiveGlobals.setProperty(prop, val);
            }

            if (booleanParameter != null) {
                plugin.setEnabled(enabled);
            }
        }
    }
    Exception exception = null;
    try {
        enabled = plugin.isEnabled();
    }
    catch (Exception e) {
        /** Here we will draw up that there was an error loading the plugin **/
        exception = e;
    }

%>

<html>
<head>
    <title>General Settings</title>
    <meta name="pageID" content="item-phone-settings"/>

    <style type="text/css">

        .div-border {
            border: 1px solid #CCCCCC;
            -moz-border-radius: 3px;
        }

        table.settingsTable {
            display: block;
            border: 1px solid #BBBBBB;
            margin: 5px 0px 15px 0px;
        }

        table.settingsTable thead th {
            background-color: #EAF1F8;
            border-bottom: 1px solid #BBBBBB;
            padding: 3px 8px 3px 12px;
            font-weight: bold;
            text-align: left;
        }

        table.settingsTable tbody tr td {
            padding: 5px 10px 5px 15px;
        }

        table.settingsTable tbody tr td p {
            padding: 10px 0px 5px 0px;
        }

        table.settingsTable tr {
            padding: 0px 0px 10px 0px;
        }

        /* --------------------------------------------- */
        /*  Tooltip styles                               */
        /* --------------------------------------------- */
        .openfire-helpicon-with-tooltip {
            position: relative;
            display: inline-block;
        }

        .openfire-helpicon-with-tooltip .helpicon {
            display: block;
            float: left;
            width: 14px;
            height: 14px;
            background: transparent url('images/setup_helpicon.gif') no-repeat;
        }

        .openfire-helpicon-with-tooltip .tooltiptext {
            font-family: Arial, Helvetica sans-serif;
            font-size: small;
            visibility: hidden;
            width: 240px;
            background-color: #FFFBE2;
            color: black;
            text-align: center;
            border: 1px solid #bbb;
            padding: 5px;
            position: absolute;
            z-index: 1;
            bottom: 125%;
            left: 50%;
            margin-left: -120px;
            white-space: normal;
        }

        .openfire-helpicon-with-tooltip:hover .tooltiptext {
            visibility: visible;
        }
    </style>
</head>

<body>

<div id="phone-settings">

<p>
    Use the form below to edit Phone integration settings.
    Changing settings will cause the plugin to be reloaded.<br>
</p>

<% if (exception != null) { %>
<div class="jive-error" style="width: 400;">
    <table cellpadding="0" cellspacing="0" border="0">
        <tbody>
            <tr>
                <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16"
                                           border="0"></td>
                <td class="jive-icon-label">Error loading plugin, see error log for details.</td>
            </tr>
        </tbody>
    </table>
</div>
</body>
</html>
<% return;
} else if (success) { %>
<div class="success" style="width: 400;">
    Service settings updated successfully. It will take a couple seconds to reconnect to the
    telephony interface
</div>
<% }
else if (isDelete) { %>
<div class="success" style="width: 400;">
    Server deleted successfully
</div>
<% }
else if (isCreate) { %>
<div class="success" style="width: 400;">
    Server created successfully
</div>
<% }
else if (isEdit) { %>
<div class="success" style="width: 400;">
    Server edited successfully
</div>
<% }
else if (errors.size() > 0) { %>

<div class="jive-error" style="width: 400;">
    <table cellpadding="0" cellspacing="0" border="0">
        <tbody>
            <tr>
                <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16"
                                           border="0"></td>
                <td class="jive-icon-label">Error saving the service settings.</td>
            </tr>
        </tbody>
    </table>
</div>
<% } %>

<form action="phone-settings.jsp" method="get">
<div class="div-border" style="background-color : #EAF1F8; width: 225px; padding: 4px; ">
    <span style="font-weight:bold;">Asterisk-IM:</span>
        <span><input type="radio" name="enabled"
                     value="true" <%= enabled ? "checked" : ""%> />Enabled</span>
        <span><input type="radio" name="enabled"
                     value="false" <%=!enabled ? "checked" : ""%> />Disabled</span>
</div>
<br/>
<br/>

<div class="div-border" style="padding: 12px; width: 95%;">
    <table class="jive-table" cellspacing="0" width="100%">
        <th width="16px">&nbsp;</th>
        <th>Name</th>
        <th>Address</th>
        <th>Port</th>
        <th>Username</th>
        <th>Options</th>
        <% boolean hasServers = false;
            PhoneManager manager = plugin.getPhoneManager();
            if (enabled) {
                Collection<PhoneServer> servers = manager.getPhoneServers();
                for (PhoneServer phoneServer : servers) {
                    hasServers = true;
        %>
        <tr style="border-left: none;">
            <td width="16px">
                <% switch (manager.getPhoneServerStatus(phoneServer.getID())) {
                    case connected: %>
                <img src="images/connected.gif" alt="connected"/>
                <% break;
                    default: %>
                <img src="images/disconnected.gif" alt="disconnected"/>
                <% break;
                } %>
            </td>
            <td><%=phoneServer.getName()%></td>
            <td><%=phoneServer.getHostname()%></td>
            <td><%=phoneServer.getPort()%></td>
            <td><%=phoneServer.getUsername()%></td>
            <td>
                <a href="create-server.jsp?serverID=<%=phoneServer.getID()%>">
                    <img src="images/edit-16x16.gif" border="0" alt="Edit Server"/></a>
                <a href="delete-server.jsp?serverID=<%=phoneServer.getID()%>">
                    <img src="images/delete-16x16.gif" border="0" alt="Delete Server"/></a>
            </td>
        </tr>
        <% }
        } %>
        <% if (enabled && !hasServers) { %>
        <tr>
            <td colspan="6" align="center">No Servers Configured</td>
        </tr>
        <% }
        else if (!enabled) { %>
        <tr>
            <td colspan="6" align="center">Asterisk IM Not Enabled</td>
        </tr>
        <% }%>
        <% if (enabled) { %>
        <tr>
            <td colspan="6">
                <a href="create-server.jsp">
                    <img src="/images/add-16x16.gif" border="0" alt="add server"
                         style="margin-right: 3px;"/>Add Server</a>
            </td>
        </tr>
        <% }%>
    </table>
</div>
<br/>
<br/>

<div>
<table class="settingsTable" cellpadding="3" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th colspan="2">Configure Phone Manager</th>
    </tr>
</thead>
<tbody>
    <%

        Collection<PhoneOption> options = plugin.getOptions();
        boolean isRequired = false;
        for (PhoneOption option : options) {
            if (!isRequired) {
                isRequired = option.isRequired();
            }
            String parameterName = option.getParamName();
            String title = option.getTitle();
            String propertyName = option.getPropertyName();
            String description = option.getDescription();
            String type = "text";
            // FIXME: we need html escaping for the value we put in! ;jw
            String val = getParameter(req, parameterName, propertyName);
            if (val == null || "".equals(val)) {
                val = option.getDefaultValue();
            }
            if (option.isPassword()) {
                type = "password";
                val = "passwordtf";
            }
            if (option.getType() == PhoneOption.Type.textbox) {
    %>
    <tr>
        <td width="1%">
            <nobr><label for="<%= parameterName %>">
                <%= option.isRequired() ? "* " : "" %>
                <%= title %>:
            </label></nobr>
        </td>
        <td width="99%">
            <textarea cols="45" rows="5" name="<%= parameterName  %>" wrap="virtual"
                      id="<%= parameterName %>"><%= val %></textarea>
            <% if (description != null) { %>
            <div class="openfire-helpicon-with-tooltip"><span class="helpicon"></span><span class="tooltiptext"><%=description%></span></div>
            <% } %>
            <% if (errors.containsKey(parameterName)) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get(parameterName)%></span>
            <% } %>
        </td>
    </tr>
    <%

    }
    else if (option.getType() == PhoneOption.Type.flag) {
    %>
    <tr>
        <td width="1%">
            <nobr><label for="<%= parameterName %>">
                <%= option.isRequired() ? "* " : "" %>
                <%= title %>:
            </label></nobr>
        </td>
        <td width="99%">
            <span><input type="radio" name="<%= parameterName %>" id="<%= parameterName %>"
                         value="true" <%= "true".equals(val) ? "checked" : "" %> />
                Yes</span>
            <span><input type="radio" name="<%= parameterName %>"
                         value="false" <%= !"true".equals(val) ? "checked" : ""%> />
                No</span>
            <% if (description != null) { %>
            <div class="openfire-helpicon-with-tooltip"><span class="helpicon"></span><span class="tooltiptext"><%=description%></span></div>
            <% } %>
        </td>
    </tr>
    <%
    }
    else {
    %>
    <tr>
        <td width="1%">
            <nobr><label for="<%= parameterName %>">
                <%= option.isRequired() ? "* " : "" %>
                <%= title %>:
            </label></nobr>
        </td>
        <td width="99%">
            <input type="<%= type %>" size="30" maxlength="100" name="<%= parameterName %>"
                   value="<%= val %>"
                   id="<%= parameterName %>"/>
            <% if (description != null) { %>
            <div class="openfire-helpicon-with-tooltip"><span class="helpicon"></span><span class="tooltiptext"><%=description%></span></div>
            <% } %>
            <% if (errors.containsKey(parameterName)) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get(parameterName)%></span>
            <% } %>
        </td>
    </tr>
    <%
            }

        }
    %>
    <% if (isRequired) { %>
    <tr>
        <td colspan="2">
            <span class="jive-description">* Required fields</span>
        </td>
    </tr>
    <% } %>
</tbody>
</table>
</div>
<br/>

<input type="submit" name="save" value="Save"/>
</form>

</div>

</body>
</html>

<%!

    String getParameter(HttpServletRequest q, String pn, String cn) {
        String s;
        if (q != null) {
            s = q.getParameter(pn);
        }
        else {
            s = JiveGlobals.getProperty(cn);
        }
        if (s == null) {
            s = "";
        }
        return s;
    }

    boolean isParamPresent(HttpServletRequest request, String param) {
        String value = request.getParameter(param);
        if (value == null) {
            return false;
        }
        return !"".equals(value);
    }

%>
