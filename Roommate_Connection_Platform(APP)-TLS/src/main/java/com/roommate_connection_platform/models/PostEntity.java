package com.roommate_connection_platform.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "posts")
@Data // to string
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private Double price;
    private Double area;
    private Integer maxMembers;
    private String status; // 'OPEN', 'FULL'
    private String description;
    private String address;
    private Integer currentMembers;
    private String gender;
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    // Quan hệ N-1: Nhiều bài đăng thuộc về 1 User (Người đăng bài)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // Quan hệ 1-N với PostImage
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImageEntity> images;

    // Quan hệ 1-N với PostConnection
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomConnectionEntity> connections;
}
