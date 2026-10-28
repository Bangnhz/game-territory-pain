package com.territorypaint.common.payload;

/**
 * Payload sent by client when sending a quick chat message during a match.
 * Client → Server direction.
 */
public class QuickChatPayload {

    /** Index into the predefined quick-chat phrase list (0-based). */
    private int phraseIndex;

    /** The match ID to validate the message belongs to the active match. */
    private String matchId;

    public QuickChatPayload() {
    }

    public QuickChatPayload(int phraseIndex, String matchId) {
        this.phraseIndex = phraseIndex;
        this.matchId = matchId;
    }

    public int getPhraseIndex() {
        return phraseIndex;
    }

    public void setPhraseIndex(int phraseIndex) {
        this.phraseIndex = phraseIndex;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }
}
