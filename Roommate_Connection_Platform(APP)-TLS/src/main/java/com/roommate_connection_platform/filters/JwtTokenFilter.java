package com.roommate_connection_platform.filters;

import com.roommate_connection_platform.components.JwtTokenUtil;
import com.roommate_connection_platform.models.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    @Value("${api.prefix}")
    private String apiPrefix;

    private final UserDetailsService userDetailsService;
    private final JwtTokenUtil jwtTokenUtil;

    // ham xac thuc token (ktra token het han)
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if(isBypassToken(request)) {
                filterChain.doFilter(request, response); //enable bypass
                return;
            }

            // lay token nam o cookies
            // final String token = getJwtFromCookie(request);  //request.getHeader("Authorization");

            //  lay token nam o muc author
            final String authHeader = request.getHeader("Authorization");

            // neu ko co token thi ko dc phan quyen
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return;
            }
            // lay token tu ki thu 7
            final String token = authHeader.substring(7);


            // lay sdt
            final String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);

            // ktra neu chua dc xac thuc thi tim kiem user, ktra token
            if (phoneNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserEntity userDetails = (UserEntity) userDetailsService.loadUserByUsername(phoneNumber);

                // ktra token con han ko
                if(jwtTokenUtil.validateToken(token, userDetails)) {

                    // xac thuc user
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, // principal
                                    null, // credentials
                                    userDetails.getAuthorities()); // lay role cua user

                    // Lưu thông tin thiết bị/địa chỉ IP của request vào chứng chỉ
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Đưa chứng chỉ vào Context của Spring để các API sau biết là đã login
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
            filterChain.doFilter(request, response); //enable bypass
        }catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        }
    }

    // ktra co can token hay ko
    private boolean isBypassToken(@NonNull  HttpServletRequest request) {
        // 1. Nếu là OPTIONS, cho qua luôn để trình duyệt hoàn tất bước Preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        final List<Pair<String, String>> bypassTokens = Arrays.asList(
                Pair.of(String.format("%s/refresh", apiPrefix), "POST"),
                Pair.of(String.format("%s/logout", apiPrefix), "POST"),
                Pair.of(String.format("%s/register", apiPrefix), "POST"),
                Pair.of(String.format("%s/login", apiPrefix), "POST")
        );
        for(Pair<String, String> bypassToken: bypassTokens) {
            // ktra url request chua url trong list va dung method ko
            if (request.getServletPath().contains(bypassToken.getFirst()) &&
                    request.getMethod().equals(bypassToken.getSecond())) {
                return true;
            }
        }
        return false;
    }

    public String getJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) { // Tên phải khớp với lúc bạn set ở Controller
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
