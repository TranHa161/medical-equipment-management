package com.example.demo.dto;

import com.example.demo.enums.SeverityLevel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaintenanceRequestsDTO {
	private Integer deviceId;
    private Long requesterId;
    private String description;
    private SeverityLevel severity;
}
