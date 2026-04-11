package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.DeviceStatus;
import com.example.demo.model.Device;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {
	// 1. Tìm theo SerialNumber
    List<Device> findBySerialNumberContainingIgnoreCaseAndStatus(String serialNumber, DeviceStatus status);
    
    List<Device> findBySerialNumberContainingIgnoreCaseAndStatusNot(String serialNumber, DeviceStatus status);
    
    List<Device> findBySerialNumberContainingIgnoreCase(String serialNumber);

    // 2. Tìm theo Tên (thực chất là tìm trong bảng DeviceType)
    List<Device> findByDeviceType_TypeNameContainingIgnoreCaseAndStatusNot(String typeName, DeviceStatus status);

    List<Device> findAllByStatus(DeviceStatus status);
    
    List<Device> findAllByStatusNot(DeviceStatus status);
    
    boolean existsBySerialNumber(String serialNumber);
    
    // Đếm tổng số thiết bị
    long count(); 

    // Đếm thiết bị theo trạng thái (BROKEN, ACTIVE, MAINTENANCE)
    long countByStatus(DeviceStatus status);
    
}