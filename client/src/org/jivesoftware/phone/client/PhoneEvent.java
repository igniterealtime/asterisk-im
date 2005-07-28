package org.jivesoftware.phone.client;

/**
 * Element used for phone events
 *
 * @author Andrew Wright
 */
public interface PhoneEvent {
    
    /**
     * Possible status event status values
     */
    public static enum EventStatus {
        /**
         * The user is has answered and is on the phone
         */
        ON_PHONE,
        /**
         * The user has hung up their phone
         */
        HANG_UP,
        /**
         * The user's phone is ringing
         */
        RING,
        /**
         * If we have dialed and we are waiting for the other user to answer
         */
        DIALED
    }

    /**
     * Returns status of the current event
     *
     * @return status of the current event
     */
    EventStatus getEventStatus();

    /**
     * The device (specific sip device) that the event is happening for
     *
     * @return The device this event is tied to
     */
    String getDevice();

    /**
     * Call session id of the event
     *
     * @return the call session id
     */
    String getCallID();

}
