package com.roommate_connection_platform.DTO.RoomConnection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JoinedRoomMemberResponseDTO {
    private Long roomConnectionId;
    private Long userId;
    private String fullName;
}
