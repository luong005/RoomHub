package com.roommate_connection_platform.repositories;

import com.roommate_connection_platform.models.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
    @Query("select p.id from PostEntity p where p.user.id = :userId")
    List<Long> findPostIdsByUserId(@Param("userId") Long userId);

    @Query("""
            select distinct p from PostEntity p
            join fetch p.user u
            left join fetch u.profile
            left join fetch p.images
            where u.id = :userId
                and p.status = :status
            order by p.createdAt desc
            """)
    List<PostEntity> findByUserIdAndStatusWithImages(
            @Param("userId") Long userId,
            @Param("status") String status
    );

    @Query("""
            select distinct p from PostEntity p
            join fetch p.user u
            left join fetch u.profile
            left join fetch p.images
            where p.status = :status
                and (:address is null or lower(p.address) like lower(concat('%', :address, '%')))
                and (:authorId is null or u.id = :authorId)
                and (:priceFrom is null or p.price >= :priceFrom)
                and (:priceTo is null or p.price <= :priceTo)
                and (:currentMember is null or p.currentMembers = :currentMember)
                and (:area is null or p.area = :area)
                and (:gender is null or lower(p.gender) = lower(:gender))
            order by p.createdAt desc
            """)
    List<PostEntity> findPostsByRequest(
            @Param("status") String status,
            @Param("address") String address,
            @Param("authorId") Long authorId,
            @Param("priceFrom") Double priceFrom,
            @Param("priceTo") Double priceTo,
            @Param("currentMember") Integer currentMember,
            @Param("area") Double area,
            @Param("gender") String gender
    );

    @Modifying
    @Query("update PostEntity p set p.status = 'OPEN' where p.status = 'SCHEDULED' and p.scheduledAt <= :now")
    int publishScheduledPosts(@Param("now") LocalDateTime now);
}
