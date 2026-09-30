package com.roommate_connection_platform.controller;

import com.roommate_connection_platform.DTO.Chat.ChatRoomResponseDTO;
import com.roommate_connection_platform.DTO.Chat.ChatSocketEventDTO;
import com.roommate_connection_platform.DTO.Chat.DirectChatSocketMessageDTO;
import com.roommate_connection_platform.DTO.Chat.MessageResponseDTO;
import com.roommate_connection_platform.DTO.Chat.RoomChatSocketMessageDTO;
import com.roommate_connection_platform.DTO.Chat.SendMessageRequestDTO;
import com.roommate_connection_platform.customexceptions.BusinessException;
import com.roommate_connection_platform.enums.MessageType;
import com.roommate_connection_platform.models.UserEntity;
import com.roommate_connection_platform.services.IChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    private final IChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // Gui tin dau tien toi user khac bang websocket va tao room PENDING neu can.
    @MessageMapping("/chat.direct")
    public void sendDirectMessage(@Valid @Payload DirectChatSocketMessageDTO payload, Principal principal) {
        Long senderId = getUserId(principal);
        SendMessageRequestDTO request = SendMessageRequestDTO.builder()
                .messageType(payload.getMessageType())
                .messageText(payload.getMessageText())
                .build();

        MessageResponseDTO message = chatService.sendDirectMessageAsUser(payload.getReceiverId(), request, senderId);
        ChatRoomResponseDTO senderRoom = chatService.getRoomForUser(message.getRoomId(), senderId);
        ChatRoomResponseDTO receiverRoom = chatService.getRoomForUser(message.getRoomId(), payload.getReceiverId());

        ChatSocketEventDTO senderEvent = buildEvent("DIRECT_MESSAGE_CREATED", senderRoom, message);
        ChatSocketEventDTO receiverEvent = buildEvent("DIRECT_MESSAGE_REQUESTED", receiverRoom, message);

        messagingTemplate.convertAndSend("/topic/chat-rooms/" + message.getRoomId(), senderEvent);
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/chat-rooms", senderEvent);
        messagingTemplate.convertAndSendToUser(chatService.getWebSocketUsername(payload.getReceiverId()), "/queue/chat-rooms", receiverEvent);
    }

    // Gui tin vao room da co; backend luu DB roi broadcast realtime cho cac client dang subscribe room.
    @MessageMapping("/chat.send")
    public void sendRoomMessage(@Valid @Payload RoomChatSocketMessageDTO payload, Principal principal) {
        Long senderId = getUserId(principal);
        SendMessageRequestDTO request = SendMessageRequestDTO.builder()
                .messageType(payload.getMessageType())
                .messageText(payload.getMessageText())
                .build();

        MessageResponseDTO message = chatService.sendRoomMessageAsUser(payload.getRoomId(), request, senderId);
        ChatRoomResponseDTO room = chatService.getRoomForUser(message.getRoomId(), senderId);
        ChatSocketEventDTO event = buildEvent("MESSAGE_CREATED", room, message);

        messagingTemplate.convertAndSend("/topic/chat-rooms/" + message.getRoomId(), event);
        chatService.getRoomMemberIds(message.getRoomId())
                .forEach(memberId -> {
                    ChatRoomResponseDTO memberRoom = chatService.getRoomForUser(message.getRoomId(), memberId);
                    ChatSocketEventDTO memberEvent = buildEvent("MESSAGE_CREATED", memberRoom, message);
                    messagingTemplate.convertAndSendToUser(
                            chatService.getWebSocketUsername(memberId),
                            "/queue/chat-rooms",
                            memberEvent
                    );
                });
    }


    private ChatSocketEventDTO buildEvent(String type, ChatRoomResponseDTO room, MessageResponseDTO message) {
        return ChatSocketEventDTO.builder()
                .type(type)
                .roomId(message.getRoomId())
                .room(room)
                .message(message)
                .build();
    }

    private Long getUserId(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof UserEntity user) {
            return user.getId();
        }
        throw new BusinessException("WEBSOCKET_UNAUTHORIZED", "Websocket chua xac thuc", HttpStatus.UNAUTHORIZED);
    }

}
