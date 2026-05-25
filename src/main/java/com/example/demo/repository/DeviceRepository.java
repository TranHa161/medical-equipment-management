package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.DeviceStatus;
import com.example.demo.model.Device;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {
    List<Device> findBySerialNumberContainingIgnoreCaseAndStatus(String serialNumber, DeviceStatus status);
    
    List<Device> findBySerialNumberContainingIgnoreCaseAndStatusNot(String serialNumber, DeviceStatus status);
    
    List<Device> findBySerialNumberContainingIgnoreCase(String serialNumber);

    List<Device> findByDeviceType_TypeNameContainingIgnoreCaseAndStatusNot(String typeName, DeviceStatus status);

    List<Device> findAllByStatus(DeviceStatus status);
    
    List<Device> findAllByStatusNot(DeviceStatus status);
    
    boolean existsBySerialNumber(String serialNumber);
    
    long count(); 

    long countByStatus(DeviceStatus status);
    
}