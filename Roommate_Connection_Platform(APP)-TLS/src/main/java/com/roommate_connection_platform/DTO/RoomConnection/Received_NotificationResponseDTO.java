package com.roommate_connection_platform.DTO.RoomConnection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class Received_NotificationResponseDTO {
    private Long id;
    private Long postId;
    private Long roomConnectionId;
    private Long senderId;
    private String senderName;
    private String type;
    private Boolean read;
    private LocalDateTime createdAt;
}
