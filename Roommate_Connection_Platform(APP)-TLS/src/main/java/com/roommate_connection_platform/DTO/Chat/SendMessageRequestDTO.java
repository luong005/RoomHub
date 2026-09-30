package com.roommate_connection_platform.DTO.Chat;

import com.roommate_connection_platform.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SendMessageRequestDTO {
    @NotBlank(message = "Noi dung tin nhan khong duoc de trong")
    @Size(max = 5000, message = "Noi dung tin nhan toi da 5000 ky tu")
    private String messageText;

    private MessageType messageType = MessageType.TEXT;
}
