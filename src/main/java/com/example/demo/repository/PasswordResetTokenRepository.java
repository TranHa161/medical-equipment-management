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

    // 1. Tìm kiếm token theo chuỗi UUID gửi từ Email
    Optional<PasswordResetToken> findByToken(String token);

    // 2. Tìm kiếm token theo User (Dùng để kiểm tra xem User đó đã yêu cầu reset chưa)
    Optional<PasswordResetToken> findByUser(Users user);

    // 3. Xóa các token đã hết hạn (Dùng cho tác vụ dọn dẹp hệ thống)
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate <= ?1")
    void deleteAllExpiredSince(LocalDateTime now);

    // 4. Xóa token của một User cụ thể (Sau khi đổi mật khẩu thành công)
    @Modifying
    void deleteByUser(Users user);
}