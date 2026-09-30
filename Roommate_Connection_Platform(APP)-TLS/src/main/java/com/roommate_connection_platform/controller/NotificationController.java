package com.roommate_connection_platform.controller;

import com.roommate_connection_platform.DTO.RoomConnection.Received_NotificationResponseDTO;
import com.roommate_connection_platform.DTO.RoomConnection.Sent_NotificationResponseDTO;
import com.roommate_connection_platform.services.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final INotificationService notificationService;

    // lay thong bao user gui di (request join room)
    @GetMapping(value = "/api/Sent_notifications")
    public ResponseEntity<?> getSent_notifications(){
        List<Sent_NotificationResponseDTO> notifies =notificationService.getRoomConnectionBySenderId();
        return ResponseEntity.ok(notifies);
    }


    // lay thong bao user nhan duoc (nhan dc tu post)
    @GetMapping(value = "/api/Received_notifications")
    public ResponseEntity<?> getReceived_notifications(){
        List<Received_NotificationResponseDTO> notifies = notificationService.getRoomConnectionByReceiverId();
        return ResponseEntity.ok(notifies);
    }

    // hien thi chi tiet thong bao
    @GetMapping(value = "/api/announcement/{id}")
    public ResponseEntity<?> getAnnouncement(@PathVariable Long id){

        return null;
    }
}
