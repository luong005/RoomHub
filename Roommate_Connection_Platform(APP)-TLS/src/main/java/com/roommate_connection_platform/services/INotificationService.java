package com.roommate_connection_platform.services;

import com.roommate_connection_platform.DTO.RoomConnection.Received_NotificationResponseDTO;
import com.roommate_connection_platform.DTO.RoomConnection.Sent_NotificationResponseDTO;

import java.util.List;

public interface INotificationService {
    List<Sent_NotificationResponseDTO> getRoomConnectionBySenderId();

    List<Received_NotificationResponseDTO> getRoomConnectionByReceiverId();
}
