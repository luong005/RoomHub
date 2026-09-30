package com.roommate_connection_platform.repositories;

import com.roommate_connection_platform.models.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByActor_Id(Long id);
    List<NotificationEntity> findByRecipient_Id(Long id);
    void deleteByRoomConnection_Id(Long roomConnectionId);
}
