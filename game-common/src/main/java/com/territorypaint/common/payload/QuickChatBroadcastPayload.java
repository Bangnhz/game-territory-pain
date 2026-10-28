package com.territorypaint.common.payload;

/**
 * Payload broadcast from server to both players when a quick chat is sent.
 * Server → Client direction.
 */
public class QuickChatBroadcastPayload {

    /** The userId of the player who sent the chat. */
    private Long senderUserId;

    /** The username of the player who sent the chat. */
    private String senderUsername;

    /** The actual quick-chat phrase text. */
    private String phrase;

    /** Player slot (1 or 2) of the sender, so the client knows which side to show the bubble. */
    private int senderSlot;

    public QuickChatBroadcastPayload() {
    }

    public QuickChatBroadcastPayload(Long senderUserId, String senderUsername, int senderSlot, String phrase) {
        this.senderUserId = senderUserId;
        this.senderUsername = senderUsername;
        this.senderSlot = senderSlot;
        this.phrase = phrase;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(Long senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public int getSenderSlot() {
        return senderSlot;
    }

    public void setSenderSlot(int senderSlot) {
        this.senderSlot = senderSlot;
    }
}
