package com.example.demo.service;

import java.util.List;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.demo.repository.UsersRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class NotificationService {

	// Sử dụng 'final' để đảm bảo tính bất biến (Immutability)
    private final JavaMailSender mailSender;
    private final UsersRepository usersRepository;

    // Constructor Injection: Spring Boot sẽ tự động nạp cả 2 bean này vào
    public NotificationService(JavaMailSender mailSender, UsersRepository usersRepository) {
        this.mailSender = mailSender;
        this.usersRepository = usersRepository;
    }

    /**
     * Hàm gửi email cơ bản (Dạng văn bản thuần túy)
     */
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@hospital-system.com"); // Email người gửi
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        
        mailSender.send(message);
    }

    /**
     * Thông báo cho kỹ thuật viên về trạng thái Phiếu công việc (Cấp mới / Hủy)
     */
    public void sendWorkOrderNotification(String technicianEmail, String serialNumber, String description, boolean isCancellation) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String statusTitle = isCancellation ? "HỦY LỆNH CÔNG VIỆC" : "PHÂN CÔNG CÔNG VIỆC";
            String headerColor = isCancellation ? "linear-gradient(to right, #f5365c, #f56036)" : "linear-gradient(to right, #11cdef, #1171ef)";
            
            helper.setTo(technicianEmail);
            helper.setSubject("[HUST MEDICAL] " + statusTitle + " - S/N: " + serialNumber);

            String htmlContent = 
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;'>" +
                    "<div style='background: " + headerColor + "; padding: 20px; text-align: center;'>" +
                        "<h2 style='color: white; margin: 0;'>" + statusTitle + "</h2>" +
                    "</div>" +
                    "<div style='padding: 30px; color: #444; line-height: 1.6;'>" +
                        "<p>Chào Kỹ thuật viên,</p>" +
                        "<p>" + (isCancellation ? "Lệnh sửa chữa sau đã được <strong>HỦY</strong> bởi Quản trị viên:" : "Bạn vừa được giao một <strong>PHIẾU CÔNG VIỆC MỚI</strong> trên hệ thống:") + "</p>" +
                        "<div style='background-color: #f8f9fa; border-left: 4px solid #11cdef; padding: 15px; margin: 20px 0;'>" +
                            "<strong>Thiết bị (S/N):</strong> " + serialNumber + "<br>" +
                            "<strong>Nội dung:</strong> " + description +
                        "</div>" +
                        "<div style='text-align: center; margin: 30px 0;'>" +
                            "<a href='http://localhost:8080/work-orders' style='background-color: #11cdef; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;'>XEM CHI TIẾT TRÊN HỆ THỐNG</a>" +
                        "</div>" +
                        "<p style='font-size: 0.85em; color: #888;'>Vui lòng cập nhật trạng thái thực hiện kịp thời để Ban quản lý theo dõi.</p>" +
                    "</div>" +
                    "<div style='background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 0.8em; color: #999; border-top: 1px solid #e0e0e0;'>" +
                        "&copy; 2026 HUST MEDICAL - Ban quản lý thiết bị" +
                    "</div>" +
                "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Lỗi gửi mail Work Order: " + e.getMessage());
        }
    }

    
    /**
     * Gửi báo cáo tổng hợp cho Admin sau khi chạy Scheduled Task
     */
    public void sendAdminAutoSummaryReport(int count) {
        List<String> adminEmails = usersRepository.findByRole_RoleName("ADMIN")
                                    .stream()
                                    .map(user -> user.getEmail())
                                    .filter(email -> email != null && !email.isEmpty())
                                    .toList();

        if (adminEmails.isEmpty()) return;

        String today = java.time.LocalDate.now().toString();

        for (String email : adminEmails) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(email);
                helper.setSubject("[HUST MEDICAL] Báo cáo bảo trì tự động - " + today);

                String htmlContent = 
                    "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;'>" +
                        "<div style='background: linear-gradient(to right, #11cdef, #1171ef); padding: 20px; text-align: center;'>" +
                            "<h2 style='color: white; margin: 0;'>BÁO CÁO HỆ THỐNG</h2>" +
                        "</div>" +
                        "<div style='padding: 30px; color: #444; line-height: 1.6;'>" +
                            "<p>Chào Quản trị viên,</p>" +
                            "<p>Hệ thống vừa thực hiện quy trình kiểm tra bảo trì định kỳ tự động (Scheduled Task).</p>" +
                            "<div style='text-align: center; padding: 20px; background-color: #f0f9ff; border-radius: 8px; margin: 20px 0;'>" +
                                "<span style='font-size: 0.9em; color: #555;'>Số phiếu bảo trì đã tạo mới:</span><br>" +
                                "<span style='font-size: 2.5em; font-weight: bold; color: #1171ef;'>" + count + "</span>" +
                            "</div>" +
                            "<p>Các phiếu công việc này đã được phân bổ cho các Kỹ thuật viên tương ứng dựa trên lịch bảo trì của thiết bị.</p>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                                "<a href='http://localhost:8080/dashboard' style='border: 2px solid #11cdef; color: #11cdef; padding: 10px 25px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;'>TRUY CẬP DASHBOARD</a>" +
                            "</div>" +
                        "</div>" +
                        "<div style='background-color: #f8f9fa; padding: 15px; text-align: center; font-size: 0.8em; color: #999;'>" +
                            "Đây là thông báo tự động từ máy chủ HUST MEDICAL." +
                        "</div>" +
                    "</div>";

                helper.setText(htmlContent, true);
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Lỗi gửi báo cáo Admin: " + e.getMessage());
            }
        }
    }

    public void notifyAccountantForApproval(Long workOrderId, String deviceName, String technicianName) {
        List<String> accountantEmails = usersRepository.findByRole_RoleName("")
        							.stream()
                                    .map(user -> user.getEmail())
                                    .filter(email -> email != null && !email.isEmpty())
                                    .toList();

        if (accountantEmails.isEmpty()) {
            System.out.println("Không tìm thấy email người duyệt để gửi thông báo!");
            return;
        }

        for (String email : accountantEmails) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(email);
                helper.setSubject("[HUST MEDICAL] YÊU CẦU NGHIỆM THU - Phiếu #" + workOrderId);

                String htmlContent = 
                    "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;'>" +
                        "<div style='background: linear-gradient(to right, #fb6340, #fbb140); padding: 20px; text-align: center;'>" +
                            "<h2 style='color: white; margin: 0;'>YÊU CẦU NGHIỆM THU</h2>" +
                        "</div>" +
                        "<div style='padding: 30px; color: #444; line-height: 1.6;'>" +
                            "<p>Chào bạn,</p>" +
                            "<p>Phiếu sửa chữa/bảo trì cho thiết bị <strong>" + deviceName + "</strong> đã được hoàn thành bởi kỹ thuật viên <strong>" + technicianName + "</strong>.</p>" +
                            "<div style='background-color: #fff4e5; border-left: 4px solid #fb6340; padding: 15px; margin: 20px 0;'>" +
                                "Vui lòng đăng nhập hệ thống để <strong>kiểm tra hình ảnh bằng chứng</strong> và xác nhận nghiệm thu để thiết bị có thể hoạt động trở lại." +
                            "</div>" +
                            "<div style='text-align: center; margin: 30px 0;'>" +
                                "<a href='http://localhost:8080/approvals' style='background-color: #fb6340; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;'>ĐẾN TRANG NGHIỆM THU</a>" +
                            "</div>" +
                        "</div>"+
                    "</div>";

                helper.setText(htmlContent, true);
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("Lỗi gửi thông báo nghiệm thu: " + e.getMessage());
            }
        }
    }
}
