package com.roommate_connection_platform.models;
import com.roommate_connection_platform.enums.RoomConnectionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
        name = "room_connection",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "sender_id"})
)
@Data // to string
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomConnectionEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomConnectionStatus status = RoomConnectionStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String message;

    // Quan hệ N-1: Nhiều kết nối trỏ về 1 Bài đăng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    // Quan hệ N-1: Nhiều kết nối do 1 User (Người đi xin) tạo ra
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @OneToMany(mappedBy = "roomConnection", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<NotificationEntity> notificationEntities;
}
