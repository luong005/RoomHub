package com.roommate_connection_platform.repositories;

import com.roommate_connection_platform.models.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity,Long> {
    Optional<UserProfileEntity> findByUser_Id(Long userId);
}
