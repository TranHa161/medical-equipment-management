package com.example.demo.model;

import com.example.demo.enums.DeviceStatus;

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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "device")
public class Device {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "DeviceID")
	private Integer id;
	
	@ManyToOne
    @JoinColumn(name = "TypeID")
    private DeviceType deviceType;
	
	@Column(name = "SerialNumber", length = 100, unique = true)
	private String serialNumber;
	
	@Column(name = "Location", length = 255)
    private String location;
	
	@Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20)
    private DeviceStatus status;
	
	@Column(name = "LastMaintenanceDate")
    private java.time.LocalDate lastMaintenanceDate;
	
	@ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
	
	@Column(name = "ImageUrl", length = 255)
	private String imageUrl;

	@Column(name = "Notes", columnDefinition = "TEXT")
	private String notes;
}
