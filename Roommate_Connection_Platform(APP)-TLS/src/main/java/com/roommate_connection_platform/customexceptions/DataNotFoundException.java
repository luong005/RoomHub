package com.roommate_connection_platform.customexceptions;

public class DataNotFoundException extends RuntimeException{
    public DataNotFoundException(String message) {
        super(message);
    }
}
