package com.roommate_connection_platform.configurations;

import com.roommate_connection_platform.filters.JwtTokenFilter;
import com.roommate_connection_platform.models.RoleEntity;
import com.roommate_connection_platform.ratelimit.LoginRateLimitFilter;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
//@EnableMethodSecurity
@EnableWebSecurity
@EnableWebMvc
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;

    @Bean
    public FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilterRegistration() {
        FilterRegistrationBean<LoginRateLimitFilter> registration = new FilterRegistrationBean<>(loginRateLimitFilter);
        registration.setEnabled(false); // Registered only in the Spring Security chain.
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))// Phải disable CSRF để test POST từ Postman dễ hơn
                .addFilterBefore((Filter) jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loginRateLimitFilter, JwtTokenFilter.class)
                .authorizeHttpRequests(auth -> auth
                        //.requestMatchers("/api/profile", "/api/update-password", "/api/update-info").hasAnyRole(RoleEntity.ADMIN, RoleEntity.USER)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole(RoleEntity.ADMIN)
                        .requestMatchers(
                                "/api/login",
                                "/api/refresh",
                                "/api/logout",
                                "/api/register",
                                "/ws",
                                "/ws/**")
                        .permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }

//    @Bean
//    public CorsFilter corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//
//        // Domain của frontend gọi đến
//        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:5500"));
//
//        // Các HTTP method được phép
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//
//        // Các header được phép gửi lên (Quan trọng: phải có Authorization nếu dùng JWT)
//        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
//
//        // Header được phép trả về cho trình duyệt đọc (ví dụ nếu bạn trả JWT qua header)
//        configuration.setExposedHeaders(List.of("Authorization"));
//
//        configuration.setAllowCredentials(true); // Cho phép đính kèm cookie hoặc thông tin xác thực
//
//        // Áp dụng cấu hình này cho tất cả các endpoint (/**)
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//        return new CorsFilter(source);
//    }
}
