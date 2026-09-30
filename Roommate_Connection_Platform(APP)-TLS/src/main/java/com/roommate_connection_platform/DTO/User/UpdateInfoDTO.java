package com.roommate_connection_platform.DTO.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data //toString
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateInfoDTO {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "hometown is required")
    private String hometown;

    @NotNull(message = "Year of birth is required")
    private Long yearOfBirth;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Study is required")
    private String study;

    private String bio;

    @NotBlank(message = "Current current address is required")
    private String currentAddress;
}
