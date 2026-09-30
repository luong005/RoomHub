package com.roommate_connection_platform.customexceptions;

public class PermissionDenyException extends RuntimeException{
    public PermissionDenyException(String message) {
        super(message);
    }
}
