package com.roommate_connection_platform.controller;

import com.roommate_connection_platform.DTO.Chat.MarkReadRequestDTO;
import com.roommate_connection_platform.DTO.Chat.ChatRoomResponseDTO;
import com.roommate_connection_platform.DTO.Chat.ChatSocketEventDTO;
import com.roommate_connection_platform.services.IChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {
    private final IChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // Lay danh sach phong chat da chap nhan de hien thi o inbox.
    @GetMapping("/api/chat-rooms")
    public ResponseEntity<?> getInboxRooms() {
        return ResponseEntity.ok(chatService.getInboxRooms());
    }

    // Lấy danh sách tin nhắn đang chờ
    @GetMapping("/api/chat-rooms/requests")
    public ResponseEntity<?> getMessageRequests() {
        return ResponseEntity.ok(chatService.getMessageRequests());
    }

    // Lay danh sach phong chat da bi chan cua user hien tai.
    @GetMapping("/api/chat-rooms/blocked")
    public ResponseEntity<?> getBlockedRooms() {
        return ResponseEntity.ok(chatService.getBlockedRooms());
    }

    // Lay lich su tin nhan cua mot phong chat theo phan trang.
    @GetMapping("/api/chat-rooms/{roomId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(chatService.getMessages(roomId, page, size));
    }

    // Chap nhan tin nhan cho de chuyen phong chat sang trang thai ACCEPTED.
    @PatchMapping("/api/chat-rooms/{roomId}/accept")
    public ResponseEntity<?> acceptRoom(@PathVariable Long roomId) {
        ChatRoomResponseDTO room = chatService.acceptRoom(roomId);
        messagingTemplate.convertAndSend("/topic/chat-rooms/" + roomId, buildRoomEvent("CHAT_ACCEPTED", room));
        return ResponseEntity.ok(room);
    }

    // Chan phong chat; nguoi bi chan se khong gui tiep duoc tin nhan.
    @PatchMapping("/api/chat-rooms/{roomId}/block")
    public ResponseEntity<?> blockRoom(@PathVariable Long roomId) {
        ChatRoomResponseDTO room = chatService.blockRoom(roomId);
        messagingTemplate.convertAndSend("/topic/chat-rooms/" + roomId, buildRoomEvent("CHAT_BLOCKED", room));
        return ResponseEntity.ok(room);
    }

    // Cap nhat user hien tai da doc toi tin nhan nao trong phong chat.
    @PatchMapping("/api/chat-rooms/{roomId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long roomId,
            @Valid @RequestBody MarkReadRequestDTO request
    ) {
        return ResponseEntity.ok(chatService.markAsRead(roomId, request));
    }

    private ChatSocketEventDTO buildRoomEvent(String type, ChatRoomResponseDTO room) {
        return ChatSocketEventDTO.builder()
                .type(type)
                .roomId(room.getId())
                .room(room)
                .build();
    }
}
