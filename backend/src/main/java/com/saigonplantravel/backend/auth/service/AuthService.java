package com.saigonplantravel.backend.auth.service;

import com.saigonplantravel.backend.auth.dto.RegisterRequest;
import com.saigonplantravel.backend.auth.dto.RegisterResponse;
import com.saigonplantravel.backend.auth.entity.UserAccount;
import com.saigonplantravel.backend.auth.exception.EmailAlreadyExistsException;
import com.saigonplantravel.backend.auth.repository.UserAccoutRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserAccoutRepository userAccoutRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccoutRepository userAccoutRepository, PasswordEncoder passwordEncoder) {
        this.userAccoutRepository = userAccoutRepository;
        this.passwordEncoder = passwordEncoder;
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
