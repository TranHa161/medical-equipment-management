package com.example.demo.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceDTO {
    // --- THÔNG TIN THIẾT BỊ (DEVICE) ---
    private Integer id;
    private String serialNumber;
    private String name; // Tên riêng của máy (nếu có)
    private String location;
    private String status;
    private String imageUrl;
    private String notes;
    private LocalDate lastMaintenanceDate;

    // --- THÔNG TIN LOẠI THIẾT BỊ (DEVICE TYPE) ---
    
    // 1. Nếu chọn loại có sẵn: Gửi ID này về
    private Integer typeId; 

    // 2. Nếu thêm loại mới: Gửi các thông tin này về
    private String newTypeName;               // Tên loại mới (thay thế typeName)
    private String manufacturer;              // Nhà sản xuất
    private String model;                     // Model máy
    private Integer defaultMaintenanceCycle;  // Chu kỳ bảo trì (ngày)
    private String typeDescription;           // Mô tả loại máy
    private String manualUrl;                 // Link tài liệu hướng dẫn
}