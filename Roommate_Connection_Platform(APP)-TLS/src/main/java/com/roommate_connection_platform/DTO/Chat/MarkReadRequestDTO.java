package com.roommate_connection_platform.DTO.Chat;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkReadRequestDTO {
    @NotNull(message = "lastReadMessageId khong duoc de trong")
    private Long lastReadMessageId;
}
