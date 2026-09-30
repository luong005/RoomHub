package com.roommate_connection_platform.DTO.Post;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePostDTO {
    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be greater than or equal to 0")
    private Double price;

    @NotNull(message = "Area is required")
    @PositiveOrZero(message = "Area must be greater than or equal to 0")
    private Double area;

    @NotNull(message = "Max members is required")
    @Min(value = 1, message = "Max members must be at least 1")
    private Integer maxMembers;

    @NotNull(message = "Current members is required")
    @Min(value = 0, message = "Current members must be at least 0")
    private Integer currentMembers;

    private String description;

    @NotBlank(message = "Address is required")
    private String address;

    private String gender;

    @Pattern(
            regexp = "^$|^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?([+-]\\d{2}:\\d{2}|Z)?$",
            message = "Scheduled time must be ISO date time"
    )
    private String scheduledAt;
}
