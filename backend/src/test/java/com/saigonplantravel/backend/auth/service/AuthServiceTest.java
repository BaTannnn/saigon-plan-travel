package com.saigonplantravel.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.dto.LoginRequest;
import com.saigonplantravel.backend.auth.dto.LoginResponse;
import com.saigonplantravel.backend.auth.dto.RegisterRequest;
import com.saigonplantravel.backend.auth.dto.RegisterResponse;
import com.saigonplantravel.backend.auth.entity.UserAccount;
import com.saigonplantravel.backend.auth.exception.EmailAlreadyExistsException;
import com.saigonplantravel.backend.auth.exception.InvalidCredentialsException;
import com.saigonplantravel.backend.auth.repository.UserAccountRepository;
import com.saigonplantravel.backend.auth.security.jwt.JwtService;
import com.saigonplantravel.backend.auth.service.model.AuthenticationResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userAccountRepository, passwordEncoder, jwtService);
    }

    @Test
    void registersUserWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest("  Traveler@Example.COM  ", "raw-password", "  Lan Anh  ");
        when(userAccountRepository.existsByEmail("traveler@example.com")).thenReturn(false);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userAccountRepository.save(org.mockito.ArgumentMatchers.any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterResponse response = authService.register(request);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());
        UserAccount savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password").isNotEqualTo("raw-password");
        assertThat(response.publicId()).isEqualTo(savedUser.getPublicId());
        assertThat(response.email()).isEqualTo("traveler@example.com");
        assertThat(response.displayName()).isEqualTo("Lan Anh");
        assertThat(response.role()).isEqualTo(UserRole.USER);
    }

    @Test
    void rejectsRegistrationWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "raw-password", "Existing User");
        when(userAccountRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(EmailAlreadyExistsException.class);

        verifyNoInteractions(passwordEncoder, jwtService);
        verify(userAccountRepository, never()).save(org.mockito.ArgumentMatchers.any(UserAccount.class));
    }

    @Test
    void logsInActiveUserWithValidCredentials() {
        UserAccount user = new UserAccount("traveler@example.com", "encoded-password", "Lan Anh");
        LoginRequest request = new LoginRequest("  Traveler@Example.COM  ", "raw-password");
        when(userAccountRepository.findByEmail("traveler@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-password", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(org.mockito.ArgumentMatchers.any(AuthenticationResult.class)))
                .thenReturn("generated-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(request);

        ArgumentCaptor<AuthenticationResult> authenticationCaptor = ArgumentCaptor.forClass(AuthenticationResult.class);
        verify(jwtService).generateAccessToken(authenticationCaptor.capture());
        assertThat(authenticationCaptor.getValue())
                .isEqualTo(
                        new AuthenticationResult(user.getPublicId(), "traveler@example.com", "Lan Anh", UserRole.USER));
        assertThat(response)
                .isEqualTo(new LoginResponse(
                        "generated-token",
                        "Bearer",
                        3600L,
                        user.getPublicId(),
                        "traveler@example.com",
                        "Lan Anh",
                        UserRole.USER));
    }

    @Test
    void rejectsLoginWhenEmailDoesNotExist() {
        LoginRequest request = new LoginRequest("missing@example.com", "raw-password");
        when(userAccountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void rejectsLoginWhenPasswordDoesNotMatch() {
        UserAccount user = new UserAccount("traveler@example.com", "encoded-password", "Lan Anh");
        LoginRequest request = new LoginRequest("traveler@example.com", "wrong-password");
        when(userAccountRepository.findByEmail("traveler@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(org.mockito.ArgumentMatchers.any(AuthenticationResult.class));
    }

    @Test
    void rejectsLoginWhenUserIsInactive() {
        UserAccount user = new UserAccount("traveler@example.com", "encoded-password", "Lan Anh");
        user.deactivate();
        LoginRequest request = new LoginRequest("traveler@example.com", "raw-password");
        when(userAccountRepository.findByEmail("traveler@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw-password", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(org.mockito.ArgumentMatchers.any(AuthenticationResult.class));
    }
}
