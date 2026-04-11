package com.example.demo.model;

import com.example.demo.enums.ScheduleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "maintenanceschedule")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaintenanceSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ScheduleID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "DeviceID")
    private Device device;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ScheduleType", length = 20) // DAILY, WEEKLY, MONTHLY
    private ScheduleType scheduleType;

    @Column(name = "CycleValue")
    private Integer cycleValue;

    @Column(name = "StartDate")
    private java.time.LocalDate startDate;

    @Column(name = "IsActive")
    private Boolean isActive;
}