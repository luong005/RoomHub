package com.roommate_connection_platform.models;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfileEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private Long yearOfBirth;
    private String gender;
    private String study;
    private String currentAddress;
    private String hometown;

    @Column(columnDefinition = "TEXT")
    private String bio;

    // Quan hệ 1-1 với User
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private UserEntity user;
}
