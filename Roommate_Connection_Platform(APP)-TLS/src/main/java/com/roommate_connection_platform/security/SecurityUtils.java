package com.roommate_connection_platform.security;

import com.roommate_connection_platform.DTO.User.MyUserDetail;
import com.roommate_connection_platform.models.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SecurityUtils {

    // Trả về user đang login
    public static MyUserDetail getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        UserEntity userEntity = (UserEntity) authentication.getPrincipal();
        return new MyUserDetail(userEntity);
    }

    // Lấy danh sách các role/authority dưới dạng String
    public static List<String> getAuthorities() {
        List<String> results = new ArrayList<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getAuthorities() != null) {
            // Duyệt trực tiếp qua Collection của Spring Security, không cần ép kiểu List
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                results.add(authority.getAuthority());
            }
        }
        return results;
    }
}
