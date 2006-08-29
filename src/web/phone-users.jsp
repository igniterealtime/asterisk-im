<%@ page import="org.jivesoftware.phone.*,
                 org.jivesoftware.util.JiveGlobals,
                 org.jivesoftware.util.LocaleUtils,
                 org.jivesoftware.util.Log,
                 org.jivesoftware.util.ParamUtils,
                 org.jivesoftware.wildfire.XMPPServer,
                 org.jivesoftware.wildfire.user.UserManager,
                 org.jivesoftware.wildfire.user.UserNotFoundException,
                 org.jivesoftware.wildfire.container.PluginManager,
                 java.util.Collection,
                 java.util.HashMap" %>
<%@ page import="java.util.List" %>

<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt_rt" prefix="fmt" %>


<%!
    final int DEFAULT_RANGE = 15;
    final int[] RANGE_PRESETS = {15, 25, 50, 75, 100};
    final String USER_RANGE_PROP = "admin.userlist.range";
%>

<%
    PluginManager pluginManager = XMPPServer.getInstance().getPluginManager();
    PhonePlugin plugin = (PhonePlugin) pluginManager.getPlugin("asterisk-im");
    if (plugin == null) {
	    // Complain about not being able to get the plugin
    	String msg = "Unable to acquire asterisk plugin instance!";
	    Log.error(msg);
        throw new IllegalStateException(msg);
    }

    // For cancel we will just forward before doing anything
    if (request.getParameter("cancel") != null) {
        response.sendRedirect("phone-users.jsp");
        return;
    }

    if (!plugin.isEnabled()) {
        response.sendRedirect("phone-settings.jsp?usersDisabled=true");
        return;
    }

    boolean delete = request.getParameter("delete") != null;
    boolean save = request.getParameter("save") != null;
    boolean success = request.getParameter("success") != null;

    // Paginator props
    int start = ParamUtils.getIntParameter(request, "start", 0);
    int range = ParamUtils.getIntParameter(request, "range", DEFAULT_RANGE);

    long deviceID = ParamUtils.getLongParameter(request, "deviceID", -1);
    long userID = ParamUtils.getLongParameter(request, "userID", -1);
    String username = request.getParameter("username");
    String device = request.getParameter("devicetf");
    if (device == null || "".equals(device)) {
        device = request.getParameter("device");
    }
    String callerID = request.getParameter("callerID");
    String extension = request.getParameter("extension");
    boolean isPrimary = request.getParameter("primary") != null;

    HashMap<String, String> errors = new HashMap<String, String>();

    PhoneManager phoneManager;
    try {
        phoneManager = plugin.getPhoneManager();

        PhoneUser phoneUser = null;
        List<PhoneDevice> devices = null;
        PhoneDevice phoneDevice = null;

        // find the phoneUser if one was passed in
        if (userID > 0) {
            phoneUser = phoneManager.getPhoneUserByID(userID);

            // find the phone device if one was passed in
            if (phoneUser != null && deviceID > 0) {
                devices = phoneManager.getPhoneDevicesByUserID(userID);

                phoneDevice = phoneManager.getPhoneDeviceByID(deviceID);
            }


            if (phoneUser == null) {
                Log.error("User with id " + userID + " was not found!");
            }

        }
        else if (username != null) {
            devices = phoneManager.getPhoneDevicesByUsername(username);
        }

        if (delete) {
            if (deviceID > -1) {
                try {

                    if (devices.size() > 1) {
                        phoneManager.removePhoneServer(phoneDevice);


                    }
                    else {
                        // only one removePhoneServer the whole mapping
                        phoneManager.removePhoneServer(phoneUser);
                    }
                }
                catch (Exception e) {
                    Log.error("error attempting to delete device id " + deviceID
                            + " belonging to phoneUser id " + userID, e);
                }
                response.sendRedirect("phone-users.jsp?success=true&start=" + start
                        + "&range=" + range);
                return;
            }
        }
        else if (save) {
            // Save with no copy is add mode

            if (phoneUser == null) {
                if (username == null || "".equals(username)) {
                    Log.debug("Username is required!!");
                    errors.put("username", "Username is required");
                }
                else {
                    UserManager userManager = XMPPServer.getInstance().getUserManager();
                    try {
                        userManager.getUser(username);
                    }
                    catch (UserNotFoundException e) {
                        Log.debug(e);
                        errors.put("username", "User does not exist");
                    }

                }
            }

            if (device == null || "".equals(device)) {
                Log.debug("Phone is required!");
                errors.put("device", "Phone is required");
            }
            // if we are adding a new device make sure this name is unique
            else if (phoneDevice == null) {

                if (phoneManager.getPhoneUserByDevice(device) != null) {
                    Log.debug("Phone must be unique!");
                    errors.put("device", "Phone must be unique");
                }

            }
            // Make sure it is unique if we are updating, ignore our own username
            else if (phoneDevice != null) {

                if (phoneManager.getPhoneUserByDevice(device) != null
                        && !phoneDevice.getDevice().equals(device)) {
                    Log.debug("Phone must be unique!");
                    errors.put("device", "Phone must be unique");
                }


            }

            if (extension == null || "".equals(extension)) {
                Log.debug("Extension is required!");
                errors.put("extension", "Extension is required");
            }

            // See if the username already, exists
            if (phoneUser == null) {
                phoneUser = phoneManager.getPhoneUserByUsername(username);
            }

            if (errors.size() == 0) {

                try {

                    if (phoneUser == null) {
                        Log.debug("username does not exist, creating new username");
                        phoneUser = new PhoneUser(username);
                    }


                    if (phoneDevice == null) {
                        // This is a new device
                        phoneDevice = new PhoneDevice();
                    }
                    phoneDevice.setDevice(device);

                    if ("".equals(callerID)) {
                        callerID = null;
                    }
                    phoneDevice.setCallerId(callerID);
                    phoneDevice.setExtension(extension);

                    // Make all the other users not the primary
                    if (isPrimary && devices != null) {

                        for (PhoneDevice currentDevice : devices) {

                            if (currentDevice.getID() != phoneDevice.getID()) {
                                currentDevice.setPrimary(false);
                                phoneManager.update(currentDevice);
                            }
                        }

                        phoneDevice.setPrimary(true);
                    }
                    // If there are no others,set the default
                    else if (!containsPrimary(devices, phoneDevice)) {
                        phoneDevice.setPrimary(true);
                    }
                    // Other wise just set the value
                    else {
                        phoneDevice.setPrimary(isPrimary);
                    }

                    if (phoneUser.getID() < 1) {
                        phoneManager.insert(phoneUser);
                    }

                    phoneDevice.setPhoneUserID(phoneUser.getID());
                    if (phoneDevice.getID() < 1) {
                        phoneManager.insert(phoneDevice);
                    }
                    else {
                        phoneManager.update(phoneDevice);
                    }

                }
                catch (Exception e) {
                    Log.error(e);
                }
                response.sendRedirect("phone-users.jsp?success=true&start="
                        + start + "&range=" + range);
                return;
            }
        }

        // Populate values for the form below
        if (phoneDevice != null) {

            callerID = phoneDevice.getCallerId();
            username = phoneUser.getUsername();
            device = phoneDevice.getDevice();
            extension = phoneDevice.getExtension();
            isPrimary = phoneDevice.isPrimary();

        }

        // Get the list of users for the correct page
        List<PhoneUser> allusers = phoneManager.getAllPhoneUsers();
        int userCount = allusers.size();
        int endpoint = start + range;

        // the sublist, either the limit or how many is left
        List<PhoneUser> users = allusers.subList(start, endpoint < userCount ? endpoint : userCount);

        // If there were no users on this page (this could happen with a delete)
        // go to the page before it
        if (start > 0 && users.size() < 1) {

            start = 0;
            endpoint = start + range;
            users = allusers.subList(start, endpoint < userCount ? endpoint : userCount);
        }

        // paginator vars
        int numPages = (int) Math.ceil((double) userCount / (double) range);
        int curPage = (start / range) + 1;

        // wheteher or not to use sip device drop down
       	boolean useSipDropDown = JiveGlobals
                   .getBooleanProperty(PhoneProperties.DEVICE_DROP_DOWN, true);


        Collection<String> sipDevices = null;
        if (useSipDropDown) {
            try {
                sipDevices = phoneManager.getDevices(1);
            }
            catch (PhoneException e) {
                Log.error(e);
            }
        }

        Collection<PhoneServer> phoneServers = phoneManager.getPhoneServers();
%>


<html>
<head>
    <title>Phone Mappings</title>
    <meta name="pageID" content="item-phone-users"/>
</head>

<body>


<style type="text/css">

    .phone-required {
        font-size: 7pt;
    }

    #phone-users .jive-table .jive-odd TD {
        border-bottom: 0px;
    }

    #phone-users .jive-table .jive-even TD {
        border-bottom: 0px;
    }

    #phone-users .jive-table .jive-odd-last TD {
        border-bottom: 1px #ccc solid; /*background-color: #fff;*/
    }

    #phone-users .jive-table .jive-even-last TD {
        border-bottom: 1px #ccc solid; /*background-color: #eee;*/
    }


</style>


<div id="phone-users">

    <% if (success) { %>

    <div class="jive-success">
        <table cellpadding="0" cellspacing="0" border="0">
            <tbody>
                <tr>
                    <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0"></td>
                    <td class="jive-icon-label">Operation completed successfully.</td>
                </tr>
            </tbody>
        </table>
    </div><br>

    <% }
    else if (errors.size() > 0) { %>

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

    <% } %>

    Total Users:
    <%= LocaleUtils.getLocalizedNumber(userCount) %> --

    Sorted by Username

    - Users per Page:
    <select size="1"
            onchange="location.href='phone-users.jsp?start=0&range=' + this.options[this.selectedIndex].value;">

        <% for (int i = 0; i < RANGE_PRESETS.length; i++) { %>

        <option value="<%= RANGE_PRESETS[i] %>"
                <%= (RANGE_PRESETS[i] == range ? "selected" : "") %>><%= RANGE_PRESETS[i] %></option>

        <% } %>

    </select>
<br/>

<% if (numPages > 1) { %>

<p>
    Pages:
    [
    <% int num = 15 + curPage;
        int s = curPage - 1;
        if (s > 5) {
            s -= 5;
        }
        if (s < 5) {
            s = 0;
        }
        if (s > 2) {
    %>
    <a href="phone-users.jsp?start=0&range=<%= range %>">1</a> ...

    <%
        }
        int i = 0;
        for (i = s; i < numPages && i < num; i++) {
            String sep = ((i + 1) < numPages) ? " " : "";
            boolean isCurrent = (i + 1) == curPage;
    %>
    <a href="phone-users.jsp?start=<%= (i*range) %>&range=<%= range %>"
       class="<%= ((isCurrent) ? "jive-current" : "") %>"
            ><%= (i + 1) %></a><%= sep %>

    <% } %>

    <% if (i < numPages) { %>

    ... <a href="phone-users.jsp?start=<%= ((numPages-1)*range) %>&range=<%= range %>"><%= numPages %></a>

    <% } %>

    ]

</p>

<% } %>

<div class="jive-table">
    <table cellpadding="0" cellspacing="0" border="0" width="100%">
        <thead>
            <tr>
                <th nowrap>Username</th>
                <th nowrap>Device</th>
                <th nowrap>Extension</th>
                <th nowrap>Caller ID</th>
                <th style="text-align:center;">Edit</th>
                <th style="text-align:center;">Delete</th>
            </tr>
        </thead>
        <tbody>

            <% if (users.size() == 0) { %>

            <tr>
                <td colspan="6">
                    No User/Device Mappings
                </td>
            </tr>

            <% } %>

            <%
                int i = start;
                boolean isFirst; // whether or not this is the first row for a user
                boolean isLast;
                for (PhoneUser currentUser : users) {
                    i++;
                    isFirst = true;
                    isLast = false;
            %>


            <%
                List<PhoneDevice> userDevices = phoneManager.getPhoneDevicesByUserID(currentUser.getID());
                int deviceListSize = userDevices.size();


                if (deviceListSize < 1 && start > 0) {


                }


                int j = 0;
                for (PhoneDevice currentDevice : userDevices) {
                    j++;
                    isLast = (j == deviceListSize); // we are the last entry
            %>

            <tr valign="top" class="jive-<%= (((i%2)==0) ? "even" : "odd") %><%= isLast ? "-last" : ""%>">
                <td><%=isFirst ? currentUser.getUsername() : "&nbsp;"%></td>
                <td><%=currentDevice.getDevice() %>
                    <% if (deviceListSize > 1) { %>
                    <span class="phone-required"><%= (currentDevice.isPrimary() ? "(primary)" : "")%></span>
                    <% } %>
                </td>
                <td><%=currentDevice.getExtension()%></td>
                <td><%=currentDevice.getCallerId() != null ? currentDevice.getCallerId() : "&nbsp;"%></td>
                <td align="center">
                    <a href="phone-users.jsp?deviceID=<%=currentDevice.getID()%>&userID=<%=currentUser.getID()%>&start=<%= start %>&range=<%= range %>">
                        <img src="images/edit-16x16.gif" width="16" height="16" alt="Edit" border="0">
                    </a>
                </td>
                <td align="center" style="border-right:1px #ccc solid;">
                    <a href="phone-users.jsp?delete=true&deviceID=<%=currentDevice.getID()%>&userID=<%=currentUser.getID()%>&start=<%= start %>&range=<%= range %>">
                        <img src="images/delete-16x16.gif" width="16" height="16" alt="Delete" border="0">
                    </a>
                </td>
            </tr>
            <% isFirst = false; %>
            <% } %>
            <% } %>
        </tbody>
    </table>
</div>

<div style="padding-top : 5px">

    <% if (numPages > 1) { %>

    <p>
        Pages:
        [
        <% int num = 15 + curPage;
            int s = curPage - 1;
            if (s > 5) {
                s -= 5;
            }
            if (s < 5) {
                s = 0;
            }
            if (s > 2) {
        %>
        <a href="phone-users.jsp?start=0&range=<%= range %>">1</a> ...

        <%
            }
            i = 0;
            for (i = s; i < numPages && i < num; i++) {
                String sep = ((i + 1) < numPages) ? " " : "";
                boolean isCurrent = (i + 1) == curPage;
        %>
        <a href="phone-users.jsp?start=<%= (i*range) %>&range=<%= range %>"
           class="<%= ((isCurrent) ? "jive-current" : "") %>"
                ><%= (i + 1) %></a><%= sep %>

        <% } %>

        <% if (i < numPages) { %>

        ... <a href="phone-users.jsp?start=<%= ((numPages-1)*range) %>&range=<%= range %>"><%= numPages %></a>

        <% } %>

        ]

    </p>

    <% } %>

</div>

<br/><br/>

<form action="phone-users.jsp" method="post">

<input type="hidden" name="deviceID" value="<%=deviceID%>"/>
<input type="hidden" name="userID" value="<%=userID%>"/>
<input type="hidden" name="start" value="<%=start%>"/>
<input type="hidden" name="range" value="<%=range%>"/>

<div class="jive-table">
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th colspan="2">
            <%= phoneDevice != null ? "Edit" : "Add"%> User/Asterisk Phone mapping
        </th>
    </tr>
</thead>
<tbody>
    <tr valign="top">
        <td width="1%">
            <nobr><label for="usernametf">* Username:</label></nobr>
        </td>
        <td width="99%">
            <% if (phoneDevice == null) { %>
            <input type="text" name="username" size="35" value="<%= username != null ? username
            : ""%>" id="usernametf"/>
            <% if (errors.containsKey("username")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("username")%></span>
            <% } %>
            <% }
            else { %>
            <%= username %>
            <% } %>
        </td>
    </tr>
    <% if (phoneServers.size() > 1) { %>
    <tr valign="top">
        <td width="1%" nowrap="nowrap">
            * Server:
        </td>
        <td width="99%">
            <select name="phoneServer" id="phoneServer">
                <option value="">Select</option>
                <% for(PhoneServer server : phoneServers) { %>
                <option value="<%=server.getID()%>"><%=server.getName()%></option>
                <% } %>
            </select>
        </td>
    </tr>
    <% } %>
    <tr valign="top">
        <td width="1%">
            <nobr><label for="devicetf">* Phone:</label></nobr>
        </td>
        <td width="99%">

            <% if (useSipDropDown && sipDevices != null) { %>
            <select name="device" id="devicetf">
                <option value="">Select</option>

                <% for (String current : sipDevices) { %>
                <option value="<%=current%>" <%=current.equals(device) ? "selected=\"selected\""
                        : ""%>><%=current%></option>
                <% } %>
            </select>

            or
            <% } %>

            <input type="text" name="devicetf" size="35"
                   value="<%=device != null && (sipDevices != null && !sipDevices.contains(device))
                   ? device : "" %>" id="devicetf"/>
            <% if (errors.containsKey("device")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("device")%></span>
            <% } %>
        </td>
    </tr>
    <tr valign="top">
        <td width="1%">
            <nobr><label for="extensiontf">* Extension:</label></nobr>
        </td>
        <td width="99%">
            <input type="text" id="extensiontf" name="extension" size="35"
                    value="<%= extension != null ? extension : ""%>"/>
            <% if (errors.containsKey("extension")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("extension")%></span>
            <% } %>
        </td>
    </tr>
    <tr valign="top">
        <td width="1%">
            <nobr><label for="callerIDtf">Caller ID:</label></nobr>
        </td>
        <td width="99%">
            <input type="text" name="callerID" size="35"
                   value="<%= callerID != null ? callerID : ""%>"
                   id="callerIDtf"/>
            <% if (errors.containsKey("callerID")) { %>
            <br/>
            <span class="jive-error-text"><%=errors.get("callerID")%></span>
            <% } %>
        </td>
    </tr>
    <tr>
        <td width="1%">
            <nobr><label for="isPrimary">Primary:</label></nobr>
        </td>
        <td width="99%">
            <input type="checkbox" id="isPrimary" name="primary" value="true"
                    <%= isPrimary ? "checked" : ""%> />
        </td>
    </tr>
</tbody>
<tfoot>
    <tr>
        <td colspan="2">
            <input type="submit" name="save" value="<%= phoneDevice != null ? "Save" : "Add"%>"/>
            <input type="submit" name="cancel" value="Cancel"/>
        </td>
    </tr>
</tfoot>
</table>
</div>

</form>

<span class="jive-description">
    * Required fields
</span>

</div>


<%
    }
    catch (Exception e) {
        Log.error(e);
    }

%>

</body>
</html>


<%!
    // checks to see if there is a primary
    boolean containsPrimary(Collection<PhoneDevice> devices, PhoneDevice ignored) {

        if (devices != null) {
            for (PhoneDevice current : devices) {

                if (current.isPrimary() &&
                        current.getID() != ignored.getID()) {
                    return true;
                }

            }
        }

        return false;
    }

%>