package com.roommate_connection_platform.DTO.RoomConnection;

import com.roommate_connection_platform.enums.RoomConnectionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoomConnectionRequest {
    @NotNull
    private Long roomConnectionId;

    @NotNull
    private RoomConnectionStatus status;
}
