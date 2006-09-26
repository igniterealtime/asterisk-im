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
import java.util.Collection;
import java.util.Arrays;
import java.util.List;

public class AjtapiPlugin extends JtapiPlugin {

    public AjtapiPlugin() {
        super();
    }

    public Collection<? extends PhoneOption> getJtapiOptions() {
        return Arrays.asList(
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
                        "OutgoingContext"));
    }

    public Collection<PhoneOption> getOptions() {
        List<PhoneOption> l = new ArrayList<PhoneOption>();
        Collection<? extends PhoneOption> po = getJtapiOptions();
        l.addAll(po);
        l.add(new PhoneOption("Drop-down device selection",
                PhoneProperties.DEVICE_DROP_DOWN,
                "DropDown",
                PhoneOption.Type.flag));
        return l;
    }

}
