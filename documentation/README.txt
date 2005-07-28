Phone

client:
- smack (http://jivesoftware.org/smack)

PREREQUISITES:
- Asterisk installed with manager api enabled

INSTALLATION:

1. Copy the phone plugin jar file to the MESSENGER_HOME/plugins directory.
2. Restart the messenger
3. Configure the asterisk manager connection on the PBX Integration page

USING THE CLIENT API:

The client api first requires that the messenger phone plugin is installed.

To initialize the client do the following:

XMPPConnection conn = new XMPPConnection("myserver.foo.com");
PhoneClient client = new PhoneClient(conn);

PHONE EVENTS:

Phone events are notifications from the server that the pbx is going through a state change with
one of the current users devices. Phone events are XMPP packets with a child element phone-event that belongs to the namespace http://jivesoftware.com/xmlns/phone.
Phone events xml elements contain a type attribute describing what kind of event is being dispatched.
Phone events can also contain an attribute device which tells which device (line) the user is receiving a call on.
This is useful if the user has more than one telephone line.

Current Phone Events

ON_PHONE:
Packet: Presence packet with an "away" presence.
Client Class: org.jivesoftware.phone.client.AnswerEvent
Description: Signifies that the user has answered a call.
Children: Contains a callerID child element containg the caller id of the caller

HANG_UP:
Packet: Presence packet that sets the user's presence back to its previous presence before the call.
Client Class: org.jivesoftware.phone.client.HangUpEvent
Description: Signifies that the user has hung up the phone.
Children: None

RING:
Packet: Message Packet
Client Class: org.jivesoftware.phone.client.RingingEvent
Description: Signifies that the user's phone is ringing.
Children: Contains a callerID child element containg the caller id of the caller

Registering an Event Listener with the client api via the PhoneClient#addEventListener method:

PhoneClient client = new PhoneClient(conn);
client.addEventListener(new BasePhoneEventListener() {

    // Override the onRing event to see when we are receiving a call
    public void onRing(RingEvent event) {
        log.info("receiving a call from " + event.getCallerID());
    }

});

PHONE ACTIONS:

Phone actions are requests from the client to perform tasks such as dialing an number.
Phone actions are IQ packets with child node of phone-action that belongs to the namespace http://jivesoftware.com/xmlns/phone.
Phone actions have a type attribute which determines what action to be performed (ie DIAL, FORWARD)

Current Phone Actions:

DIAL
Description: Used to dial an extension (or full phone number).
Children: Contains an element extension which contains the number to be dialed.

FORWARD
Description: Forwards a call to another extension
Children: Contains an element extension which contains the number to be dialed.

