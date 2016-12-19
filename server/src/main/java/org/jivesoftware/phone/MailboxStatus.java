/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

/**
 * Object that tells how many old and new messages are in a particular mailbox
 *
 * @author Andrew Wright
 */
public class MailboxStatus {

    private String mailbox;
    private int oldMessages;
    private int newMessages;

    /**
     * Creates a new MailboxStatus object
     *
     * @param mailbox     the name of the mailbox
     * @param oldMessages number of old messages
     * @param newMessages number of new messages
     */
    public MailboxStatus(String mailbox, int oldMessages, int newMessages) {
        this.mailbox = mailbox;
        this.oldMessages = oldMessages;
        this.newMessages = newMessages;
    }

    /**
     * Returns the name of the Mailbox that this status object is for
     *
     * @return the name of the Mailbox that this status object is for
     */
    public String getMailbox() {
        return mailbox;
    }

    /**
     * Returns the number of old messages stored in the mailbox
     *
     * @return the number of old messages stored in the mailbox
     */
    public int getOldMessages() {
        return oldMessages;
    }

    /**
     * Returns the number of new messages stored in the mailbox
     *
     * @return the number of new messages stored in the mailbox
     */
    public int getNewMessages() {
        return newMessages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MailboxStatus that = (MailboxStatus) o;

        if (newMessages != that.newMessages) {
            return false;
        }
        if (oldMessages != that.oldMessages) {
            return false;
        }
        return !(mailbox != null ? !mailbox.equals(that.mailbox) : that.mailbox != null);

    }

    @Override
    public int hashCode() {
        int result;
        result = (mailbox != null ? mailbox.hashCode() : 0);
        result = 29 * result + oldMessages;
        result = 29 * result + newMessages;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MailboxStatus");
        sb.append("{mailbox='").append(mailbox).append('\'');
        sb.append(", oldMessages=").append(oldMessages);
        sb.append(", newMessages=").append(newMessages);
        sb.append('}');
        return sb.toString();
    }
}
