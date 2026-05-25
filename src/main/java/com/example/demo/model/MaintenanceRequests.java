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
    @Column(name = "Severity", length = 20)
    private SeverityLevel severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20)
    private RequestStatus status;

    @Column(name = "CreatedAt")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "RejectionReason")
    private String rejectionReason;
    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = RequestStatus.NEW;
        }

        if (this.severity == null) {
            this.severity = SeverityLevel.LOW;
        }

        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
    }
}