package com.territorypaint.common.payload;

/**
 * Error message payload.
 * Requirements: 13.1, 13.2
 */
public class ErrorPayload {
    private String code;
    private String message;
    
    public ErrorPayload() {
    }
    
    public ErrorPayload(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
