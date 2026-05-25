package com.example.demo.dto;

import java.time.LocalDate;

import com.example.demo.enums.ScheduleType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceScheduleResponseDTO {

    private Long id;

    private Integer deviceId;
    private String deviceName;

    private ScheduleType scheduleType;
    private Integer cycleValue;

    private LocalDate startDate;
    private Boolean isActive;
}
