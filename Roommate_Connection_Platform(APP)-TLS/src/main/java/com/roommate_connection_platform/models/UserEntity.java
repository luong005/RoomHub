package com.roommate_connection_platform.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users")
@Data // to string
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEntity extends BaseEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", length = 50, nullable = false)
    private String phoneNumber;

    @Column(name = "password", length = 200, nullable = false)
    private String password;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "google_account_id")
    private int googleAccountId;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private RoleEntity role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserChatRoomEntity> userChatRooms;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageEntity> message;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfileEntity profile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<PostEntity> postEntities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
        authorityList.add(new SimpleGrantedAuthority("ROLE_"+this.getRole().getName().toUpperCase()));
//        authorityList.add(new SimpleGrantedAuthority("ROLE_USER"));
        return authorityList;
    }

    @Override
    public String getUsername() {
        return this.getPhoneNumber();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}