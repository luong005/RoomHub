package com.roommate_connection_platform.services.Impl;

import com.roommate_connection_platform.DTO.Post.PostImageResponseDTO;
import com.roommate_connection_platform.DTO.Post.PostResponseDTO;
import com.roommate_connection_platform.DTO.RoomConnection.SendRequestDTO;
import com.roommate_connection_platform.DTO.RoomConnection.JoinedRoomMemberResponseDTO;
import com.roommate_connection_platform.DTO.RoomConnection.UpdateRoomConnectionRequest;
import com.roommate_connection_platform.DTO.User.MyUserDetail;
import com.roommate_connection_platform.converter.MapStruct;
import com.roommate_connection_platform.customexceptions.BusinessException;
import com.roommate_connection_platform.enums.NotificationType;
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
import com.roommate_connection_platform.services.IRoomConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomConnectionService implements IRoomConnectionService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RoomConnectionReopsitory roomConnectionReopsitory;
    private final NotificationRepository notificationRepository;
    private final MapStruct mapStruct;

    @Override
    public void createRoomConnection(SendRequestDTO sendRequestDTO) {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }

        PostEntity post = postRepository.findById(sendRequestDTO.getPostId())
                .orElseThrow(() -> new BusinessException("POST_NOT_FOUND", "Khong tim thay bai dang", HttpStatus.NOT_FOUND));
        if (post.getUser().getId().equals(principal.getId())) {
            throw new BusinessException("JOIN_ERROR", "Khong the tham gia phong cua minh");
        }

        UserEntity actor = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Khong tim thay nguoi dung", HttpStatus.NOT_FOUND));
        UserEntity recepient = userRepository.findById(post.getUser().getId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Khong tim thay nguoi dung", HttpStatus.NOT_FOUND));

        RoomConnectionEntity roomConnectionEntity = RoomConnectionEntity.builder()
                .message(sendRequestDTO.getMessage())
                .sender(actor)
                .status(RoomConnectionStatus.PENDING)
                .post(post)
                .build();

        NotificationEntity notificationEntity = NotificationEntity.builder()
                .read(false)
                .actor(actor)
                .recipient(recepient)
                .type(NotificationType.JOIN_REQUEST_SENT)
                .roomConnection(roomConnectionEntity)
                .build();
        roomConnectionReopsitory.save(roomConnectionEntity);
        notificationRepository.save(notificationEntity);
    }

    @Override
    public void deleteRoomConnection(Long id) {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }

        RoomConnectionEntity roomConnection = roomConnectionReopsitory.findById(id)
                .orElseThrow(() -> new BusinessException("REQUEST_NOT_FOUND", "Khong tim thay yeu cau da gui", HttpStatus.NOT_FOUND));

        if (!roomConnection.getSender().getId().equals(principal.getId()) || !roomConnection.getStatus().equals(RoomConnectionStatus.PENDING)) {
            throw new BusinessException("DELETE_ERROR", "Loi khong the xoa", HttpStatus.BAD_REQUEST);
        }

        roomConnectionReopsitory.deleteById(id);
    }

    @Override
    public void updateRequest(UpdateRoomConnectionRequest updateRoomConnectionRequest) {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }

        RoomConnectionEntity roomConnection = roomConnectionReopsitory.findById(updateRoomConnectionRequest.getRoomConnectionId())
                .orElseThrow(() -> new BusinessException("ROOM_ERRO", "Khong tim thay phong", HttpStatus.NOT_FOUND));


        NotificationEntity notificationEntity = NotificationEntity.builder()
                .read(false)
                .actor(roomConnection.getPost().getUser())
                .recipient(roomConnection.getSender())
                .roomConnection(roomConnection)
                .build();

        PostEntity postEntity = postRepository.findById(roomConnection.getPost().getId()).get();

        if (roomConnection.getPost().getUser().getId().equals(principal.getId())) {
            if (roomConnection.getStatus().equals(RoomConnectionStatus.PENDING)) {
                switch (updateRoomConnectionRequest.getStatus()) {
                    case JOINED -> {
                        roomConnection.setStatus(RoomConnectionStatus.JOINED);
                        notificationEntity.setType(NotificationType.JOIN_REQUEST_ACCEPTED);

                        postEntity.setCurrentMembers(postEntity.getCurrentMembers()+1);
                        postRepository.save(postEntity);
                    }
                    case REJECTED -> {
                        roomConnection.setStatus(RoomConnectionStatus.REJECTED);
                        notificationEntity.setType(NotificationType.JOIN_REQUEST_REJECTED);
                    }
                    default -> throw new BusinessException("UPDATE_ERROR", "Loi cap nhat", HttpStatus.BAD_REQUEST);
                }
                roomConnectionReopsitory.save(roomConnection);
                notificationRepository.save(notificationEntity);
                return;
            }
            if (roomConnection.getStatus().equals(RoomConnectionStatus.JOINED) && updateRoomConnectionRequest.getStatus().equals(RoomConnectionStatus.KICKED)) {
                roomConnection.setStatus(RoomConnectionStatus.KICKED);
                notificationEntity.setType(NotificationType.USER_KICKED);

                postEntity.setCurrentMembers(postEntity.getCurrentMembers()-1);
                postRepository.save(postEntity);

            }
            else throw new BusinessException("UPDATE_ERROR", "Loi cap nhat", HttpStatus.BAD_REQUEST);
        }

        if (roomConnection.getSender().getId().equals(principal.getId())) {
            if(roomConnection.getStatus().equals(RoomConnectionStatus.PENDING)) {
                roomConnection.setStatus(RoomConnectionStatus.CANCELLED);
                notificationEntity.setType(NotificationType.USER_CANCLLED);
            }
            if(roomConnection.getStatus().equals(RoomConnectionStatus.JOINED)){
                roomConnection.setStatus(RoomConnectionStatus.LEFT);
                notificationEntity.setType(NotificationType.USER_LEFT);

                postEntity.setCurrentMembers(postEntity.getCurrentMembers()-1);
                postRepository.save(postEntity);
            }
            else throw new BusinessException("UPDATE_ERROR", "Loi cap nhat", HttpStatus.BAD_REQUEST);

        }
        roomConnectionReopsitory.save(roomConnection);
        notificationRepository.save(notificationEntity);

    }

    @Override
    public List<PostResponseDTO> getUser_Joined() {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }

        return roomConnectionReopsitory.findJoinedRoomConnectionsBySenderId(principal.getId(), RoomConnectionStatus.JOINED)
                .stream()
                .map(this::toPostResponseDTO)
                .toList();
    }

    @Override
    public List<JoinedRoomMemberResponseDTO> getJoinedMembersByPostId(Long postId) {
        MyUserDetail principal = SecurityUtils.getPrincipal();
        if (principal == null) {
            throw new BusinessException("LOGIN_REQUIRED", "Yeu cau dang nhap", HttpStatus.UNAUTHORIZED);
        }

        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("POST_NOT_FOUND", "Khong tim thay bai dang", HttpStatus.NOT_FOUND));
        if (!post.getUser().getId().equals(principal.getId())) {
            throw new BusinessException("FORBIDDEN", "Ban khong phai chu bai dang", HttpStatus.FORBIDDEN);
        }

        return roomConnectionReopsitory.findByPostIdAndStatus(postId, RoomConnectionStatus.JOINED)
                .stream()
                .map(roomConnection -> new JoinedRoomMemberResponseDTO(
                        roomConnection.getId(),
                        roomConnection.getSender().getId(),
                        roomConnection.getSender().getProfile() == null
                                ? null
                                : roomConnection.getSender().getProfile().getFullName()
                ))
                .toList();
    }

    private PostResponseDTO toPostResponseDTO(RoomConnectionEntity roomConnection) {
        PostEntity post = roomConnection.getPost();
        PostResponseDTO postResponseDTO = mapStruct.toPostResponseDto(post);
        postResponseDTO.setRoomConnectionId(roomConnection.getId());
        List<PostImageResponseDTO> postImageResponseDTOS = post.getImages() == null
                ? List.of()
                : post.getImages().stream().map(mapStruct::toPostImageResponseDto).toList();
        postResponseDTO.setImages(postImageResponseDTOS);
        return postResponseDTO;
    }

}
