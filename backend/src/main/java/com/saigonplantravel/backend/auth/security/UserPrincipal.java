package com.saigonplantravel.backend.auth.security;

import com.saigonplantravel.backend.auth.domain.UserRole;
import com.saigonplantravel.backend.auth.entity.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        UUID publicId,
        String email,
        String displayName,
        UserRole role
) implements Principal{
    public static UserPrincipal from(UserAccount userAccount){
        return new UserPrincipal(
                userAccount.getPublicId(),
                userAccount.getEmail(),
                userAccount.getDisplayName(),
                userAccount.getRole()
        );
    }
    @Override
    public String getName() {
        return publicId.toString();
    }
    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + role.name()
                )
        );
    }
}