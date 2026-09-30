package com.roommate_connection_platform.services;

import com.roommate_connection_platform.DTO.Chat.ChatRoomResponseDTO;
import com.roommate_connection_platform.DTO.Chat.MarkReadRequestDTO;
import com.roommate_connection_platform.DTO.Chat.MessageResponseDTO;
import com.roommate_connection_platform.DTO.Chat.SendMessageRequestDTO;

import java.util.List;

public interface IChatService {
    List<ChatRoomResponseDTO> getInboxRooms();

    List<ChatRoomResponseDTO> getMessageRequests();

    List<ChatRoomResponseDTO> getBlockedRooms();

    List<MessageResponseDTO> getMessages(Long roomId, int page, int size);

    MessageResponseDTO sendDirectMessageAsUser(Long receiverId, SendMessageRequestDTO request, Long senderId);

    MessageResponseDTO sendRoomMessageAsUser(Long roomId, SendMessageRequestDTO request, Long senderId);

    ChatRoomResponseDTO getRoomForUser(Long roomId, Long userId);

    List<Long> getRoomMemberIds(Long roomId);

    String getWebSocketUsername(Long userId);

    ChatRoomResponseDTO acceptRoom(Long roomId);

    ChatRoomResponseDTO blockRoom(Long roomId);

    ChatRoomResponseDTO markAsRead(Long roomId, MarkReadRequestDTO request);
}
