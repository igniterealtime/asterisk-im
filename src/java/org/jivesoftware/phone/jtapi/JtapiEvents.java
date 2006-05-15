/*
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.phone.jtapi;

import java.util.HashMap;

import javax.telephony.Call;
import javax.telephony.CallEvent;
import javax.telephony.ConnectionEvent;
import javax.telephony.MetaEvent;
import javax.telephony.Terminal;
import javax.telephony.TerminalConnection;
import javax.telephony.TerminalConnectionEvent;
import javax.telephony.TerminalConnectionListener;
import javax.telephony.callcontrol.CallControlCall;

import org.jivesoftware.util.Log;

import org.jivesoftware.phone.*;
import org.jivesoftware.phone.element.PhoneEvent;
import org.jivesoftware.phone.element.PhoneStatus;
import org.jivesoftware.phone.element.PhoneStatus.Status;

import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

/**
 * 
 * @author Jens Wilke
 *
 */
public class JtapiEvents implements TerminalConnectionListener {
	
	PhoneManager phoneManager;
	PhonePlugin plugin;
	
	HashMap<Call, String> call2id = new HashMap<Call, String>();
	HashMap<String,Call> id2call = new HashMap<String, Call>();
	int callCnt = 0;
	HashMap<TerminalConnection, TerminalConnection> activeConnections = 
		new HashMap<TerminalConnection, TerminalConnection>(); 
	
	/**
	 * Constructs a new call id, factored out from getCallId()
	 */
	synchronized String constructCallId(Terminal t, Call c) {
		callCnt++;
		return callCnt+"";
	}
	
	/** Get a call id used in our XMPP comunication for a jtapi call object. */
	public String getCallId(Terminal t, Call c) {
		String s = (String) call2id.get(c);
		if (s==null) {
			synchronized(this) {
				// race condition!
				s = (String) call2id.get(c);
				if (s==null) {
					s = constructCallId(t,c);
					call2id.put(c,s);
					id2call.put(s,c);
				}
			}
		}
		return s;
	}

	/**
	 * Get the caller id.
	 */
	public String getCallerId(Terminal t, Call _call) {
		if (_call instanceof CallControlCall) {
			CallControlCall ccc = (CallControlCall) _call;
			return ccc.getCallingAddress().getName();
		}
		return null;
	}
	
	public void terminalConnectionActive(TerminalConnectionEvent ev) {
		// FIXME: maybe we need an event processing thread? ;jw
		activeConnections.put(
				ev.getTerminalConnection(), 
				ev.getTerminalConnection());
		onphone(ev.getTerminalConnection());
	}
	
	public void terminalConnectionRinging(TerminalConnectionEvent ev) {
		ring(ev.getTerminalConnection());
	}
	
	public void terminalConnectionDropped(TerminalConnectionEvent ev) {
		Object o = activeConnections.get(ev.getTerminalConnection());
		if (o!=null) {
			drop(ev.getTerminalConnection());
			activeConnections.remove(ev.getTerminalConnection());
		} else {
			missed(ev.getTerminalConnection());
		}
	}
	
	private void drop(TerminalConnection tc) {
		Terminal t = tc.getTerminal();
		String device = t.getName();
		PhoneUser phoneUser = phoneManager.getPhoneUserByDevice(device);
		plugin.restorePresence(phoneUser.getUsername());
		Message message = new Message();
		
		Call _call = tc.getConnection().getCall();
		String callerId = getCallerId(t, _call);
		message.setType(Message.Type.chat);
		message.setBody("Finished: "+callerId);
		// FIXME: hmm, if the user has no sessions, this packet gets withdrawn, is there a way
		// to queue it in? ;jw
		plugin.sendPacket2User(phoneUser.getUsername(), message);
	}

	public void missed(TerminalConnection tc) {
		Call _call = tc.getConnection().getCall();
		Terminal t = tc.getTerminal();
		String device = t.getName();
		PhoneUser phoneUser = phoneManager.getPhoneUserByDevice(device);
		Log.info("jtapi: Missed call on terminal "+t.getName());
		if (phoneUser==null) {			
			Log.info("OnPhoneTask: Could not find device/jid mapping for device " + device + " returning");
			return;
		}
        String id = getCallId(t, _call);
        String callerId = getCallerId(t, _call);
		
		Message message = new Message();
		message.setID(id);
		message.setType(Message.Type.chat);
		message.setBody("Missed Call: "+callerId);
		// FIXME: hmm, if the user has no sessions, this packet gets withdrawn, is there a way
		// to queue it in? ;jw
		plugin.sendPacket2User(phoneUser.getUsername(), message);
	
		plugin.restorePresence(phoneUser.getUsername());
	}

	
	public void onphone(TerminalConnection tc) {
		Call _call = tc.getConnection().getCall();
		Terminal t = tc.getTerminal();
		PhoneUser phoneUser = phoneManager.getActivePhoneUserByDevice(t.getName());
		if (phoneUser==null) {
			return;
		}
       // send message to client that we answered the phone
        String id = getCallId(t, _call);
        String callerId = getCallerId(t, _call);
		Message message = new Message();
		// FIXME: whats this ID? ;jw
		message.setID(id);
		PhoneEvent phoneEvent = new PhoneEvent(id, PhoneEvent.Type.ON_PHONE, t.getName());
		phoneEvent.addElement("callerID").setText(callerId);
		message.getElement().add(phoneEvent);
		plugin.sendPacket2User(phoneUser.getUsername(), message);

		/*-
		message = new Message();
		message.setID(id+"b");
		message.setType(Message.Type.chat);
		message.setBody("Active?: "+callerId);
		plugin.sendPacket2User(phoneUser.getUsername(), message);
		-*/

		// construct new on the phone presence
        Presence presence = new Presence();
        presence.setShow(Presence.Show.away);
        presence.setStatus("On Phone");
        PhoneStatus phoneStatus = new PhoneStatus(Status.ON_PHONE);
        presence.getElement().add(phoneStatus);
		
        // sent the presence and intercept other presences from the client
        plugin.setPresence(phoneUser.getUsername(), presence);
	}

	public void ring(TerminalConnection tc) {
		Call _call = tc.getConnection().getCall();
		Terminal t = tc.getTerminal();
		PhoneUser phoneUser = phoneManager.getActivePhoneUserByDevice(t.getName());
		Log.info("jtapi: Ring on terminal "+t.getName());
		if (phoneUser==null) {
			return;
		}
       // send message to client that our phone is ringing
        String id = getCallId(t, _call);
        String callerId = getCallerId(t, _call);
		Message message = new Message();
		message.setID(id);
		PhoneEvent phoneEvent = new PhoneEvent(id, PhoneEvent.Type.RING, t.getName());
		phoneEvent.addElement("callerID").setText(callerId);
		message.getElement().add(phoneEvent);
		plugin.sendPacket2User(phoneUser.getUsername(), message);
		
		message = new Message();
		message.setID(id+"b");
		message.setType(Message.Type.chat);
		message.setBody("Calling: "+callerId);
		plugin.sendPacket2User(phoneUser.getUsername(), message);
		
		// construct new on the phone presence
        Presence presence = new Presence();
        presence.setShow(Presence.Show.away);
        presence.setStatus("Phone Ringing");
        PhoneStatus phoneStatus = new PhoneStatus(Status.ON_PHONE);
        presence.getElement().add(phoneStatus);
		
        // sent the presence and intercept other presences from the client
        plugin.setPresence(phoneUser.getUsername(), presence);
	}

	public void terminalConnectionCreated(TerminalConnectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void terminalConnectionPassive(TerminalConnectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}



	public void terminalConnectionUnknown(TerminalConnectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void connectionAlerting(ConnectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void connectionConnected(ConnectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void connectionCreated(ConnectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void connectionDisconnected(ConnectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void connectionFailed(ConnectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void connectionInProgress(ConnectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void connectionUnknown(ConnectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void callActive(CallEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void callInvalid(CallEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void callEventTransmissionEnded(CallEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void singleCallMetaProgressStarted(MetaEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void singleCallMetaProgressEnded(MetaEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void singleCallMetaSnapshotStarted(MetaEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void singleCallMetaSnapshotEnded(MetaEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void multiCallMetaMergeStarted(MetaEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void multiCallMetaMergeEnded(MetaEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void multiCallMetaTransferStarted(MetaEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void multiCallMetaTransferEnded(MetaEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
}
