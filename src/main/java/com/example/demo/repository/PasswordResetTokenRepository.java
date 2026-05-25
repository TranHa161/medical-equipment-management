package com.example.demo.repository;

import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(Users user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDateTime now);

    @Modifying
    void deleteByUser(Users user);
}