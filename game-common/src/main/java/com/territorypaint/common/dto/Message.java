package com.territorypaint.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.territorypaint.common.protocol.ActionType;

import java.util.UUID;

/**
 * Envelope for all TCP communications between client and server.
 * Requirements: 13.1, 13.2
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {
    private ActionType actionType;
    private UUID requestId; // Client-provided for requests, null for events
    private String timestamp; // UTC ISO-8601
    private Object payload; // Specific DTO based on actionType
    
    public Message() {
    }
    
    public Message(ActionType actionType, Object payload) {
        this.actionType = actionType;
        this.payload = payload;
    }
    
    public Message(ActionType actionType, UUID requestId, String timestamp, Object payload) {
        this.actionType = actionType;
        this.requestId = requestId;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public static Message event(ActionType actionType, Object payload) {
        return new Message(actionType, null, java.time.Instant.now().toString(), payload);
    }

    public static Message reply(ActionType actionType, UUID requestId, Object payload) {
        return new Message(actionType, requestId, java.time.Instant.now().toString(), payload);
    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
    
    public UUID getRequestId() {
        return requestId;
    }
    
    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public Object getPayload() {
        return payload;
    }
    
    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
