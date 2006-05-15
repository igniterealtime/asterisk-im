/*
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.phone.ajtapi;

import org.jivesoftware.phone.PhoneOption;
import org.jivesoftware.phone.PhoneProperties;
import org.jivesoftware.phone.RequiredOption;
import org.jivesoftware.phone.jtapi.JtapiPlugin;
import org.jivesoftware.phone.jtapi.JtapiProperties;
import java.util.ArrayList;

public class AjtapiPlugin extends JtapiPlugin {

	public AjtapiPlugin() {
		super();
	}

    public PhoneOption[] getJtapiOptions() {
    	return new PhoneOption[]{
 				new RequiredOption("Server",
    	    			JtapiProperties.SERVER,
    	    			"Server"),
    	 		new RequiredOption("Port",
    	    			JtapiProperties.PORT,
    	    			"Port"),
    	    	new RequiredOption("Username",
						JtapiProperties.USERNAME,
						"Login"),
    			new RequiredOption("Password",
 						JtapiProperties.PASSWORD,
 						"Password"){
    					public boolean isPassword() { return true; }
    			},
    			new RequiredOption("Incoming Context",
    					JtapiProperties.INCOMING_CONTEXT,
    					"IncomingContext"),
    			new RequiredOption("Terminal Context",
    					JtapiProperties.TERMINAL_CONTEXT,
    					"TerminalContext"),
    			new RequiredOption("Outgoing Context", 
    					JtapiProperties.OUTGOING_CONTEXT,
    					"OutgoingContext")}; 
    }
    
    public PhoneOption[] getOptions() {
    	ArrayList<PhoneOption> l = new ArrayList<PhoneOption>();
    	PhoneOption[] po = getJtapiOptions();
    	for (int i=0; i<po.length; i++) {
    		l.add(po[i]);
    	}
    	l.add(new PhoneOption("Drop-down device selection",
    			PhoneProperties.DEVICE_DROP_DOWN,
    			"DropDown",
    			PhoneOption.FLAG));
    	po = new PhoneOption[l.size()];
    	l.toArray(po);
    	return po;
    }
	
}
