package com.example.demo.dto;

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
public class MaintenanceHistoryResponseDTO {
	private Long id;
    private String deviceName; 
    private String deviceSerial;
    
    private String technicianName;
    private java.time.LocalDateTime maintenanceDate;
    private String result;
    private java.math.BigDecimal cost;
}
