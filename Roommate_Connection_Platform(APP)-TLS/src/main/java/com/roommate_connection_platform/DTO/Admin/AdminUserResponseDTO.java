package com.roommate_connection_platform.DTO.Admin;

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
public class AdminUserResponseDTO {
    private Long id;
    private String username;
    private String phoneNumber;
    private Boolean active;
    private String role;
}
