package com.roommate_connection_platform.DTO.RoomConnection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class Sent_NotificationResponseDTO {
    private Long id;
    private Long postId;
    private Long roomConnectionId;
    private Long receiverId;
    private String receiverName;
    private String type;
    private Boolean read;
    private LocalDateTime createdAt;
}
