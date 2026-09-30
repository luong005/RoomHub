package com.roommate_connection_platform.DTO.User;

import lombok.*;

@Data //toString
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRequestDTO {
    private String currentAddress;
    private Long role_id;
}
