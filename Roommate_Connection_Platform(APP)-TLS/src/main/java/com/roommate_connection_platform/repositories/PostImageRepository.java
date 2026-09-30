package com.roommate_connection_platform.repositories;

import com.roommate_connection_platform.models.PostImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImageEntity, Long> {
}
