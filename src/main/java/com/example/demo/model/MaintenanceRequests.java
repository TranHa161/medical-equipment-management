package com.example.demo.model;

import com.example.demo.enums.RequestStatus;
import com.example.demo.enums.SeverityLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "maintenancerequests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequests {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RequestID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "DeviceID")
    private Device device;

    @ManyToOne
    @JoinColumn(name = "UserID") 
    private Users requester;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "Severity", length = 20) // LOW, MEDIUM, HIGH, URGENT
    private SeverityLevel severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20) // NEW, PROCESSING, COMPLETED, CANCELLED
    private RequestStatus status;

    @Column(name = "CreatedAt")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "RejectionReason")
    private String rejectionReason;
	// Hàm tự động chạy trước khi dữ liệu được lưu lần đầu vào DB
    @PrePersist
    protected void onCreate() {
        // 1. Mặc định trạng thái là OPEN
        if (this.status == null) {
            this.status = RequestStatus.NEW;
        }

        // 2. Mặc định mức độ là LOW (nếu người dùng không chọn)
        if (this.severity == null) {
            this.severity = SeverityLevel.LOW;
        }

        // 3. Tự động lấy thời gian hiện tại của hệ thống
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
    }
}