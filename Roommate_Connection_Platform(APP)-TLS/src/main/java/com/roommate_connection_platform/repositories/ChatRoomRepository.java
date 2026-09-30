package com.roommate_connection_platform.repositories;

import com.roommate_connection_platform.enums.ChatRoomStatus;
import com.roommate_connection_platform.enums.ChatRoomType;
import com.roommate_connection_platform.models.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {
    Optional<ChatRoomEntity> findByDirectKey(String directKey);

    @Query("""
            select distinct c from ChatRoomEntity c
            join c.userChatRooms uc
            where uc.user.id = :userId
                and c.type = :type
                and c.status = :status
            order by c.updatedAt desc
            """)
    List<ChatRoomEntity> findRoomsByUserAndStatus(
            @Param("userId") Long userId,
            @Param("type") ChatRoomType type,
            @Param("status") ChatRoomStatus status
    );

    @Query("""
            select distinct c from ChatRoomEntity c
            join c.userChatRooms uc
            where uc.user.id = :userId
                and c.type = :type
                and c.status = :status
                and c.createdBy.id <> :userId
            order by c.updatedAt desc
            """)
    List<ChatRoomEntity> findIncomingPendingRooms(
            @Param("userId") Long userId,
            @Param("type") ChatRoomType type,
            @Param("status") ChatRoomStatus status
    );
}
