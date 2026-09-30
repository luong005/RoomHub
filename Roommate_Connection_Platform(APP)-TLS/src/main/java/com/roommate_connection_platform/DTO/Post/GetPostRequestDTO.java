package com.roommate_connection_platform.DTO.Post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
public class GetPostRequestDTO {
    private String address;
    private Long authorId;
    private Double priceFrom;
    private Double priceTo;
    private Integer currentMember;
    private Double area;
    private String gender;
}
