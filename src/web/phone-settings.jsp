<%@ page import="org.jivesoftware.phone.PhoneManager,
				 org.jivesoftware.phone.PhonePlugin,
				 org.jivesoftware.phone.PhoneOption,
 				 org.jivesoftware.phone.PhoneProperties,
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

	// if we were not enabled before and we are now restart the plugin
    PluginManager pluginManager = XMPPServer.getInstance().getPluginManager();
    PhonePlugin plugin = (PhonePlugin) pluginManager.getPlugin("asterisk-im");
    if (plugin==null) {
	    // Complain about not being able to get the plugin
    	String msg = "Unable to acquire asterisk plugin instance!";
	    Log.error(msg);
        throw new IllegalStateException(msg);
    }
    
    boolean enabled = ParamUtils.getBooleanParameter(request, "enabled", false);
    boolean isSave = request.getParameter("save") != null;
	HttpServletRequest req = null;
    boolean success = request.getParameter("success") != null;

    HashMap<String, String> errors = new HashMap<String, String>();

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
            JiveGlobals.setProperty(PhoneProperties.ENABLED, String.valueOf(enabled));


            for (int i=0; i<options.length; i++) {
    			String prop = options[i].getPropertyName();
    			String param = options[i].getParamName();
				String val = request.getParameter(param);
	            if (options[i].isPassword() && "passwordtf".equals(val)) {
	            	continue;
        	    }
   	            JiveGlobals.setProperty(prop, val);
			}    			
			plugin.restart();
			// FIXME: maybe get some results of the restart here? ;jw
			response.sendRedirect("phone-settings.jsp?success=true");
            return;
    	}
    } else {
        enabled = JiveGlobals.getBooleanProperty(PhoneProperties.ENABLED, false);
    }

    //try to establish a connection, if there is an error we log it below
    boolean isConnected = false;

    if (enabled) {
        PhoneManager phoneManager = plugin.getPhoneManager();
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
    Use the form below to edit Phone integration settings.
    Changing settings will cause the plugin to be reloaded.<br>
</p>


<% if (success) { %>

<div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
        <tbody>
            <tr>
                <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
                <td class="jive-icon-label">Service settings updated successfully. It will take a couple seconds to
                    reconnect to the telephony interface</td>
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
</div><br/>

<% } else if (!isConnected) { %>

<p>
    <div class="jive-error">
        <table cellpadding="0" cellspacing="0" border="0">
            <tbody>
                <tr>
                    <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0"></td>
                    <td class="jive-icon-label">Unable to establish a connection to the telephony server, please see error
                        log</td>
                </tr>
            </tbody>
        </table>
    </div><br/>

</p>

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
                    <td><input type="radio" name="enabled" id="enabledtf" value="true" <%= enabled ? "checked" : ""%> /></td>
                    <td style="padding-right : 10px;">Yes</td>
                    <td><input type="radio" name="enabled" value="false" <%=!enabled ? "checked" : ""%> /></td>
                    <td>No</td>
                </tr>
            </table>
        </td>
    </tr>
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
            <table cellpadding="0" cellspacing="0" border="0">
                <tr>
                    <td><input type="radio" name="<%= pn %>" id="<%= pn %>" value="true" <%= "true".equals(val) ? "checked" : "" %> /></td>
                    <td style="padding-right : 10px;">Yes</td>
                    <td><input type="radio" name="<%= pn %>" value="false" <%= !"true".equals(val) ? "checked" : ""%> /></td>
                    <td>No</td>
                </tr>
            </table>
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
