<%@ page import="org.jivesoftware.openfire.XMPPServer" %>
<%@ page import="org.jivesoftware.openfire.container.PluginManager" %>
<%@ page import="org.jivesoftware.phone.PhoneManager" %>
<%@ page import="org.jivesoftware.phone.PhoneOption" %>
<%@ page import="org.jivesoftware.phone.PhonePlugin" %>
<%@ page import="org.jivesoftware.phone.PhoneServer" %>
<%@ page import="org.jivesoftware.util.JiveGlobals" %>
<%@ page import="org.jivesoftware.util.Log" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="javax.servlet.http.HttpServletRequest" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>
<%
    PluginManager pluginManager = XMPPServer.getInstance().getPluginManager();
    PhonePlugin plugin = (PhonePlugin) pluginManager.getPlugin("asterisk-im");
    if (plugin == null) {
        // Complain about not being able to get the plugin
        String msg = "Unable to acquire asterisk plugin instance!";
        Log.error(msg);
        throw new IllegalStateException(msg);
    }
    Map<String, String> errors = new HashMap<String, String>();
    boolean editServer = request.getParameter("serverID") != null;
    int editServerID = ParamUtils.getIntParameter(request, "serverID", -1);
    boolean saved = request.getParameter("createServer") != null;
%>
<head>
    <title><%=editServer ? "Edit Phone Server" : "Create Phone Server"%></title>
    <meta name="pageID" content="item-phone-settings"/>
    <link rel="stylesheet" type="text/css" href="/style/global.css">
    <style type="text/css">
        .div-border {
            border: 1px;
            border-color: #ccc;
            border-style: dotted;
        }
    </style>
</head>
<%
    String serverName = "";
    String serverAddress = "";
    String username = "";
    int serverPort = 0;
    String password = "";
    if (saved) {
        serverName = ParamUtils.getParameter(request, "serverName", false);
        serverAddress = ParamUtils.getParameter(request, "serverAddress", false);
        serverPort = ParamUtils.getIntParameter(request, "serverPort",
                plugin.getServerConfiguration().getDefaultPort());
        username = ParamUtils.getParameter(request, "username", false);
        password = ParamUtils.getParameter(request, "password", true);
    }
    else if(editServer && !saved) {
        PhoneServer server = plugin.getPhoneManager().getPhoneServerByID(editServerID);
        serverName = server.getName();
        serverAddress = server.getHostname();
        serverPort = server.getPort();
        username = server.getUsername();
        password = server.getPassword();
    }

    if (saved && (serverName != null || serverAddress != null || username != null)) {
        if (serverName == null) {
            errors.put("serverName", "Server name must be specified.");
        }
        if (serverAddress == null) {
            errors.put("serverAddress", "Server address must be specified");
        }
        if (username == null) {
            errors.put("username", "Username must be specified");
        }
        if (serverPort <= 0 || serverPort > 65535) {
            errors.put("serverPort", "Invalid port number.");
        }

        if (errors.size() <= 0) {
            if (!editServer) {
                PhoneServer server = plugin.getPhoneManager().createPhoneServer(serverName,
                        serverAddress, serverPort, username, password);
                response.sendRedirect("phone-settings.jsp?serverCreated=" + server.getID());
            }
            else {
                PhoneServer server = plugin.getPhoneManager().updatePhoneServer(editServerID,
                        serverName, serverAddress, serverPort, username, password);
                response.sendRedirect("phone-settings.jsp?serverEdited=" + server.getID());
            }
        }
    }
%>
<body>
<p>
    Add a connection to a new phone server.
</p>

<form action="create-server.jsp" method="post">
    <table class="div-border" cellpadding="3">
        <tr valign="top">
            <td><b>Server Name:</b></td>
            <td>
                <input type="text" name="serverName" size="30" value="<%=serverName != null
                ? serverName : ""%>"/>
                <% if (errors.get("serverName") != null) { %>
                <br/><span class="jive-error-text"><%= errors.get("serverName")%></span><br/>
                <% } %>
            </td>
        </tr>
        <tr valign="top">
            <td><b>Server Address:</b></td>
            <td>
                <input type="text" name="serverAddress" size="30" value="<%=serverAddress != null
                ? serverAddress : ""%>"/>
                <% if (errors.get("serverAddress") != null) { %>
                <br/><span class="jive-error-text"><%= errors.get("serverAddress")%></span><br/>
                <% } %>
            </td>
        </tr>
        <tr valign="top">
            <td><b>Port:</b></td>
            <td>
                <input type="text" name="serverPort" size="30" value="<%=serverPort%>"/>
                <% if (errors.get("serverPort") != null) { %>
                <br/><span class="jive-error-text"><%= errors.get("serverPort")%></span><br/>
                <% } %>
            </td>
        </tr>
        <tr valign="top">
            <td><b>Username:</b></td>
            <td>
                <input type="text" name="username" size="30" value="<%=username != null
                ? username : ""%>"/>
                <% if (errors.get("username") != null) { %>
                <br/><span class="jive-error-text"><%= errors.get("username")%></span><br/>
                <% } %>
            </td>
        </tr>
        <tr valign="top">
            <td><b>Password:</b></td>
            <td>
                <input type="password" name="password" size="30" value="<%=password != null
                ? password : ""%>"/>
                <% if (errors.get("password") != null) { %>
                <br/><span class="jive-error-text"><%= errors.get("password")%></span><br/>
                <% } %>
            </td>
        </tr>
        <tr>
            <td></td>
            <td><input type="submit" name="createServer"
                       value="<%= editServer ? "Edit Server" : "Create Server"  %>"/>&nbsp;
                <input type="button" value="Cancel"
                       onclick="window.location.href='phone-settings.jsp'; return false;">
            </td>
        </tr>
    </table>
    <% if (editServer) { %>
    <input type="hidden" name="serverID" value="<%=editServerID%>"/>
    <% } %>
</form>
</body>