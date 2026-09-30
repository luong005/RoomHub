package com.roommate_connection_platform.models;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;


    @Column(name = "public_id")
    private String publicId;

    @Column(name = "display_order")
    private Integer displayOrder;

    // Quan hệ N-1: Nhiều ảnh thuộc về 1 Post
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;
}
