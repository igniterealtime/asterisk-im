package org.jivesoftware.phone.asterisk;

import org.asteriskjava.live.*;
import org.asteriskjava.live.internal.AsteriskAgentImpl;
import org.jivesoftware.phone.PhoneUser;
import org.jivesoftware.util.Log;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Handles state changes received from an Asterisk server.
 *
 * @author Stefan Reuter
 */
public class AsteriskIMServerListener extends AbstractAsteriskServerListener implements PropertyChangeListener
{
    private final long serverID;
    private final AsteriskPhoneManager phoneManager;
    private final CallSessionFactory callSessionFactory;

    public AsteriskIMServerListener(long serverID, AsteriskPhoneManager asteriskPhoneManager,
                                    CallSessionFactory callSessionFactory)
    {
        this.serverID = serverID;
        this.phoneManager = asteriskPhoneManager;
        this.callSessionFactory = callSessionFactory;
    }

    @Override
    public void onNewAsteriskChannel(AsteriskChannel channel)
    {
        final PhoneUser phoneUser;

        // Do nothing when the phoneManager is being removed/destroyed
        if (!phoneManager.isReady())
        {
            return;
        }

        // watch channel for changes
        channel.addPropertyChangeListener(this);

        // create a new session if this channel belongs to a phone user
        phoneUser = getPhoneUserForChannel(channel.getName());
        if (phoneUser != null)
        {
            createNewCallSession(phoneUser, channel);
        }
    }

    public void onNewAgent(AsteriskAgentImpl agent)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void onNewQueueEntry(AsteriskQueueEntry entry)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void onNewAgent(AsteriskAgent agent)
    {
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
        final AsteriskChannel channel;
        final CallSession session;

        // Do nothing when the phoneManager is being removed/destroyed
        if (!phoneManager.isReady())
        {
            return;
        }

        channel = (AsteriskChannel) evt.getSource();
        session = getCallSession(channel);

        if (AsteriskChannel.PROPERTY_NAME.equals(evt.getPropertyName()))
        {
            final PhoneUser phoneUser = getPhoneUserForChannel((String) evt.getNewValue());

            // renaming of a channel we have an active session for?
            if (session != null)
            {
                // channel no longer belongs to any phone user?
                if (phoneUser == null)
                {
                    destroyCallSession(channel);
                }
                // channel now belongs to another phone user?
                else if (!session.getUsername().equals(phoneUser.getUsername()))
                {
                    destroyCallSession(channel);
                    createNewCallSession(phoneUser, channel);
                }
            }
            // no session for this channel yet?
            else
            {
                // does it now belong to a phone user?
                if (phoneUser != null)
                {
                    createNewCallSession(phoneUser, channel);
                }
            }
        }
        else if (AsteriskChannel.PROPERTY_DIALED_CHANNEL.equals(evt.getPropertyName()))
        {
            final AsteriskChannel dialedChannel = (AsteriskChannel) evt.getNewValue();

            if (session == null)
            {
                return;
            }

            // TODO what do we need dialing information for? (srt)
            if (dialedChannel.getCallerId() != null) {
                session.setCallerID(dialedChannel.getCallerId().getNumber());
                session.setCallerIDName(dialedChannel.getCallerId().getName());
            }
            callSessionFactory.modifyCallSession(session, CallSession.Status.dialed);
        }
        else if (AsteriskChannel.PROPERTY_DIALING_CHANNEL.equals(evt.getPropertyName()))
        {
            final AsteriskChannel dialingChannel = (AsteriskChannel) evt.getNewValue();

            if (session == null)
            {
                return;
            }

            if (dialingChannel.getCallerId() != null)
            {
                session.setCallerID(dialingChannel.getCallerId().getNumber());
                session.setCallerIDName(dialingChannel.getCallerId().getName());
            }
            callSessionFactory.modifyCallSession(session, CallSession.Status.ringing);
        }
        else if (AsteriskChannel.PROPERTY_STATE.equals(evt.getPropertyName()))
        {
            if (ChannelState.HUNGUP.equals(evt.getNewValue()))
            {
                destroyCallSession(channel);
            }
        }
    }

    private CallSession getCallSession(AsteriskChannel channel)
    {
        return callSessionFactory.getCallSession(channel.getId());
    }

    private void createNewCallSession(PhoneUser phoneUser, AsteriskChannel channel)
    {
        final CallSession session;

        session = callSessionFactory.createCallSession(serverID, channel.getId(), phoneUser.getUsername());
        if (channel.getCallerId() != null)
        {
            session.setCallerID(channel.getCallerId().getNumber());
            session.setCallerIDName(channel.getCallerId().getName());
        }
        callSessionFactory.modifyCallSession(session, CallSession.Status.onphone);
    }

    private void destroyCallSession(AsteriskChannel channel)
    {
        callSessionFactory.destroyPhoneSession(channel.getId());
    }

    protected PhoneUser getPhoneUserForChannel(String channelName)
    {
        final String device;
        final PhoneUser phoneUser;

        //everything after the hyphen should be skipped
        device = AsteriskUtil.getDevice(channelName);

        // TODO check if we really only need active users
        phoneUser = phoneManager.getActivePhoneUserByDevice(serverID, device);

        if (phoneUser == null)
        {
            Log.debug("Could not find device/jid mapping for device '" + device + "': returning null");
            return null;
        }

        Log.debug("Found device mapping for device '" + device + "': returning " + phoneUser);
        return phoneUser;
    }
}
