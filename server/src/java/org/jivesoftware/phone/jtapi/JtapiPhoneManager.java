/*
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.phone.jtapi;

import javax.telephony.Call;
import javax.telephony.InvalidArgumentException;
import javax.telephony.JtapiPeer;
import javax.telephony.JtapiPeerFactory;
import javax.telephony.JtapiPeerUnavailableException;
import javax.telephony.MethodNotSupportedException;
import javax.telephony.Provider;
import javax.telephony.ResourceUnavailableException;
import javax.telephony.Terminal;
import javax.telephony.Address;

import org.jivesoftware.phone.*;
import org.jivesoftware.phone.database.PhoneDAO;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.xmpp.packet.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PhoneManager implementation for Jtapi
 *
 * @author Jens Wilke
 */
public class JtapiPhoneManager extends BasePhoneManager  {

    private static final Logger log = Logger.getLogger(JtapiPhoneManager.class.getName());
    private Provider provider;
    private JtapiEvents events;

    public JtapiPhoneManager(PhoneDAO dao) {
        super(dao);
    }
    
    public void init(JtapiPlugin plugin) throws JtapiPeerUnavailableException  {

        // idea: support an asterisk specific setup if that configuration is set
        // otherwise move to a generic JTAPI configuration scheme
        // Populate the manager configuration
    	boolean enabled = 
    		JiveGlobals.getBooleanProperty(JtapiProperties.ENABLED, false);
    	if (!enabled) {
    		return;
    	}
        String _peerName = 
    		JiveGlobals.getProperty(JtapiProperties.JTAPI_PEER, null); 
    	String _provider = 
    		JiveGlobals.getProperty(JtapiProperties.JTAPI_PROVIDER,
    			"com.headissue.asterisk.jtapi.AsteriskJtapiProvider");
        String _providerString = _provider;
        PhoneOption[] options = plugin.getJtapiOptions();
        boolean ouch = false;
        for (int i=0; options!=null && i<options.length; i++) {
        	PhoneOption o = options[i];
        	String p = o.getPropertyName();
        	String v = JiveGlobals.getProperty(p);
        	String err = o.check(v);
        	if (err!=null) {
        		Log.error("Property \""+p+"\" error: "+err);
        	}
        	_providerString += ";"+
        		o.getParamName()+"="+v;
        }
        if (ouch) {
        	return;
        }
        String add = JiveGlobals.getProperty("jtapi.params");
        if (add!=null) {
        	_providerString += ";"+add;
        }
		JtapiPeer _peer = JtapiPeerFactory.getJtapiPeer(_peerName);
        Log.info("Initializing JTAPI provider, providerString="+_providerString);
        provider = _peer.getProvider(_providerString);
        events = new JtapiEvents();
        events.phoneManager = this;
        events.plugin = plugin;
        registerTerminalListeners();
    }

    private void registerTerminalListeners() {
    	Log.info("jtapi: registering terminal listeners");
    	boolean x = false;
        Terminal ta[] = null;
    	try {
    		ta = provider.getTerminals();
    	} catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    	if (ta!=null) {
    		for (Terminal t : ta) {
    			PhoneDevice d = getDevice(t.getName());
    			if (d!=null) {
    				Log.info("jtapi: Listening for events on terminal "+t.getName());
    				x = true;
    				try {
						t.addCallListener(events);
					} catch (ResourceUnavailableException e) {
						e.printStackTrace();
					} catch (MethodNotSupportedException e) {
						e.printStackTrace();
					}
    			}
    		}
    	}
    	if (!x) {
    		Log.info("jtapi: Not listening on incoming calls!");
    	}
    }
    
	public void destroy() {
		Log.info("Shutdown JTAPI provider");
		if (provider!=null && provider.getState()==Provider.IN_SERVICE) {
			provider.shutdown();
		}
	}

	public MailboxStatus mailboxStatus(long serverID, String mailbox) throws PhoneException {
    	// not possible
    	return new MailboxStatus(mailbox, 0,0);
    }

	
	
    @Override
	public void insert(PhoneDevice phoneDevice) {
		super.insert(phoneDevice);
		Terminal t = null;
		try {
			 t = provider.getTerminal(phoneDevice.getDevice());
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
			return;
		}
		try {
			t.addCallListener(events);
		} catch (ResourceUnavailableException e) {
			e.printStackTrace();
		} catch (MethodNotSupportedException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void remove(PhoneDevice phoneDevice) {
		String n = phoneDevice.getDevice();
		super.remove(phoneDevice);
		PhoneDevice d = getDevice(n);
		// if we have this device still in the database, dont remove the listener
		if (d!=null) {
			return;
		}
		Terminal t = null;
		try {
			 t = provider.getTerminal(phoneDevice.getDevice());
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
			return;
		}
		t.removeCallListener(events);
	}

	public Map<Long, Collection<String>> getDevices() throws PhoneException {
    	Terminal ta[];
    	try {
    		ta = provider.getTerminals();
    	} catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new PhoneException("Unable to retrieve terminal list ", e);
        }
    	List<String> devices = new ArrayList<String>();
    	for (Terminal t : ta) {
    		devices.add(t.getName());
    	}
        Collections.sort(devices);
        Map<Long, Collection<String>> toReturn = new HashMap<Long, Collection<String>>();

        toReturn.put((long) 0, devices);

        return toReturn;
    }

    public Collection<String> getDevices(long serverID) throws PhoneException {
        return getDevices().get(0);
    }

    public void dial(String username, String extension, JID jid) throws PhoneException {
        PhoneUser user = getPhoneUserByUsername(username);
        PhoneDevice primaryDevice = getPrimaryDevice(user.getID());
        try {
            Terminal t = provider.getTerminal(primaryDevice.getDevice());
            // check for the right caller id
            Address aa[] = t.getAddresses();
            Address callerId = null;
            for (Address a : aa) {
                if (a.getName().equals(primaryDevice.getCallerId())) {
                    callerId = a;
                }
            }
            if (callerId == null) {
                callerId = aa[0];
            }
            Call c = provider.createCall();
            c.connect(t, callerId, extension);
        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new PhoneException("Unabled to dial extention " + extension, e);
        }
    }

    public void forward(String callSessionID, String username, String extension, JID jid) throws PhoneException {
    	String str = "not yet implemented";
        log.log(Level.SEVERE, str);
        throw new PhoneException("Unabled to forward " + extension +": "+ str);
    }

	public boolean isConnected() {
		return 
			provider!=null && provider.getState() == Provider.IN_SERVICE;
	}


}
