package com.roommate_connection_platform.repositories;
import com.roommate_connection_platform.enums.RoomConnectionStatus;
import com.roommate_connection_platform.models.RoomConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface RoomConnectionReopsitory extends JpaRepository<RoomConnectionEntity, Long> {
    @Query("""
            select distinct rc from RoomConnectionEntity rc
            join fetch rc.post p
            join fetch p.user u
            left join fetch u.profile
            left join fetch p.images
            where rc.sender.id = :senderId
                and rc.status = :status
            order by p.createdAt desc
            """)
    List<RoomConnectionEntity> findJoinedRoomConnectionsBySenderId(
            @Param("senderId") Long senderId,
            @Param("status") RoomConnectionStatus status
    );

    @Query("""
            select rc from RoomConnectionEntity rc
            join fetch rc.sender s
            left join fetch s.profile
            where rc.post.id = :postId
                and rc.status = :status
            order by rc.createdAt asc
            """)
    List<RoomConnectionEntity> findByPostIdAndStatus(
            @Param("postId") Long postId,
            @Param("status") RoomConnectionStatus status
    );

}
