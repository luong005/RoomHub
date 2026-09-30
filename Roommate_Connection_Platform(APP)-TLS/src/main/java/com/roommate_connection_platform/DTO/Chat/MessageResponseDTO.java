package com.roommate_connection_platform.DTO.Chat;

import com.roommate_connection_platform.enums.MessageStatus;
import com.roommate_connection_platform.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponseDTO {
    private Long id;
    private Long roomId;
    private ChatUserResponseDTO sender;
    private String messageText;
    private MessageType messageType;
    private MessageStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
