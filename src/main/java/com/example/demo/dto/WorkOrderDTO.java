package com.example.demo.dto;

import com.example.demo.enums.WorkOrderStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkOrderDTO {
    private Long id;
    private Integer deviceId;
    private String deviceName;
    private Long technicianId;
    private Long scheduleId;
    private String technicianName;
    private WorkOrderStatus status;
    private java.time.LocalDateTime createdAt;
}
