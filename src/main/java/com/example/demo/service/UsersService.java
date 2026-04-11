package com.example.demo.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.UserSaveRequest;
import com.example.demo.dto.UsersResponseDTO;
import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.Roles;
import com.example.demo.model.Users;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.RolesRepository;
import com.example.demo.repository.UsersRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RolesRepository rolesRepository;
    
    @Autowired
    private S3Service s3Service;
    
    @Autowired private JavaMailSender mailSender;
    
    @Autowired private PasswordResetTokenRepository tokenRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ===================== LOGIN =====================
    public Users login(String username, String rawPassword) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập không tồn tại!"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác!");
        }

        return user;
    }

    // ===================== CREATE (by Registration) =====================
    public Users createUser(Users newUser, Integer roleId) {
        if (usersRepository.existsByUsername(newUser.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }

        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        Roles role = rolesRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ!"));
        newUser.setRole(role);
        newUser.setIsActive(true);

        return usersRepository.save(newUser);
    }

    // ===================== CHANGE PASSWORD =====================
    public void changePassword(String username, String oldPassword, String newPassword) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không chính xác!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);
    }


    // ===================== DTO Mapping =====================
    private UsersResponseDTO toResponseDTO(Users user) {
        UsersResponseDTO dto = new UsersResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRoleName(user.getRole().getRoleName()); // ADMIN / TECHNICIAN
        // Trong hàm convert sang UsersResponseDTO
        dto.setRoleId(user.getRole() != null ? user.getRole().getId() : null);
        dto.setIsActive(user.getIsActive());
        dto.setProfilePicture(user.getProfilePicture());
        return dto;
    }

    // ===================== FIND ALL =====================
    public List<UsersResponseDTO> findAllUsers() {
        return usersRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
    
    public List<UsersResponseDTO> findAllTechnicians() {
        // Lọc danh sách người dùng có RoleName là 'TECHNICIAN'
        List<Users> technicians = usersRepository.findByRole_RoleName("TECHNICIAN");

        return technicians.stream()
                .map(user -> UsersResponseDTO.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .username(user.getUsername())
                        .roleName(user.getRole().getRoleName())
                        .isActive(user.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    // ===================== FIND BY ID =====================
    public UsersResponseDTO getUserById(Long id) {
        Users user = usersRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));
        return toResponseDTO(user);
    }

    public List<Roles> getAllRoles() {
        return rolesRepository.findAll();
    }
    
    public Long getUserIdByUsername(String username) {
        return usersRepository.findByUsername(username)
                .map(Users::getId) // Nếu tìm thấy, lấy ID
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với tên đăng nhập: " + username));
    }

    // ===================== CREATE + UPDATE  =====================
    @Transactional
    public UsersResponseDTO saveUser(UserSaveRequest dto) {

        boolean isCreate = (dto.getId() == null);

        // Check trùng username khi create
        if (isCreate && usersRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username đã tồn tại!");
        }

        Users entity;

        if (isCreate) {
            entity = new Users();
            entity.setIsActive(true); // default

            if (dto.getPassword() == null || dto.getPassword().isBlank()) {
                throw new RuntimeException("Password không được để trống khi tạo mới!");
            }

            entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        } 
        else {
            entity = usersRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("User không tồn tại!"));

            // Nếu có nhập password mới thì đổi
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                entity.setPassword(passwordEncoder.encode(dto.getPassword()));
            }

            // Nếu update username thì check trùng với người khác
            Optional<Users> checkUser = usersRepository.findByUsername(dto.getUsername());
            if (checkUser.isPresent() && !checkUser.get().getId().equals(dto.getId())) {
                throw new RuntimeException("Username đã tồn tại!");
            }
        }

        entity.setUsername(dto.getUsername());
        entity.setFullName(dto.getFullName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setIsActive(dto.getIsActive());

        // Set role theo roleId
        Roles role = rolesRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role không hợp lệ!"));
        entity.setRole(role);

        Users saved = usersRepository.save(entity);

        return toResponseDTO(saved);
    }
    
    
    public List<UsersResponseDTO> searchUsers(String keyword) {
        List<Users> users = usersRepository.findByFullNameContainingIgnoreCase(keyword);
        
        return users.stream()
                    .map(this::toResponseDTO)
                    .collect(Collectors.toList());
    }
    
    @Transactional
    public UsersResponseDTO updateUserProfile(String username, String fullName, String phone, String email, MultipartFile file) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);

        if (file != null && !file.isEmpty()) {
            try {
                String oldImageUrl = user.getProfilePicture();

                String newImageUrl = s3Service.uploadFile(file); 
                
                user.setProfilePicture(newImageUrl);

                // Chỉ xóa ảnh cũ nếu nó tồn tại và không phải ảnh mặc định
                if (oldImageUrl != null && !oldImageUrl.contains("logo.png")) {
                    s3Service.deleteFile(oldImageUrl); 
                }

                System.out.println("Cập nhật Profile thành công. URL mới: " + newImageUrl);
                
            } catch (IOException e) {
                System.err.println("Lỗi Upload Profile: " + e.getMessage());
                throw new RuntimeException("Không thể cập nhật ảnh đại diện lên Cloud!");
            }
        }

        return toResponseDTO(usersRepository.save(user)); 
    }
    
    public void sendTemporaryPassword(String email) {
        // 1. Kiểm tra email có tồn tại không
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống!"));

        // 2. Tạo mật khẩu tạm thời
        String tempPass = "Hust@" + (int)(Math.random() * 900000 + 100000);

        // 3. Cập nhật vào Database 
        user.setPassword(passwordEncoder.encode(tempPass));
        usersRepository.save(user);

        // 4. Gửi Mail
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("HUST MEDICAL - Khôi phục mật khẩu");
        message.setText("Chào " + user.getFullName() + ",\n\n" +
                        "Mật khẩu tạm thời của bạn là: " + tempPass + "\n" +
                        "Vui lòng đăng nhập và đổi lại mật khẩu ngay để bảo mật tài khoản.");
        mailSender.send(message);
    }
    
    @Transactional
    public void createPasswordResetTokenForUser(String username, String host) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập không tồn tại!"));

        // 1. Tạo Token ngẫu nhiên
        String token = UUID.randomUUID().toString();
        
        // 2. Lưu token với thời hạn 15 phút
        PasswordResetToken myToken = new PasswordResetToken();
        myToken.setToken(token);
        myToken.setUser(user);
        myToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(myToken);

        // 3. Gửi Email với Link
        String resetUrl = host + "/reset-password?token=" + token;
        sendResetEmail(user.getEmail(), user.getFullName(), resetUrl);
    }
    
    public void sendResetEmail(String recipientEmail, String fullName, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("HUST MEDICAL - Yêu cầu khôi phục mật khẩu");

            String htmlContent = 
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;'>" +
                    "<div style='background: linear-gradient(to right, #11cdef, #1171ef); padding: 20px; text-align: center;'>" +
                        "<h2 style='color: white; margin: 0;'>HUST MEDICAL</h2>" +
                    "</div>" +
                    "<div style='padding: 30px; color: #444; line-height: 1.6;'>" +
                        "<p>Chào <strong>" + fullName + "</strong>,</p>" +
                        "<p>Chúng tôi đã nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn tại hệ thống <strong>HUST MEDICAL</strong>.</p>" +
                        "<p>Vui lòng nhấn vào nút bên dưới để tiến hành đặt lại mật khẩu. <strong>Lưu ý: Link này sẽ hết hạn sau 15 phút.</strong></p>" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='" + resetUrl + "' style='background-color: #11cdef; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                                "ĐẶT LẠI MẬT KHẨU" +
                            "</a>" +
                        "</div>" +
                        "<p style='font-size: 0.9em; color: #888;'>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này hoặc liên hệ với Admin để đảm bảo an toàn cho tài khoản.</p>" +
                    "</div>" +
                    "<div style='background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 0.8em; color: #999; border-top: 1px solid #e0e0e0;'>" +
                        "&copy; 2026 HUST MEDICAL - Hệ thống Quản lý Thiết bị Y tế" +
                    "</div>" +
                "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            System.err.println("Lỗi gửi email khôi phục: " + e.getMessage());
            throw new RuntimeException("Không thể gửi email khôi phục mật khẩu. Vui lòng thử lại sau!");
        }
    }
    
    @Transactional
    public void updatePasswordAndDeleteToken(String token, String newPassword) {
        // 1. Tìm và kiểm tra lại Token một lần nữa cho chắc chắn
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .filter(t -> !t.isExpired())
                .orElseThrow(() -> new RuntimeException("Link khôi phục đã hết hạn hoặc không hợp lệ!"));

        // 2. Lấy User gắn liền với Token đó
        Users user = resetToken.getUser();

        // 3. Mã hóa mật khẩu mới bằng BCrypt và cập nhật
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        // 4. Xóa Token ngay sau khi sử dụng
        tokenRepository.delete(resetToken);
    }

}
