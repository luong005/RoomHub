package com.roommate_connection_platform.customexceptions;

public class InvalidParamException extends RuntimeException{
    public InvalidParamException(String message) {
        super(message);
    }
}
