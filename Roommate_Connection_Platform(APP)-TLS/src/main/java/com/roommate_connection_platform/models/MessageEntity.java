package com.roommate_connection_platform.models;

import com.roommate_connection_platform.enums.MessageStatus;
import com.roommate_connection_platform.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_room_created", columnList = "room_id, created_at"),
                @Index(name = "idx_chat_messages_sender", columnList = "sender_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String messageText;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType = MessageType.TEXT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status = MessageStatus.SENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoomEntity chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;
}
