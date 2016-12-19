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
    public static final class EventStatus {
        /**
         * The user is has answered and is on the phone
         */
        public static final EventStatus ON_PHONE = new EventStatus("ON_PHONE");

        /**
         * The user has hung up their phone
         */
        public static final EventStatus HANG_UP = new EventStatus("HANG_UP");

        /**
         * The user's phone is ringing
         */
        public static final EventStatus RING = new EventStatus("RING");

        /**
         * If we have dialed and we are waiting for the other user to answer
         */
        public static final EventStatus DIALED = new EventStatus("DIALED");

        private String name;

        private EventStatus(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final EventStatus that = (EventStatus) o;

            return !(name != null ? !name.equals(that.name) : that.name != null);

        }

        public int hashCode() {
            return (name != null ? name.hashCode() : 0);
        }

        public String toString() {
            return name;
        }

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
