<%@ page import="org.jivesoftware.admin.AdminPageBean,
                 org.jivesoftware.phone.asterisk.AsteriskPlugin,
                 org.jivesoftware.messenger.XMPPServer,
                 java.util.HashMap,
                 org.jivesoftware.phone.asterisk.AsteriskPlugin,
                 org.jivesoftware.phone.database.HibernateUtil,
                 org.jivesoftware.messenger.container.PluginManager,
                 net.sf.asterisk.manager.ManagerConnection,
                 org.jivesoftware.phone.asterisk.ManagerConnectionPoolFactory,
                 org.jivesoftware.phone.asterisk.ManagerException,
                 org.jivesoftware.util.*"%>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="java.util.logging.Level"%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<jsp:useBean id="admin" class="org.jivesoftware.util.WebManager"  />
<c:set var="admin" value="${admin.manager}" />
<% admin.init(request, response, session, application, out ); %>

<%

    boolean isSave = request.getParameter("save") != null;
    boolean success = request.getParameter("success") != null;
    boolean usersDisabled = request.getParameter("usersDisabled") != null;

    String server = request.getParameter("server");
    int port = ParamUtils.getIntParameter(request, "port", -1);
    String username = request.getParameter("username");
    String password = request.getParameter("password");
    int poolSize = ParamUtils.getIntParameter(request, "poolSize", -1);
    boolean enabled = ParamUtils.getBooleanParameter(request, "enabled", false);
    String callerID = request.getParameter("callerID");
    String context = request.getParameter("context");

    HashMap<String,String> errors = new HashMap<String,String>();

    if(isSave) {

        if(server == null || "".equals(server)) {
            errors.put("server", "Server is required");
        }

        if(username == null || "".equals(username)) {
            errors.put("username", "Username is required");
        }

        if(password == null || "".equals(password)) {
            errors.put("password", "Password is required");
        }

        // If a port was specified make sure that it is valid
        if(isParamPresent(request, "port") && port < 1025) {
            errors.put("port", "Port must be greater than 1024 and not in use");
        }

        // If a poolSize was specified make sure it is valid
        if(isParamPresent(request, "poolSize") && poolSize < 2) {
            errors.put("poolSize", "Pool Size must be greater than 1");
        }


        // If there are no errors initialize the manager
        if(errors.size() == 0) {

            JiveGlobals.setProperty(AsteriskPlugin.Properties.SERVER, server);
            JiveGlobals.setProperty(AsteriskPlugin.Properties.USERNAME, username);
            JiveGlobals.setProperty(AsteriskPlugin.Properties.PASSWORD, password);
            JiveGlobals.setProperty(AsteriskPlugin.Properties.DEFAULT_CALLER_ID, callerID);
            JiveGlobals.setProperty(AsteriskPlugin.Properties.CONTEXT, context);

            JiveGlobals.setProperty(AsteriskPlugin.Properties.ENABLED, String.valueOf(enabled));

            if(poolSize > 1) {
                JiveGlobals.setProperty(AsteriskPlugin.Properties.POOLSIZE, String.valueOf(poolSize));
            }

            if(port > 1024 ) {
                JiveGlobals.setProperty(AsteriskPlugin.Properties.PORT, String.valueOf(port));
            }

            try {
                 // if we were not enabled before and we are now restart the plugin
                PluginManager pluginManager = XMPPServer.getInstance().getPluginManager();
                AsteriskPlugin plugin = (AsteriskPlugin) pluginManager.getPlugin(AsteriskPlugin.NAME);

                plugin.destroy();
                Thread.sleep(1 * JiveConstants.SECOND);

                plugin.init();
                Thread.sleep(1 * JiveConstants.SECOND);

            } catch (Exception e) {
                Log.error(e);
            }

            response.sendRedirect("phone-settings.jsp?success=true");
            return;

        }

    }


    // See what the values are
    server = JiveGlobals.getProperty(AsteriskPlugin.Properties.SERVER);
    port = JiveGlobals.getIntProperty(AsteriskPlugin.Properties.PORT, -1);
    username = JiveGlobals.getProperty(AsteriskPlugin.Properties.USERNAME);
    password = JiveGlobals.getProperty(AsteriskPlugin.Properties.PASSWORD);
    poolSize = JiveGlobals.getIntProperty(AsteriskPlugin.Properties.POOLSIZE, -1);
    enabled = JiveGlobals.getBooleanProperty(AsteriskPlugin.Properties.ENABLED, false);
    callerID = JiveGlobals.getProperty(AsteriskPlugin.Properties.DEFAULT_CALLER_ID);
    context = JiveGlobals.getProperty(AsteriskPlugin.Properties.CONTEXT);


    //try to establish a connection, if there is an error we log it below
    ManagerConnection conn = null;
    boolean isConnected = true;

    if(enabled) {
        try {
            conn = ManagerConnectionPoolFactory.getManagerConnectionPool().getConnection();

        }
        catch (Exception e) {
            isConnected = false;
            Log.error("unable to get a manager connection : "+e.getMessage(), e);
        }
        finally {
            if(conn != null) {
                conn.logoff();
            }
        }
    }


%>

<jsp:useBean id="pageinfo" scope="request" class="org.jivesoftware.admin.AdminPageBean" />
<%
    String title = "General Settings";
    pageinfo.setTitle(title);
    pageinfo.getBreadcrumbs().add(new AdminPageBean.Breadcrumb(title, "phone-settings.jsp"));
    pageinfo.setPageID("item-phone-settings");
%>

<jsp:include page="top.jsp" flush="true" />
<jsp:include page="title.jsp" flush="true" />

<div id="phone-settings">

<p>
Use the form below to edit Asterisk integration settings.
Changing settings will cause the plugin to be reloaded.<br>
</p>


<% if (!HibernateUtil.tablesExist()) { %>

<p>

<div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
        	<td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0"></td>
        	<td class="jive-icon-label">The Asterisk plugin was not able to succesfully initialize the database.
                Please see the documentation on initializing the database manually.</td>
        </tr>
    </tbody>
    </table>
    </div><br>

</p>

<%  } %>



<% if (success) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr>
	        <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
	        <td class="jive-icon-label">Service settings updated successfully. It will take a couple seconds to reconnect to the asterisk manager</td>
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
            	<td class="jive-icon-label">Unable to establish a connection to the manager server, please see error log</td>
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
                <td witdh="1%">
                    <nobr><label for="enabledtf">* Enabled:</label>
                </td>
                <td witdh="99%">
                    <table cellpadding="0" cellspacing="0" border="0">
                        <tr>
                            <td><input type="radio" name="enabled" value="true" <%=enabled ? "checked" : ""%> /></td>
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
                <td witdh="99%">
                    <input type="text" name="server" size="30" maxlength="100" value="<%=server != null ? server : ""%>" id="servertf" />
                    <% if (errors.containsKey("server")) { %>
                        <br />
                        <span class="jive-error-text"><%=errors.get("server")%></span>
                    <% } %>
                </td>
            </tr>
            <tr>
                <td width="1%">
                    <nobr><label for="porttf">Port:</label></nobr>
                </td>
                <td witdh="99%">
                    <input type="text" name="port" size="30" maxlength="100" value="<%= port != -1 ? String.valueOf(port) : "" %>" id="porttf" />
                    <% if (errors.containsKey("port")) { %>
                        <br />
                        <span class="jive-error-text"><%=errors.get("port")%></span>
                    <% } %>
                </td>
            </tr>
            <tr>
                <td width="1%">
                    <nobr><label for="usernametf">* Username:</label></nobr>
                </td>
                <td witdh="99%">
                    <input type="text" name="username" size="30" maxlength="100" value="<%=username != null ? username : ""%>" id="usernametf" />
                    <% if (errors.containsKey("username")) { %>
                        <br />
                        <span class="jive-error-text"><%=errors.get("username")%></span>
                    <% } %>
                </td>
            </tr>
            <tr>
                <td width="1%">
                    <nobr><label for="passwordtf">Password:</label> *</nobr>
                </td>
                <td witdh="99%">
                    <input type="password" name="password" size="30" maxlength="100" value="<%=password != null ? password : ""%>" id="passwordtf" />
                    <% if (errors.containsKey("password")) { %>
                        <br />
                        <span class="jive-error-text"><%=errors.get("password")%></span>
                    <% } %>
                </td>
            </tr>
            <tr>
                <td width="1%">
                    <nobr><label for="poolSizetf">Pool Size:</label></nobr>
                </td>
                <td witdh="99%">
                    <input type="text" name="poolSize" size="30" maxlength="100" value="<%= poolSize != -1 ? String.valueOf(poolSize) : "" %>" id="poolSizetf" />
                    <% if (errors.containsKey("poolSize")) { %>
                        <br />
                        <span class="jive-error-text"><%=errors.get("poolSize")%></span>
                    <% } %>
                </td>
            </tr>
            <tr>
                <td width="1%">
                    <nobr><label for="contexttf">Asterisk Context:</label></nobr>
                </td>
                <td witdh="99%">
                    <input type="text" name="context" size="30" maxlength="100" value="<%= context != null ? context : "" %>" id="contexttf" />
                    <% if (errors.containsKey("context")) { %>
                        <br />
                        <span class="jive-error-text"><%=errors.get("context")%></span>
                    <% } %>
                </td>
            </tr>
            <tr>
                <td width="1%">
                    <nobr><label for="callerIDtf">Default Caller ID:</label></nobr>
                </td>
                <td witdh="99%">
                    <input type="text" name="callerID" size="30" maxlength="100" value="<%= callerID != null ? callerID : "" %>" id="callerIDtf" />
                    <% if (errors.containsKey("callerID")) { %>
                        <br />
                        <span class="jive-error-text"><%=errors.get("callerID")%></span>
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
<br />

<input type="submit" name="save" value="Save"/>

</div>

<jsp:include page="bottom.jsp" flush="true" />


<%!
    boolean isParamPresent(HttpServletRequest request, String param) {
        String value = request.getParameter(param);
        if(value == null) { return false; }
        if("".equals(value)) { return false; }
        return true;
    }

%>
