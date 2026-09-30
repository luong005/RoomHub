package com.roommate_connection_platform.repositories;

import com.roommate_connection_platform.models.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity,Long> {
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    @EntityGraph(attributePaths = {"role", "profile"})
    Page<UserEntity> findByRole_Name(String roleName, Pageable pageable);

//    @Query("""
//            SELECT u
//            FROM UserEntity u
//            WHERE (:hometown IS NULL OR LOWER(u.hometown) LIKE LOWER(CONCAT('%', :hometown, '%')))
//              AND (:roleId IS NULL OR u.role.id = :roleId)
//            """)
//    List<UserEntity> searchUsers(
//            @Param("hometown") String hometown,
//            @Param("roleId") Long roleId
//    );
}
