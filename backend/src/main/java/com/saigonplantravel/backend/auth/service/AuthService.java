package com.saigonplantravel.backend.auth.service;

import com.saigonplantravel.backend.auth.dto.LoginRequest;
import com.saigonplantravel.backend.auth.dto.LoginResponse;
import com.saigonplantravel.backend.auth.dto.RegisterRequest;
import com.saigonplantravel.backend.auth.dto.RegisterResponse;
import com.saigonplantravel.backend.auth.entity.UserAccount;
import com.saigonplantravel.backend.auth.exception.EmailAlreadyExistsException;
import com.saigonplantravel.backend.auth.exception.InvalidCredentialsException;
import com.saigonplantravel.backend.auth.repository.UserAccountRepository;
import com.saigonplantravel.backend.auth.service.model.AuthenticationResult;
import com.saigonplantravel.backend.common.security.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    private AuthenticationResult authenticate(LoginRequest request){
        UserAccount userAccount = userAccountRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                userAccount.getPasswordHash());
        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        if (!Boolean.TRUE.equals(userAccount.getActive())) {
            throw new InvalidCredentialsException();
        }

        return AuthenticationResult.from(userAccount);
    }

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest){
        if(userAccountRepository.existsByEmail(registerRequest.email())){
            throw new EmailAlreadyExistsException();
        }
        String passwordHash = passwordEncoder.encode(registerRequest.password());
        UserAccount userAccount = new UserAccount(
                registerRequest.email(),
                passwordHash,
                registerRequest.displayName()
        );
        UserAccount savedUserAccount = userAccountRepository.save(userAccount);
        return RegisterResponse.from(savedUserAccount);
    }
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request){
        AuthenticationResult authenticationResult = authenticate(request);
        String accesToken = jwtService.generateAccessToken(authenticationResult);
        return new LoginResponse(
                accesToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                authenticationResult.publicId(),
                authenticationResult.email(),
                authenticationResult.displayName(),
                authenticationResult.role()
        );
    }
}
