package com.roommate_connection_platform.DTO.RoomConnection;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendRequestDTO {
    @NotNull(message = "postId is required")
    private Long postId;

    private String message;
}
