package com.roommate_connection_platform.services.Impl;

import com.roommate_connection_platform.DTO.RoomConnection.Received_NotificationResponseDTO;
import com.roommate_connection_platform.DTO.RoomConnection.Sent_NotificationResponseDTO;
import com.roommate_connection_platform.DTO.User.MyUserDetail;
import com.roommate_connection_platform.converter.MapStruct;
import com.roommate_connection_platform.customexceptions.BusinessException;
import com.roommate_connection_platform.enums.RoomConnectionStatus;
import com.roommate_connection_platform.models.NotificationEntity;
import com.roommate_connection_platform.models.PostEntity;
import com.roommate_connection_platform.models.RoomConnectionEntity;
import com.roommate_connection_platform.models.UserEntity;
import com.roommate_connection_platform.repositories.NotificationRepository;
import com.roommate_connection_platform.repositories.PostRepository;
import com.roommate_connection_platform.repositories.RoomConnectionReopsitory;
import com.roommate_connection_platform.repositories.UserRepository;
import com.roommate_connection_platform.security.SecurityUtils;
import com.roommate_connection_platform.services.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {
    private final NotificationRepository notificationRepository;
    private final MapStruct mapStruct;
    @Override

    public List<Sent_NotificationResponseDTO> getRoomConnectionBySenderId() {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }

        List<NotificationEntity> roomConnectionEntities = notificationRepository.findByActor_Id(principal.getId());
        return roomConnectionEntities.stream()
                .map(mapStruct::toSentNotificationResponseDTO)
                .toList();
    }

    @Override
    public List<Received_NotificationResponseDTO> getRoomConnectionByReceiverId() {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }

        List<NotificationEntity> roomConnectionEntities = notificationRepository.findByRecipient_Id(principal.getId());
        return roomConnectionEntities.stream()
                .map(mapStruct::toReceivedNotificationResponseDto)
                .toList();
    }
}
