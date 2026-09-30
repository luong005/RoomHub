package com.roommate_connection_platform.repositories;

import com.roommate_connection_platform.models.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity,Long> {

    Optional<RefreshTokenEntity> findByToken(String token);
    void deleteByToken(String token);
}
