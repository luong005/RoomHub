package com.roommate_connection_platform.DTO.User;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;

@Data //toString
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoResponseDTO {
    //id
    @JsonProperty("fullname")
    private String fullName;

    @JsonProperty("phone_number")
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private String hometown;

    private String currentAddress;

    private Long yearOfBirth;

    private String study;

    private String bio;

    private String gender;

    @JsonProperty("google_account_id")
    private int googleAccountId;

    @NotNull(message = "Role id is required")
    @JsonProperty("role_id")
    private Long roleId;
}
