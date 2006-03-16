<%@ page import="org.jivesoftware.phone.PhoneManager,
                 org.jivesoftware.phone.PhoneManagerFactory,
                 org.jivesoftware.phone.asterisk.AsteriskPlugin,
                 org.jivesoftware.util.JiveConstants,
                 org.jivesoftware.util.JiveGlobals,
                 org.jivesoftware.util.Log,
                 org.jivesoftware.util.ParamUtils,
                 org.jivesoftware.wildfire.XMPPServer,
                 org.jivesoftware.wildfire.container.PluginManager" %>
<%@ page import="javax.servlet.http.HttpServletRequest"%>
<%@ page import="java.util.HashMap"%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%

    boolean isSave = request.getParameter("save") != null;
    boolean success = request.getParameter("success") != null;
    boolean usersDisabled = request.getParameter("usersDisabled") != null;

    String server = request.getParameter("server");
    int port = ParamUtils.getIntParameter(request, "port", -1);
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    boolean enabled = ParamUtils.getBooleanParameter(request, "enabled", false);
    String callerID = request.getParameter("callerID");
    String context = request.getParameter("context");
    String dialVariables = request.getParameter("dialVariables");

    HashMap<String, String> errors = new HashMap<String, String>();

    if (isSave) {

        if (server == null || "".equals(server)) {
            errors.put("server", "Server is required");
        }

        if (username == null || "".equals(username)) {
            errors.put("username", "Username is required");
        }

        if (password == null || "".equals(password)) {
            errors.put("password", "Password is required");
        }

        // If a port was specified make sure that it is valid
        if (isParamPresent(request, "port") && port < 1025) {
            errors.put("port", "Port must be greater than 1024 and not in use");
        }

        // If there are no errors initialize the manager
        if (errors.size() == 0) {

            JiveGlobals.setProperty(AsteriskPlugin.Properties.SERVER, server);
            JiveGlobals.setProperty(AsteriskPlugin.Properties.USERNAME, username);

            if (!"passwordtf".equals(password)) {
                JiveGlobals.setProperty(AsteriskPlugin.Properties.PASSWORD, password);
            }

            JiveGlobals.setProperty(AsteriskPlugin.Properties.DEFAULT_CALLER_ID, callerID);
            JiveGlobals.setProperty(AsteriskPlugin.Properties.CONTEXT, context);

            JiveGlobals.setProperty(AsteriskPlugin.Properties.ENABLED, String.valueOf(enabled));

            if (port > 1024) {
                JiveGlobals.setProperty(AsteriskPlugin.Properties.PORT, String.valueOf(port));
            }


            JiveGlobals.setProperty(AsteriskPlugin.Properties.DIAL_VARIABLES, dialVariables);

                // if we were not enabled before and we are now restart the plugin
            PluginManager pluginManager = XMPPServer.getInstance().getPluginManager();
            AsteriskPlugin plugin = (AsteriskPlugin) pluginManager.getPlugin(AsteriskPlugin.NAME);

            if (plugin != null) {
                plugin.destroy();
                try {
                    Thread.sleep(1 * JiveConstants.SECOND);
                }
                catch (InterruptedException e) {
                    Log.error(e);
                }

                plugin.init();
                try {
                    Thread.sleep(1 * JiveConstants.SECOND);
                }
                catch (InterruptedException e) {
                   Log.error(e);
                }
            }
            else {
                // Complain about not being able to get the plugin
                String msg = "Unable to acquire asterisk plugin instance!";
                Log.error(msg);
                throw new IllegalStateException(msg);
            }


            response.sendRedirect("phone-settings.jsp?success=true");
            return;

        }

    } else {

        // See what the values are
        server = JiveGlobals.getProperty(AsteriskPlugin.Properties.SERVER);
        port = JiveGlobals.getIntProperty(AsteriskPlugin.Properties.PORT, -1);
        username = JiveGlobals.getProperty(AsteriskPlugin.Properties.USERNAME);
        if (JiveGlobals.getProperty(AsteriskPlugin.Properties.PASSWORD) != null) {
            password = "passwordtf"; // show some value
        } else if (password == null) {
            password = "";
        }
        enabled = JiveGlobals.getBooleanProperty(AsteriskPlugin.Properties.ENABLED, false);
        callerID = JiveGlobals.getProperty(AsteriskPlugin.Properties.DEFAULT_CALLER_ID);
        context = JiveGlobals.getProperty(AsteriskPlugin.Properties.CONTEXT);
        dialVariables = JiveGlobals.getProperty(AsteriskPlugin.Properties.DIAL_VARIABLES);

    }

    //try to establish a connection, if there is an error we log it below
    boolean isConnected = false;

    if (enabled) {
        PhoneManager phoneManager = PhoneManagerFactory.getPhoneManager();
        isConnected = phoneManager != null && phoneManager.isConnected();
    }


%>

<html>
<head>
    <title>General Settings</title>
    <meta name="pageID" content="item-phone-settings"/>

  <style type="text/css">
    #enabledtf {}
  </style>
</head>
<body>

<div id="phone-settings">

<p>
    Use the form below to edit Asterisk integration settings.
    Changing settings will cause the plugin to be reloaded.<br>
</p>


<% if (success) { %>

<div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
        <tbody>
            <tr>
                <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
                <td class="jive-icon-label">Service settings updated successfully. It will take a couple seconds to
                    reconnect to the asterisk manager</td>
            </tr>
        </tbody>
    </table>
</div><br>

<%  } else if (errors.size() > 0) { %>

<div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
        <tbody>
            <tr>
                <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0"></td>
                <td class="jive-icon-label">Error saving the service settings.</td>
            </tr>
        </tbody>
    </table>
</div><br>

<% } else if (!isConnected) { %>

<p>

    <div class="jive-error">
        <table cellpadding="0" cellspacing="0" border="0">
            <tbody>
                <tr>
                    <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0"></td>
                    <td class="jive-icon-label">Unable to establish a connection to the manager server, please see error
                        log</td>
                </tr>
            </tbody>
        </table>
    </div><br>

</p>


<%  } else if (usersDisabled) { %>

<div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
        <tbody>
            <tr>
                <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0"></td>
                <td class="jive-icon-label">Plugin must be enabled to manage users</td>
            </tr>
        </tbody>
    </table>
</div><br>

<% } %>

<form action="phone-settings.jsp" method="get">
<fieldset>
<legend>General Configuration</legend>

<div>
<table cellpadding="3" cellspacing="0" border="0" width="100%">
<tbody>
    <tr>
        <td width="1%">
            <nobr><label for="enabledtf">* Enabled:</label></nobr>
        </td>
        <td width="99%">
            <table cellpadding="0" cellspacing="0" border="0">
                <tr>
                    <td><input type="radio" name="enabled" id="enabledtf" value="true" <%=enabled ? "checked" : ""%> /></td>
                    <td style="padding-right : 10px;">Yes</td>
                    <td><input type="radio" name="enabled" value="false" <%=!enabled ? "checked" : ""%> /></td>
                    <td>No</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td width="1%">
            <nobr><label for="servertf">* Server:</label></nobr>
        </td>
        <td width="99%">
            <input type="text" name="server" id="servertf" size="30" maxlength="100" value="<%=server != null ? server : ""%>"/>
            <% if (errors.containsKey("server")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("server")%></span>
            <% } %>
        </td>
    </tr>
    <tr>
        <td width="1%">
            <nobr><label for="porttf">Port:</label></nobr>
        </td>
        <td width="99%">
            <input type="text" name="port" id="porttf" size="30" maxlength="100"
                   value="<%= port != -1 ? String.valueOf(port) : "" %>" />
            <% if (errors.containsKey("port")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("port")%></span>
            <% } %>
        </td>
    </tr>
    <tr>
        <td width="1%">
            <nobr><label for="usernametf">* Username:</label></nobr>
        </td>
        <td width="99%">
            <input type="text" name="username" size="30" maxlength="100" value="<%=username != null ? username : ""%>"
                   id="usernametf"/>
            <% if (errors.containsKey("username")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("username")%></span>
            <% } %>
        </td>
    </tr>
    <tr>
        <td width="1%">
            <nobr>* <label for="passwordtf">Password:</label></nobr>
        </td>
        <td width="99%">
            <input type="password" name="password" size="30" maxlength="100" value="<%=password%>" id="passwordtf"/>
            <% if (errors.containsKey("password")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("password")%></span>
            <% } %>
        </td>
    </tr>
    <tr>
        <td width="1%">
            <nobr><label for="contexttf">Asterisk Context:</label></nobr>
        </td>
        <td width="99%">
            <input type="text" name="context" size="30" maxlength="100" value="<%= context != null ? context : "" %>"
                   id="contexttf"/>
            <% if (errors.containsKey("context")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("context")%></span>
            <% } %>
        </td>
    </tr>
    <tr>
        <td width="1%">
            <nobr><label for="callerIDtf">Default Caller ID:</label></nobr>
        </td>
        <td width="99%">
            <input type="text" name="callerID" size="30" maxlength="100" value="<%= callerID != null ? callerID : "" %>"
                   id="callerIDtf"/>
            <% if (errors.containsKey("callerID")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("callerID")%></span>
            <% } %>
        </td>
    </tr>
    <tr>
        <td width="1%">
            <nobr><label for="dialVariablestf">Dial Command Variables:</label></nobr>
        </td>
        <td width="99%">
            <input type="text" name="dialVariables" size="30" maxlength="100" value="<%= dialVariables != null ? dialVariables : "" %>"
                   id="dialVariablestf"/>
            <% if (errors.containsKey("dialVariables")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("dialVariables")%></span>
            <% } %>
        </td>
    </tr>
</tbody>
</table>

<br>
<span class="jive-description">
        * Required fields
        </span>
</div>

</fieldset>
<br/>

<input type="submit" name="save" value="Save"/>

</div>

</body>
</html>



<%!
    boolean isParamPresent(HttpServletRequest request, String param) {
        String value = request.getParameter(param);
        if (value == null) {
            return false;
        }
        return !"".equals(value);
    }

%>
