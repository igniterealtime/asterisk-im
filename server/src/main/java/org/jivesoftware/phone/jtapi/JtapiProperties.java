/*
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.phone.jtapi;

import org.jivesoftware.phone.PhoneProperties;

/**
 * 
 * Jive property keys this plugin expects
 * 
 * @author Jens Wilke
 * 
 */
public class JtapiProperties extends PhoneProperties {

	public static final String JTAPI_PROVIDER = "jtapi.provider";

	public static final String JTAPI_PEER = "jtapi.peer";
	
	public static final String JTAPI_PARAMS = "jtapi.params";

	public static final String INCOMING_CONTEXT = "jtapi.IncomingContext";

	public static final String TERMINAL_CONTEXT = "jtapi.TerminalContext";

	public static final String OUTGOING_CONTEXT = "jtapi.OutgoingContext";

}