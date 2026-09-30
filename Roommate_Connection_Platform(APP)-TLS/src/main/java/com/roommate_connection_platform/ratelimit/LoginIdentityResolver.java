package com.roommate_connection_platform.ratelimit;

import com.roommate_connection_platform.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class LoginIdentityResolver {
    private final UserRepository userRepository;

    public String resolve(String phoneNumber) {
        // Reuse the login lookup: MySQL may equate case/accent variants of the same value.
        // The identifier exists only in RAM; security_events has no user foreign key.
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(user -> "account:" + user.getId())
                .orElseGet(() -> "phone:" + digest(phoneNumber.strip().toLowerCase(Locale.ROOT)));
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
