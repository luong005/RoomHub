package com.roommate_connection_platform.models;

import com.roommate_connection_platform.enums.ChatRoomStatus;
import com.roommate_connection_platform.enums.ChatRoomType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
        name = "chat_rooms",
        indexes = {
                @Index(name = "idx_chat_rooms_direct_key", columnList = "direct_key"),
                @Index(name = "idx_chat_rooms_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_rooms_direct_key", columnNames = "direct_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomType type = ChatRoomType.DIRECT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomStatus status = ChatRoomStatus.PENDING;

    @Column(name = "direct_key", length = 50)
    private String directKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private UserEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_by_id")
    private UserEntity blockedBy;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserChatRoomEntity> userChatRooms;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageEntity> messages;
}
