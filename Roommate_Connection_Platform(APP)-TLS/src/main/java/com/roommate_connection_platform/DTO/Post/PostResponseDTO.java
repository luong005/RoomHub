package com.roommate_connection_platform.DTO.Post;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PostResponseDTO {
    private Long id;
    private Long roomConnectionId;
    private String title;
    private Double price;
    private Double area;
    private Integer maxMembers;
    private String status;
    private String description;
    private String address;
    private Integer currentMembers;
    private String gender;
    private Long userId;
    private String authorName;
    private List<PostImageResponseDTO> images;
}
