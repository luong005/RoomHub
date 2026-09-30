package com.roommate_connection_platform.configurations;

import com.roommate_connection_platform.components.JwtTokenUtil;
import com.roommate_connection_platform.models.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtStompChannelInterceptor implements ChannelInterceptor {
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        }

        if ((StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand()))
                && accessor.getUser() == null) {
            throw new AccessDeniedException("Unauthorized websocket request");
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String token = extractBearerToken(accessor.getNativeHeader("Authorization"));
        if (token == null) {
            throw new AccessDeniedException("Missing websocket token");
        }

        String phoneNumber = jwtTokenUtil.extractPhoneNumber(token);
        UserEntity userDetails = (UserEntity) userDetailsService.loadUserByUsername(phoneNumber);
        if (!jwtTokenUtil.validateToken(token, userDetails)) {
            throw new AccessDeniedException("Invalid websocket token");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        accessor.setUser(authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractBearerToken(List<String> authorizationHeaders) {
        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            return null;
        }

        String authHeader = authorizationHeaders.get(0);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
