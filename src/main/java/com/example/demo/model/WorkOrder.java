package com.example.demo.model;

import com.example.demo.enums.WorkOrderStatus;

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
@Table(name = "workorder")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class WorkOrder {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WorkOrderID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "DeviceID")
    private Device device;

    @ManyToOne
    @JoinColumn(name = "RequestID")
    private MaintenanceRequests request;

    @ManyToOne
    @JoinColumn(name = "ScheduleID")
    private MaintenanceSchedule schedule;

    @ManyToOne
    @JoinColumn(name = "AssignedTo") // Kỹ thuật viên được giao việc
    private Users technician;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20) // PLANNED, IN_PROGRESS, COMPLETED
    private WorkOrderStatus status;
    
    @Column(name = "CreatedAt")
    private java.time.LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = java.time.LocalDateTime.now();
    }
}