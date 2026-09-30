package com.roommate_connection_platform.converter;


import com.roommate_connection_platform.DTO.Post.CreatePostDTO;
import com.roommate_connection_platform.DTO.Post.PostImageResponseDTO;
import com.roommate_connection_platform.DTO.Post.PostResponseDTO;
import com.roommate_connection_platform.DTO.Post.UpdatePostDTO;
import com.roommate_connection_platform.DTO.RoomConnection.Received_NotificationResponseDTO;
import com.roommate_connection_platform.DTO.RoomConnection.Sent_NotificationResponseDTO;
import com.roommate_connection_platform.DTO.Chat.ChatRoomResponseDTO;
import com.roommate_connection_platform.DTO.Chat.ChatUserResponseDTO;
import com.roommate_connection_platform.DTO.Chat.MessageResponseDTO;
import com.roommate_connection_platform.DTO.Admin.AdminUserResponseDTO;
import com.roommate_connection_platform.DTO.User.UpdateInfoDTO;
import com.roommate_connection_platform.DTO.User.UserInfoResponseDTO;
import com.roommate_connection_platform.models.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Map;


@Mapper(componentModel = "spring")
public interface MapStruct {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateUserProfileEntity(UpdateInfoDTO updateInfoDTO, @MappingTarget UserProfileEntity userProfileEntity);


    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "googleAccountId", source = "user.googleAccountId")
    @Mapping(target = "roleId", source = "user.role.id")
    UserInfoResponseDTO toUserInfoResponseDTO(UserProfileEntity userProfileEntity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "connections", ignore = true)
    @Mapping(target = "scheduledAt", ignore = true)
    @Mapping(target = "status", constant = "OPEN")
    PostEntity toPostEntity(CreatePostDTO dto);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "authorName", source = "user.profile.fullName")
    @Mapping(target = "images", ignore = true)
    PostResponseDTO toPostResponseDto(PostEntity post);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "connections", ignore = true)
    void updatePostEntity(UpdatePostDTO dto, @MappingTarget PostEntity post);

    PostImageResponseDTO toPostImageResponseDto(PostImageEntity postImageEntity);

    @Mapping(target = "postId", source = "roomConnection.post.id")
    @Mapping(target = "roomConnectionId", source = "roomConnection.id")
    @Mapping(target = "receiverId", source = "recipient.id")
    @Mapping(target = "receiverName", source = "recipient.profile.fullName")
    Sent_NotificationResponseDTO toSentNotificationResponseDTO(NotificationEntity notificationEntity);

    @Mapping(target = "postId", source = "roomConnection.post.id" )
    @Mapping(target = "roomConnectionId", source = "roomConnection.id" )
    @Mapping(target = "senderId", source = "actor.id")
    @Mapping(target = "senderName", source = "actor.profile.fullName")
    Received_NotificationResponseDTO toReceivedNotificationResponseDto(NotificationEntity notificationEntity);

    @Mapping(target = "fullName", source = "profile.fullName")
    ChatUserResponseDTO toChatUserResponseDTO(UserEntity user);

    @Mapping(target = "username", source = "profile.fullName")
    @Mapping(target = "role", source = "role.name")
    AdminUserResponseDTO toAdminUserResponseDTO(UserEntity user);

    @Mapping(target = "roomId", source = "chatRoom.id")
    MessageResponseDTO toMessageResponseDTO(MessageEntity message);

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "blockedById", source = "blockedBy.id")
    @Mapping(target = "otherUser", ignore = true)
    @Mapping(target = "lastMessage", ignore = true)
    @Mapping(target = "unreadCount", ignore = true)
    ChatRoomResponseDTO toChatRoomResponseDTO(ChatRoomEntity chatRoom);
}
