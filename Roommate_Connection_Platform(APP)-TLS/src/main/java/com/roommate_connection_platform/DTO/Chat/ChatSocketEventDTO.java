package com.roommate_connection_platform.DTO.Chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSocketEventDTO {
    private String type;
    private Long roomId;
    private ChatRoomResponseDTO room;
    private MessageResponseDTO message;
}
