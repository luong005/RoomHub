package com.roommate_connection_platform.DTO.Chat;

import com.roommate_connection_platform.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomChatSocketMessageDTO {
    @NotNull(message = "roomId khong duoc de trong")
    private Long roomId;

    @NotBlank(message = "Noi dung tin nhan khong duoc de trong")
    @Size(max = 5000, message = "Noi dung tin nhan toi da 5000 ky tu")
    private String messageText;

    private MessageType messageType = MessageType.TEXT;
}
