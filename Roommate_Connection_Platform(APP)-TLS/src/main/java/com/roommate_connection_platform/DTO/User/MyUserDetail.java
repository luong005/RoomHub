package com.roommate_connection_platform.DTO.User;

import com.roommate_connection_platform.models.UserEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.List;


@Getter
public class MyUserDetail implements UserDetails {

    private final Long id;
    private final String phoneNumber;
    private final String password;
    private String hometown;
    private final Collection<? extends GrantedAuthority> authorities;

    public MyUserDetail(UserEntity user) {
        this.id = user.getId();
        this.phoneNumber = user.getPhoneNumber();
        this.password = user.getPassword();
        this.authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().getName())
        );
    }

    @Override
    public String getUsername() {
        return "";
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
