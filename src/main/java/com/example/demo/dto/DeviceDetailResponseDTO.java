package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceDetailResponseDTO {
    // Thông tin từ Device
    private Integer id;
    private String serialNumber;
    private String location;
    private String status;
    private java.time.LocalDate lastMaintenanceDate;
    private String imageUrl;
    private String notes;

    // Thông tin từ DeviceType
    private String typeName;
    private String manufacturer;
    private String model;
    private Integer defaultMaintenanceCycle;
    private String typeDescription;
    private String manualUrl;
}