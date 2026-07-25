package com.saigonplantravel.backend.auth.repository;

import com.saigonplantravel.backend.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccoutRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByEmail(String email);
    Optional<UserAccount> findByEmail(String email);
}
