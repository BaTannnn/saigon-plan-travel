package com.saigonplantravel.backend.auth.security;


import com.saigonplantravel.backend.auth.entity.UserAccount;
import com.saigonplantravel.backend.auth.repository.UserAccoutRepository;
import com.saigonplantravel.backend.common.security.jwt.AccessTokenClaims;
import com.saigonplantravel.backend.common.security.jwt.InvalidAccessTokenException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtAuthenticationService {
    private final UserAccoutRepository userAccoutRepository;
    public JwtAuthenticationService(
            UserAccoutRepository userAccoutRepository
    ){
        this.userAccoutRepository = userAccoutRepository;
    }
    @Transactional
    public Authentication createAuthentication(
            AccessTokenClaims tokenClaims
    ){
        UserAccount userAccount = userAccoutRepository
                .findByPublicId(tokenClaims.userPublicId())
                .orElseThrow(InvalidAccessTokenException::new);
        if (!Boolean.TRUE.equals(userAccount.getActive())) {
            throw new InvalidAccessTokenException();
        }

        if (userAccount.getRole() != tokenClaims.role()) {
            throw new InvalidAccessTokenException();
        }

        UserPrincipal principal =
                UserPrincipal.from(userAccount);

        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.authorities()
        );
    }
}
