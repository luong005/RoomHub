package com.roommate_connection_platform.models;

import com.roommate_connection_platform.enums.ChatRoomRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_chatroom",
        indexes = {
                @Index(name = "idx_user_chatroom_user", columnList = "user_id"),
                @Index(name = "idx_user_chatroom_chatroom", columnList = "chatroom_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_chatroom_user_room", columnNames = {"user_id", "chatroom_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChatRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomRole role = ChatRoomRole.MEMBER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private MessageEntity lastReadMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatroom_id", nullable = false)
    private ChatRoomEntity chatRoom;
}
