package com.roommate_connection_platform.ratelimit;

import com.roommate_connection_platform.models.UserEntity;
import com.roommate_connection_platform.repositories.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginIdentityResolverTest {
    private final UserRepository users = mock(UserRepository.class);
    private final LoginIdentityResolver resolver = new LoginIdentityResolver(users);

    @Test
    void sameDatabaseAccountHasOneIdentityRegardlessOfInputSpelling() {
        UserEntity user = new UserEntity();
        user.setId(42L);
        when(users.findByPhoneNumber("Phone")).thenReturn(Optional.of(user));
        when(users.findByPhoneNumber("PHONE")).thenReturn(Optional.of(user));
        assertThat(resolver.resolve("Phone")).isEqualTo(resolver.resolve("PHONE"));
    }

    @Test
    void unknownIdentitiesAreBoundedAndWhitespaceVariantsShareACounter() {
        when(users.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        assertThat(resolver.resolve(" unknown ")).isEqualTo(resolver.resolve("UNKNOWN"));
        assertThat(resolver.resolve("x".repeat(10_000))).hasSize(70);
        assertThat(resolver.resolve("phone1")).isNotEqualTo(resolver.resolve("phone2"));
    }
}
