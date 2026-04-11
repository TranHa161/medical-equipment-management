package com.example.demo.dto;

import com.example.demo.enums.RequestStatus;
import com.example.demo.enums.SeverityLevel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequestsResponseDTO {
    private Long id;

    private Integer deviceId;
    private String deviceName;

    private Long requesterId;
    private String requesterUsername;
    private String requesterFullName;
    private String deviceLocation;
    private String serialNumber;
    private String description;
    private SeverityLevel severity;
    private RequestStatus status;
    private java.time.LocalDateTime createdAt;
    private String rejectionReason;
}
