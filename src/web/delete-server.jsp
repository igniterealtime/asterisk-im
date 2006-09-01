<%--
  -	$Revision: 3195 $
  -	$Date: 2005-12-13 10:07:30 -0800 (Tue, 13 Dec 2005) $
  -
  - Copyright (C) 2004-2005 Jive Software. All rights reserved.
  -
  - This software is published under the terms of the GNU Public License (GPL),
  - a copy of which is included in this distribution.
--%>

<%@ page import="org.jivesoftware.phone.PhoneManager,
                 org.jivesoftware.phone.PhonePlugin"
%>
<%@ page import="org.jivesoftware.phone.PhoneServer"%>
<%@ page import="org.jivesoftware.util.Log"%>
<%@ page import="org.jivesoftware.util.ParamUtils"%>
<%@ page import="org.jivesoftware.wildfire.XMPPServer"%>
<%@ page import="org.jivesoftware.wildfire.container.PluginManager"%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%
    PluginManager pluginManager = XMPPServer.getInstance().getPluginManager();
    PhonePlugin plugin = (PhonePlugin) pluginManager.getPlugin("asterisk-im");
    if (plugin == null) {
	    // Complain about not being able to get the plugin
    	String msg = "Unable to acquire asterisk plugin instance!";
	    Log.error(msg);
        throw new IllegalStateException(msg);
    }

    // Get parameters //
    boolean cancel = request.getParameter("cancel") != null;
    boolean delete = request.getParameter("delete") != null;
    String serverIDString = ParamUtils.getParameter(request, "serverID");

    // Handle a cancel
    if (serverIDString == null || !plugin.isEnabled() || cancel) {
        response.sendRedirect("phone-settings.jsp");
        return;
    }

    PhoneManager manager = plugin.getPhoneManager();
    long serverID = Long.valueOf(serverIDString);
    int devices = manager.getPhoneDevicesByServerID(serverID).size();

    // Load the user object
    PhoneServer server = manager.getPhoneServerByID(serverID);

    // Handle a server delete:
    if (delete) {
        manager.removePhoneServer(server.getID());
        // Done, so redirect
        response.sendRedirect("phone-settings.jsp?deleteSuccess=true");
        return;
    }
%>

<html>
    <head>
        <title>Delete Server</title>
        <meta name="pageID" content="item-phone-settings"/>
    </head>
    <body>

<p>
    Are you sure you want to remove the server, <%=server.getName()%>, deleting this server will
    delete <%=devices%> phone(s)?
</p>

<form action="delete-server.jsp">
<input type="hidden" name="serverID" value="<%= server.getID() %>">
<input type="submit" name="delete" value="Delete">
<input type="submit" name="cancel" value="Cancel">
</form>

    </body>
</html>
