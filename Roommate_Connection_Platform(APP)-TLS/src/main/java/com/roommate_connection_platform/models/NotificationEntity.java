package com.roommate_connection_platform.models;

import com.roommate_connection_platform.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserEntity recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private UserEntity actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_connection_id")
    private RoomConnectionEntity roomConnection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(length = 255)
    private String message;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
