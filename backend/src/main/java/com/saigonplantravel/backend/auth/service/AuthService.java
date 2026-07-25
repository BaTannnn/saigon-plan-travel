package com.saigonplantravel.backend.auth.service;

import com.saigonplantravel.backend.auth.dto.LoginRequest;
import com.saigonplantravel.backend.auth.dto.RegisterRequest;
import com.saigonplantravel.backend.auth.dto.RegisterResponse;
import com.saigonplantravel.backend.auth.entity.UserAccount;
import com.saigonplantravel.backend.auth.exception.EmailAlreadyExistsException;
import com.saigonplantravel.backend.auth.exception.InvalidCredentialsException;
import com.saigonplantravel.backend.auth.repository.UserAccoutRepository;
import com.saigonplantravel.backend.auth.service.model.AuthenticationResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccoutRepository userAccoutRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccoutRepository userAccoutRepository, PasswordEncoder passwordEncoder) {
        this.userAccoutRepository = userAccoutRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @Transactional(readOnly = true)
    public AuthenticationResult authenticate(LoginRequest request){
        UserAccount userAccount = userAccoutRepository.findByEmail(request.email())
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
        if(userAccoutRepository.existsByEmail(registerRequest.email())){
            throw new EmailAlreadyExistsException();
        }
        String passwordHash = passwordEncoder.encode(registerRequest.password());
        UserAccount userAccount = new UserAccount(
                registerRequest.email(),
                passwordHash,
                registerRequest.displayName()
        );
        UserAccount savedUserAccount = userAccoutRepository.save(userAccount);
        return RegisterResponse.from(savedUserAccount);
    }
}
