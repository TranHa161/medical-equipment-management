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
    private Integer id;
    private String serialNumber;
    private String name;
    private String location;
    private String status;
    private String imageUrl;
    private String notes;
    private LocalDate lastMaintenanceDate;
    
    private Integer companyId;
    private String newCompanyName;
    private String companyTaxCode;
    private String companyContactInfo;

    private Integer typeId; 


    private String newTypeName;               
    private String manufacturer;             
    private String model;
    private Integer defaultMaintenanceCycle;
    private String typeDescription;
    private String manualUrl;
}