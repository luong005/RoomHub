package com.roommate_connection_platform.DTO.Post;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PostImageResponseDTO {
    private Long id;
    private String imageUrl;
    private String publicId;
    private Integer displayOrder;
}
