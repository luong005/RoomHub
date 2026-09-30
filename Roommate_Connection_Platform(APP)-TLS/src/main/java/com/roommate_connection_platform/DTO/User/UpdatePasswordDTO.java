package com.roommate_connection_platform.DTO.User;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data //toString
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdatePasswordDTO {

    @JsonProperty("phone_number")
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "old password is required")
    private String oldPassword;

    @NotBlank(message = "new password is required")
    private String newPassword;
}
