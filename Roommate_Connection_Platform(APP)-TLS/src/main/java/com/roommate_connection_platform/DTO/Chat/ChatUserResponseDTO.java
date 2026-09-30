package com.roommate_connection_platform.DTO.Chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatUserResponseDTO {
    private Long id;
    private String phoneNumber;
    private String fullName;
}
