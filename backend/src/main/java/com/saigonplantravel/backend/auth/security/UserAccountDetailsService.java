package com.saigonplantravel.backend.auth.security;

import com.saigonplantravel.backend.auth.entity.UserAccount;
import com.saigonplantravel.backend.auth.repository.UserAccountRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountDetailsService implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;

    public UserAccountDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserAccount userAccount = userAccountRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        return User.withUsername(userAccount.getEmail())
                .password(userAccount.getPasswordHash())
                .authorities("ROLE_" + userAccount.getRole().name())
                .disabled(!Boolean.TRUE.equals(userAccount.getActive()))
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
