package com.roommate_connection_platform.repositories;

import com.roommate_connection_platform.enums.MessageStatus;
import com.roommate_connection_platform.models.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    Page<MessageEntity> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    Optional<MessageEntity> findFirstByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    @Query("""
            select count(m) from MessageEntity m
            where m.chatRoom.id = :roomId
                and m.sender.id <> :userId
                and m.status = :status
                and (:lastReadMessageId is null or m.id > :lastReadMessageId)
            """)
    Long countUnreadMessages(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("lastReadMessageId") Long lastReadMessageId,
            @Param("status") MessageStatus status
    );
}
