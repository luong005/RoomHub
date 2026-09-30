package com.roommate_connection_platform.repositories;

import com.roommate_connection_platform.models.UserChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserChatRoomRepository extends JpaRepository<UserChatRoomEntity, Long> {
    boolean existsByUserIdAndChatRoomId(Long userId, Long chatRoomId);

    Optional<UserChatRoomEntity> findByUserIdAndChatRoomId(Long userId, Long chatRoomId);

    List<UserChatRoomEntity> findByChatRoomId(Long chatRoomId);
}
