<%@ page import="org.jivesoftware.util.JiveGlobals,
                 org.jivesoftware.util.Log,
                 org.jivesoftware.util.ParamUtils,
                 org.jivesoftware.wildfire.XMPPServer,
                 org.jivesoftware.wildfire.container.PluginManager,
                 javax.servlet.http.HttpServletRequest,
                 java.util.HashMap" %>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.Collection"%>
<%@ page import="org.jivesoftware.phone.*"%>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>

<%

	// if we were not enabled before and we are now restart the plugin
    PluginManager pluginManager = XMPPServer.getInstance().getPluginManager();
    PhonePlugin plugin = (PhonePlugin) pluginManager.getPlugin("asterisk-im");
    if (plugin == null) {
	    // Complain about not being able to get the plugin
    	String msg = "Unable to acquire asterisk plugin instance!";
	    Log.error(msg);
        throw new IllegalStateException(msg);
    }

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
    	PhoneOption[] options = plugin.getOptions();
    	
    	// check parameters
    	for (int i=0; i<options.length; i++) {
    		String param = options[i].getParamName();
    	    String err = 
    	    	options[i].check(request.getParameter(param));
    	   	if (err!=null) {
    	   		errors.put(param, err);
    	   	}
    	}


        // If there are no errors initialize the manager
        if (errors.size() == 0) {

            for (int i=0; i<options.length; i++) {
    			String prop = options[i].getPropertyName();
    			String param = options[i].getParamName();
				String val = request.getParameter(param);
	            if (options[i].isPassword() && "passwordtf".equals(val)) {
	            	continue;
        	    }
   	            JiveGlobals.setProperty(prop, val);
			}

            if(booleanParameter != null) {
                plugin.setEnabled(enabled);
            }
    	}
    }
    try {
        enabled = plugin.isEnabled();
    }
    catch (Exception e) {
        /** Here we will draw up that there was an error loading the plugin **/
    }

%>

<html>
<head>
    <title>General Settings</title>
    <meta name="pageID" content="item-phone-settings"/>

  <style type="text/css">
      #enabledtf {
      }

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
  </style>
</head>
<body>

<div id="phone-settings">

<p>
    Use the form below to edit Phone integration settings.
    Changing settings will cause the plugin to be reloaded.<br>
</p>


<% if (success) { %>
<div class="success" style="width: 400;">
    Service settings updated successfully. It will take a couple seconds to reconnect to the
    telephony interface
</div>
<%  } else if (isDelete) { %>
<div class="success" style="width: 400;">
    Server deleted successfully
</div>
<%  } else if (isCreate) { %>
<div class="success" style="width: 400;">
    Server created successfully
</div>
<%  } else if (isEdit) { %>
<div class="success" style="width: 400;">
    Server edited successfully
</div>
<%  } else if (errors.size() > 0) { %>

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
<div class="div-border" style="background-color : #EAF1F8; width: 200px; padding: 4px; ">
    <span style="font-weight:bold;">Asterisk-IM:</span>
        <span><input type="radio" name="enabled" value="true" <%= enabled ? "checked" : ""%> />ON
        </span>
        <span><input type="radio" name="enabled" value="false" <%=!enabled ? "checked" : ""%> />OFF
        </span>
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
        <%  boolean hasServers = false;
            PhoneManager manager = plugin.getPhoneManager();
            if(enabled) {
            Collection<PhoneServer> servers = manager.getPhoneServers();
            for(PhoneServer phoneServer : servers) {
                hasServers = true;
        %>
        <tr style="border-left: none;">
            <td width="16px">
                <% switch(manager.getPhoneServerStatus(phoneServer.getID())) {
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
                    <img src="/images/edit-16x16.gif" border="0" alt="Edit Server"/></a>
                <a href="delete-server.jsp?serverID=<%=phoneServer.getID()%>">
                    <img src="/images/delete-16x16.gif" border="0" alt="Delete Server"/></a>
            </td>
        </tr>
        <%  }
       } %>
        <% if(enabled && !hasServers) { %>
        <tr>
                <td colspan="6" align="center">No Servers Configured</td>
        </tr>
        <% } else if(!enabled) { %>
        <tr>
                <td colspan="6" align="center">Asterisk IM Not Enabled</td>
        </tr>
        <% }%>
        <% if(enabled) { %>
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

	PhoneOption[] options = plugin.getOptions();
	for (int i=0; i<options.length; i++) {
		PhoneOption opt = options[i];
		String pn = opt.getParamName();
		String txt = opt.getDescription();
		String cn = opt.getPropertyName();
		String type = "text";
		// FIXME: we need html escaping for the value we put in! ;jw
		String val = getParameter(req, pn, cn);
		if (opt.isPassword()) {
			type = "password";
			val = "passwordtf";
		}
		if (opt.getType() == PhoneOption.TEXTBOX) {
%>
    <tr>
        <td width="1%">
            <nobr><label for="<%= pn %>">
            	<%= opt.isRequired() ? "* " : "" %>
            	<%= txt %>:
            </label></nobr>
        </td>
        <td width="99%">
        	<textarea cols="45" rows="5" name="<%= pn  %>" wrap="virtual" id=<%= pn %>><%= val %></textarea>
            <% if (errors.containsKey(pn)) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get(pn)%></span>
            <% } %>
        </td>
    </tr>
<%		
			
		} else if (opt.getType() == PhoneOption.FLAG) {
%>
    <tr>
        <td width="1%">
            <nobr><label for="<%= pn %>">
            	<%= opt.isRequired() ? "* " : "" %>
            	<%= txt %>:
			</label></nobr>
        </td>
        <td width="99%">
            <span><input type="radio" name="<%= pn %>" id="<%= pn %>"
                         value="true" <%= "true".equals(val) ? "checked" : "" %> />
                Yes</span>
            <span><input type="radio" name="<%= pn %>"
                         value="false" <%= !"true".equals(val) ? "checked" : ""%> />
                No</span>
        </td>
    </tr>
<%
		} else {
%>
    <tr>
        <td width="1%">
            <nobr><label for="<%= pn %>">
            	<%= opt.isRequired() ? "* " : "" %>
            	<%= txt %>:
            </label></nobr>
        </td>
        <td width="99%">
            <input type="<%= type %>" size="30" maxlength="100" name="<%= pn %>" value="<%= val %>"
                   id="<%= pn %>"/>
            <% if (errors.containsKey(pn)) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get(pn)%></span>
            <% } %>
        </td>
    </tr>
<% 
		}

	} 
%>
<tr>
    <td colspan="2">
        <span class="jive-description">* Required fields</span>
    </td>
</tr>
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
		String s = null;
		if (q!=null) {
			s = q.getParameter(pn);
		} else {
			s = JiveGlobals.getProperty(cn);
		}
		if (s==null) {
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
