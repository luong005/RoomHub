package com.roommate_connection_platform.customexceptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private Object details;
}

