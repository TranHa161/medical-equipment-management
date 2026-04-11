package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "devicetype")
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class DeviceType {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TypeID")
    private Integer id;

    @Column(name = "TypeName", nullable = false, length = 100)
    private String typeName;

    @Column(name = "Manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "Model", length = 100)
    private String model;

    @Column(name = "DefaultMaintenanceCycle")
    private Integer defaultMaintenanceCycle;

    @Column(name = "TypeDescription", columnDefinition = "TEXT")
    private String typeDescription;
    
    @Column(name = "ManualUrl", length = 255)
    private String manualUrl;
}
