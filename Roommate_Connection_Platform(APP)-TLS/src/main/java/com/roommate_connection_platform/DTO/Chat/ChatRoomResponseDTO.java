package com.roommate_connection_platform.DTO.Chat;

import com.roommate_connection_platform.enums.ChatRoomStatus;
import com.roommate_connection_platform.enums.ChatRoomType;
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
public class ChatRoomResponseDTO {
    private Long id;
    private String name;
    private ChatRoomType type;
    private ChatRoomStatus status;
    private Long createdById;
    private Long blockedById;
    private ChatUserResponseDTO otherUser;
    private MessageResponseDTO lastMessage;
    private Long unreadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
