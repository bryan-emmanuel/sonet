package com.myspace.sdk;

@SuppressWarnings("serial")
public class MSRequestException extends Exception {

    private int code;
    private String message;
    
    public MSRequestException(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public MSRequestException(String message) {
        this.message = message;
    }
    
    public int getCode() { return code; }
    
    public String getMessage() { return message; }
    
    public String toString() {
        return "MSRequestException (" + code + "): " + message;
    }
}
