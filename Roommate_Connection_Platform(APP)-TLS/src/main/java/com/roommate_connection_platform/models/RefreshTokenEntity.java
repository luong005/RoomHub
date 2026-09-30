package com.roommate_connection_platform.models;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "refreshTokens")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenEntity extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", length = 255)
    private String token;

    @Column(name = "token_type", length = 50)
    private String tokenType;

    @Column(name = "expiration_date")
    private Date expirationDate;

    private boolean revoked;
    private boolean expired;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;
}
