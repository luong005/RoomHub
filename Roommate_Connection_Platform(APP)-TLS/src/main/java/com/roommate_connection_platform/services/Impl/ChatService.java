package com.roommate_connection_platform.services.Impl;

import com.roommate_connection_platform.DTO.Chat.ChatRoomResponseDTO;
import com.roommate_connection_platform.DTO.Chat.MarkReadRequestDTO;
import com.roommate_connection_platform.DTO.Chat.MessageResponseDTO;
import com.roommate_connection_platform.DTO.Chat.SendMessageRequestDTO;
import com.roommate_connection_platform.DTO.User.MyUserDetail;
import com.roommate_connection_platform.converter.MapStruct;
import com.roommate_connection_platform.customexceptions.BusinessException;
import com.roommate_connection_platform.enums.ChatRoomRole;
import com.roommate_connection_platform.enums.ChatRoomStatus;
import com.roommate_connection_platform.enums.ChatRoomType;
import com.roommate_connection_platform.enums.MessageStatus;
import com.roommate_connection_platform.enums.MessageType;
import com.roommate_connection_platform.models.ChatRoomEntity;
import com.roommate_connection_platform.models.MessageEntity;
import com.roommate_connection_platform.models.UserChatRoomEntity;
import com.roommate_connection_platform.models.UserEntity;
import com.roommate_connection_platform.repositories.ChatRoomRepository;
import com.roommate_connection_platform.repositories.MessageRepository;
import com.roommate_connection_platform.repositories.UserChatRoomRepository;
import com.roommate_connection_platform.repositories.UserRepository;
import com.roommate_connection_platform.security.SecurityUtils;
import com.roommate_connection_platform.services.IChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService implements IChatService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final UserChatRoomRepository userChatRoomRepository;
    private final UserRepository userRepository;
    private final MapStruct mapStruct;

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponseDTO> getInboxRooms() {
        Long userId = getCurrentUserId();
        return chatRoomRepository.findRoomsByUserAndStatus(userId, ChatRoomType.DIRECT, ChatRoomStatus.ACCEPTED)
                .stream()
                .map(room -> mapRoom(room, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponseDTO> getMessageRequests() {
        Long userId = getCurrentUserId();
        return chatRoomRepository.findIncomingPendingRooms(userId, ChatRoomType.DIRECT, ChatRoomStatus.PENDING)
                .stream()
                .map(room -> mapRoom(room, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomResponseDTO> getBlockedRooms() {
        Long userId = getCurrentUserId();
        return chatRoomRepository.findRoomsByUserAndStatus(userId, ChatRoomType.DIRECT, ChatRoomStatus.BLOCKED)
                .stream()
                .map(room -> mapRoom(room, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponseDTO> getMessages(Long roomId, int page, int size) {
        Long userId = getCurrentUserId();
        ensureUserInRoom(userId, roomId);

        int pageIndex = Math.max(page, 0);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return messageRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(pageIndex, pageSize))
                .stream()
                .map(mapStruct::toMessageResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public MessageResponseDTO sendDirectMessageAsUser(Long receiverId, SendMessageRequestDTO request, Long senderId) {
        UserEntity sender = getUser(senderId);
        if (sender.getId().equals(receiverId)) {
            throw new BusinessException("INVALID_RECEIVER", "Khong the gui tin nhan cho chinh minh", HttpStatus.BAD_REQUEST);
        }

        UserEntity receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Khong tim thay nguoi nhan", HttpStatus.NOT_FOUND));

        String directKey = buildDirectKey(sender.getId(), receiver.getId());
        ChatRoomEntity room = chatRoomRepository.findByDirectKey(directKey)
                .orElseGet(() -> createDirectRoom(sender, receiver, directKey));

        validateCanSendMessage(room, sender.getId());
        return mapStruct.toMessageResponseDTO(saveMessage(room, sender, request));
    }

    @Override
    @Transactional
    public MessageResponseDTO sendRoomMessageAsUser(Long roomId, SendMessageRequestDTO request, Long senderId) {
        UserEntity sender = getUser(senderId);
        ChatRoomEntity room = getRoomAndEnsureMember(roomId, sender.getId());

        validateCanSendMessage(room, sender.getId());
        return mapStruct.toMessageResponseDTO(saveMessage(room, sender, request));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomResponseDTO getRoomForUser(Long roomId, Long userId) {
        ChatRoomEntity room = getRoomAndEnsureMember(roomId, userId);
        return mapRoom(room, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getRoomMemberIds(Long roomId) {
        return userChatRoomRepository.findByChatRoomId(roomId)
                .stream()
                .map(userChatRoom -> userChatRoom.getUser().getId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String getWebSocketUsername(Long userId) {
        return getUser(userId).getUsername();
    }

    @Override
    @Transactional
    public ChatRoomResponseDTO acceptRoom(Long roomId) {
        Long userId = getCurrentUserId();
        ChatRoomEntity room = getRoomAndEnsureMember(roomId, userId);

        if (!ChatRoomStatus.PENDING.equals(room.getStatus())) {
            throw new BusinessException("CHAT_NOT_PENDING", "Phong chat khong o trang thai cho chap nhan", HttpStatus.BAD_REQUEST);
        }
        if (room.getCreatedBy() != null && room.getCreatedBy().getId().equals(userId)) {
            throw new BusinessException("ACCEPT_DENIED", "Nguoi gui loi moi khong the tu chap nhan", HttpStatus.FORBIDDEN);
        }

        room.setStatus(ChatRoomStatus.ACCEPTED);
        ChatRoomEntity savedRoom = chatRoomRepository.save(room);
        return mapRoom(savedRoom, userId);
    }

    @Override
    @Transactional
    public ChatRoomResponseDTO blockRoom(Long roomId) {
        UserEntity currentUser = getUser(getCurrentUserId());
        ChatRoomEntity room = getRoomAndEnsureMember(roomId, currentUser.getId());

        room.setStatus(ChatRoomStatus.BLOCKED);
        room.setBlockedBy(currentUser);
        ChatRoomEntity savedRoom = chatRoomRepository.save(room);
        return mapRoom(savedRoom, currentUser.getId());
    }

    @Override
    @Transactional
    public ChatRoomResponseDTO markAsRead(Long roomId, MarkReadRequestDTO request) {
        Long userId = getCurrentUserId();
        UserChatRoomEntity userChatRoom = userChatRoomRepository.findByUserIdAndChatRoomId(userId, roomId)
                .orElseThrow(() -> new BusinessException("CHAT_ROOM_FORBIDDEN", "Ban khong thuoc phong chat nay", HttpStatus.FORBIDDEN));

        MessageEntity message = messageRepository.findById(request.getLastReadMessageId())
                .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "Khong tim thay tin nhan", HttpStatus.NOT_FOUND));

        if (!message.getChatRoom().getId().equals(roomId)) {
            throw new BusinessException("INVALID_READ_MESSAGE", "Tin nhan khong thuoc phong chat nay", HttpStatus.BAD_REQUEST);
        }

        userChatRoom.setLastReadMessage(message);
        userChatRoomRepository.save(userChatRoom);
        return mapRoom(userChatRoom.getChatRoom(), userId);
    }

    private ChatRoomEntity createDirectRoom(UserEntity sender, UserEntity receiver, String directKey) {
        ChatRoomEntity room = ChatRoomEntity.builder()
                .type(ChatRoomType.DIRECT)
                .status(ChatRoomStatus.PENDING)
                .directKey(directKey)
                .createdBy(sender)
                .build();
        ChatRoomEntity savedRoom = chatRoomRepository.save(room);

        UserChatRoomEntity senderRoom = UserChatRoomEntity.builder()
                .user(sender)
                .chatRoom(savedRoom)
                .role(ChatRoomRole.MEMBER)
                .build();
        UserChatRoomEntity receiverRoom = UserChatRoomEntity.builder()
                .user(receiver)
                .chatRoom(savedRoom)
                .role(ChatRoomRole.MEMBER)
                .build();
        userChatRoomRepository.saveAll(List.of(senderRoom, receiverRoom));
        return savedRoom;
    }

    private MessageEntity saveMessage(ChatRoomEntity room, UserEntity sender, SendMessageRequestDTO request) {
        MessageType messageType = request.getMessageType() == null ? MessageType.TEXT : request.getMessageType();
        MessageEntity message = MessageEntity.builder()
                .chatRoom(room)
                .sender(sender)
                .messageText(request.getMessageText())
                .messageType(messageType)
                .status(MessageStatus.SENT)
                .build();
        return messageRepository.save(message);
    }

    private void validateCanSendMessage(ChatRoomEntity room, Long senderId) {
        if (ChatRoomStatus.BLOCKED.equals(room.getStatus())) {
            throw new BusinessException("CHAT_BLOCKED", "Phong chat da bi chan", HttpStatus.FORBIDDEN);
        }

        if (ChatRoomStatus.PENDING.equals(room.getStatus())
                && room.getCreatedBy() != null
                && !room.getCreatedBy().getId().equals(senderId)) {
            throw new BusinessException("CHAT_WAITING_ACCEPT", "Can chap nhan tin nhan cho truoc khi tra loi", HttpStatus.FORBIDDEN);
        }
    }

    private ChatRoomEntity getRoomAndEnsureMember(Long roomId, Long userId) {
        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException("CHAT_ROOM_NOT_FOUND", "Khong tim thay phong chat", HttpStatus.NOT_FOUND));
        if (!userChatRoomRepository.existsByUserIdAndChatRoomId(userId, roomId)) {
            throw new BusinessException("CHAT_ROOM_FORBIDDEN", "Ban khong thuoc phong chat nay", HttpStatus.FORBIDDEN);
        }
        return room;
    }

    private void ensureUserInRoom(Long userId, Long roomId) {
        if (!userChatRoomRepository.existsByUserIdAndChatRoomId(userId, roomId)) {
            throw new BusinessException("CHAT_ROOM_FORBIDDEN", "Ban khong thuoc phong chat nay", HttpStatus.FORBIDDEN);
        }
    }

    private Long getCurrentUserId() {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }
        return principal.getId();
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Khong tim thay nguoi dung", HttpStatus.NOT_FOUND));
    }

    private String buildDirectKey(Long userId1, Long userId2) {
        Long minId = Math.min(userId1, userId2);
        Long maxId = Math.max(userId1, userId2);
        return minId + "_" + maxId;
    }

    private ChatRoomResponseDTO mapRoom(ChatRoomEntity room, Long currentUserId) {
        UserChatRoomEntity currentUserRoom = room.getUserChatRooms()
                .stream()
                .filter(userRoom -> userRoom.getUser().getId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        Long lastReadMessageId = currentUserRoom == null || currentUserRoom.getLastReadMessage() == null
                ? null
                : currentUserRoom.getLastReadMessage().getId();

        MessageResponseDTO lastMessage = messageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(room.getId())
                .map(mapStruct::toMessageResponseDTO)
                .orElse(null);

        Long unreadCount = messageRepository.countUnreadMessages(
                room.getId(),
                currentUserId,
                lastReadMessageId,
                MessageStatus.SENT
        );

        ChatRoomResponseDTO response = mapStruct.toChatRoomResponseDTO(room);
        response.setOtherUser(findOtherUser(room, currentUserId));
        response.setLastMessage(lastMessage);
        response.setUnreadCount(unreadCount);
        return response;
    }

    private com.roommate_connection_platform.DTO.Chat.ChatUserResponseDTO findOtherUser(ChatRoomEntity room, Long currentUserId) {
        return room.getUserChatRooms()
                .stream()
                .map(UserChatRoomEntity::getUser)
                .filter(user -> !user.getId().equals(currentUserId))
                .min(java.util.Comparator.comparing(UserEntity::getId))
                .map(mapStruct::toChatUserResponseDTO)
                .orElse(null);
    }

}
