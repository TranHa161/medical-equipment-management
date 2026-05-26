package com.example.demo.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.DeviceDTO;
import com.example.demo.dto.DeviceDetailResponseDTO;
import com.example.demo.enums.DeviceStatus;
import com.example.demo.enums.WorkOrderStatus;
import com.example.demo.model.Company;
import com.example.demo.model.Device;
import com.example.demo.model.DeviceType;
import com.example.demo.model.WorkOrder;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceTypeRepository;
import com.example.demo.repository.WorkOrderRepository;

import jakarta.transaction.Transactional;

@Service
public class DeviceService {
    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceTypeRepository typeRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private CompanyRepository companyRepository;
    
    public List<DeviceDTO> searchDevices(String keyword, DeviceStatus excludeStatus) {
        List<Device> devices;
        boolean hasKeyword = keyword != null && !keyword.isEmpty();

        if (excludeStatus == null) {
            devices = hasKeyword ? 
                deviceRepository.findBySerialNumberContainingIgnoreCase(keyword) : 
                deviceRepository.findAll();
        } else {
            devices = hasKeyword ? 
                deviceRepository.findBySerialNumberContainingIgnoreCaseAndStatusNot(keyword, excludeStatus) : 
                deviceRepository.findAllByStatusNot(excludeStatus);
        }
        return devices.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public DeviceDetailResponseDTO createDevice(DeviceDTO dto, MultipartFile imageFile, MultipartFile manualFile) {
        if (deviceRepository.existsBySerialNumber(dto.getSerialNumber())) {
            throw new RuntimeException("Số Serial này đã tồn tại trên hệ thống!");
        }

        DeviceType type;
        if (dto.getTypeId() != null) {
            type = typeRepository.findById(dto.getTypeId())
                    .orElseThrow(() -> new RuntimeException("Loại thiết bị không hợp lệ!"));
        } else if (dto.getNewTypeName() != null && !dto.getNewTypeName().trim().isEmpty()) {
            String cleanTypeName = dto.getNewTypeName().trim();
            Optional<DeviceType> existingType = typeRepository.findByTypeName(cleanTypeName);
            
            if (existingType.isPresent()) {
                type = existingType.get();
            } else {
                DeviceType newType = new DeviceType();
                newType.setTypeName(cleanTypeName);
                newType.setManufacturer(dto.getManufacturer());
                newType.setModel(dto.getModel());
                newType.setDefaultMaintenanceCycle(dto.getDefaultMaintenanceCycle());
                newType.setTypeDescription(dto.getTypeDescription());
                
                if (manualFile != null && !manualFile.isEmpty()) {
                	try {
                        String manualUrl = s3Service.uploadFile(manualFile);
                        newType.setManualUrl(manualUrl);
                    } catch (Exception e) {
                        throw new RuntimeException("Lỗi upload tài liệu lên S3: " + e.getMessage());
                    }
                } else {
                    newType.setManualUrl(dto.getManualUrl());
                }
                
                type = typeRepository.save(newType);
            }
        } else {
            throw new RuntimeException("Vui lòng chọn hoặc nhập thông tin loại thiết bị!");
        }
        Company targetCompany = null;

        if (dto.getCompanyId() != null) {
            targetCompany = companyRepository.findById(dto.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Công ty đối tác không tồn tại!"));
        } else if (dto.getNewCompanyName() != null && !dto.getNewCompanyName().trim().isEmpty()) {
            String cleanCompanyName = dto.getNewCompanyName().trim();
            
            Optional<Company> existingCompany = companyRepository.findByName(cleanCompanyName);
            if (existingCompany.isPresent()) {
                targetCompany = existingCompany.get();
            } else {
                Company newCompany = new Company();
                newCompany.setName(cleanCompanyName);
                newCompany.setTaxCode(dto.getCompanyTaxCode());
                newCompany.setContactInfo(dto.getCompanyContactInfo());
                
                targetCompany = companyRepository.save(newCompany);
            }
        }

        Device device = new Device();
        device.setSerialNumber(dto.getSerialNumber());
        device.setLocation(dto.getLocation());
        device.setNotes(dto.getNotes());       
        device.setDeviceType(type);
        device.setStatus(DeviceStatus.valueOf(dto.getStatus()));
        device.setCompany(targetCompany);
        
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = s3Service.uploadFile(imageFile);
                device.setImageUrl(imageUrl); 
            } catch (Exception e) {
                throw new RuntimeException("Lỗi upload ảnh lên S3: " + e.getMessage());
            }
        }
        
        device.setLastMaintenanceDate(java.time.LocalDate.now());

        Device savedDevice = deviceRepository.save(device);
        return convertToDetailDTO(savedDevice);
    }

    @Transactional
    public DeviceDetailResponseDTO updateDevice(Integer id, DeviceDTO dto, 
                                                MultipartFile imageFile, 
                                                MultipartFile manualFile,
                                                Authentication auth) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị!"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            if (!device.getSerialNumber().equals(dto.getSerialNumber())) {
                if (deviceRepository.existsBySerialNumber(dto.getSerialNumber())) {
                    throw new RuntimeException("Số Serial mới này đã tồn tại!");
                }
                device.setSerialNumber(dto.getSerialNumber());
            }
            device.setLocation(dto.getLocation());
            device.setNotes(dto.getNotes()); 

            if (dto.getCompanyId() != null) {
                Company targetCompany = companyRepository.findById(dto.getCompanyId())
                        .orElseThrow(() -> new RuntimeException("Công ty đối tác không tồn tại!"));
                device.setCompany(targetCompany);
            } else if (dto.getNewCompanyName() != null && !dto.getNewCompanyName().trim().isEmpty()) {
                String cleanCompanyName = dto.getNewCompanyName().trim();
                Optional<Company> existingCompany = companyRepository.findByName(cleanCompanyName);
                
                if (existingCompany.isPresent()) {
                    device.setCompany(existingCompany.get());
                } else {
                    Company newCompany = new Company();
                    newCompany.setName(cleanCompanyName);
                    newCompany.setTaxCode(dto.getCompanyTaxCode());
                    newCompany.setContactInfo(dto.getCompanyContactInfo());
                    
                    Company savedCompany = companyRepository.save(newCompany);
                    device.setCompany(savedCompany);
                }
            } else {
                device.setCompany(null); 
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String newImageUrl = s3Service.uploadFile(imageFile); 
                    device.setImageUrl(newImageUrl); 
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi cập nhật ảnh S3");
                }
            }

            if (dto.getTypeId() != null) {
                DeviceType newType = typeRepository.findById(dto.getTypeId())
                        .orElseThrow(() -> new RuntimeException("Loại thiết bị không hợp lệ!"));
                device.setDeviceType(newType);
            } else if (dto.getNewTypeName() != null && !dto.getNewTypeName().trim().isEmpty()) {
                // Logic tạo loại mới giữ nguyên như của bạn nhưng tối ưu lại
                String cleanTypeName = dto.getNewTypeName().trim();
                DeviceType nt = typeRepository.findByTypeName(cleanTypeName).orElse(new DeviceType());
                nt.setTypeName(cleanTypeName);
                nt.setManufacturer(dto.getManufacturer());
                nt.setModel(dto.getModel());
                nt.setDefaultMaintenanceCycle(dto.getDefaultMaintenanceCycle());
                nt.setTypeDescription(dto.getTypeDescription());
                device.setDeviceType(typeRepository.save(nt));
            }

            if (manualFile != null && !manualFile.isEmpty()) {
                DeviceType currentType = device.getDeviceType();
                if (currentType != null) {
                    try {
                        String manualUrl = s3Service.uploadFile(manualFile); 
                        currentType.setManualUrl(manualUrl);
                        typeRepository.save(currentType); 
                    } catch (Exception e) {
                        throw new RuntimeException("Lỗi cập nhật tài liệu S3");
                    }
                }
            }
            
            updateStatusSafely(device, dto.getStatus());
        }

        Device savedDevice = deviceRepository.save(device);
        return convertToDetailDTO(savedDevice);
    }

    private void updateStatusSafely(Device device, String statusStr) {
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                device.setStatus(DeviceStatus.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Trạng thái thiết bị '" + statusStr + "' không hợp lệ!");
            }
        }
    }
    
    
    public DeviceDTO getDeviceById(Integer id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị!"));
        return convertToDTO(device);
    }
    
    @Autowired
    private WorkOrderRepository workOrderRepository;

    public List<DeviceDTO> getDevicesForUser(Authentication auth, String keyword) {
        String username = auth.getName();
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return searchDevices(keyword, null); 
        } 
        
        else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"))) {
            List<WorkOrder> myOrders = workOrderRepository.findByTechnician_UsernameAndStatusNot(username, WorkOrderStatus.CANCELLED);
            return myOrders.stream()
                    .map(WorkOrder::getDevice)
                    .filter(d -> !hasKeyword || d.getSerialNumber().toLowerCase().contains(keyword.toLowerCase()))
                    .distinct()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
        
        List<Device> devices;
        if (hasKeyword) {
            devices = deviceRepository.findBySerialNumberContainingIgnoreCaseAndStatusNot(keyword, DeviceStatus.UNAVAILABLE);
        } else {
            devices = deviceRepository.findAllByStatusNot(DeviceStatus.UNAVAILABLE);
        }
            
        return devices.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private DeviceDTO convertToDTO(Device device) {
        DeviceDTO dto = new DeviceDTO();
        
        dto.setId(device.getId());
        dto.setSerialNumber(device.getSerialNumber());
        
        String typeName = (device.getDeviceType() != null) ? device.getDeviceType().getTypeName() : "N/A";
        dto.setName(typeName);
        
        dto.setLocation(device.getLocation());
        
        if (device.getCompany() != null) {
            dto.setCompanyId(device.getCompany().getId());
        }

        if (device.getStatus() != null) {
            dto.setStatus(device.getStatus().name());
        }
        
        Integer typeId = (device.getDeviceType() != null) ? device.getDeviceType().getId() : null;
        dto.setTypeId(typeId);
        
        dto.setLastMaintenanceDate(device.getLastMaintenanceDate());
        
        return dto;
    }
    
    private DeviceDetailResponseDTO convertToDetailDTO(Device device) {
        DeviceDetailResponseDTO resp = new DeviceDetailResponseDTO();
        resp.setId(device.getId());
        resp.setSerialNumber(device.getSerialNumber());
        resp.setLocation(device.getLocation());
        resp.setStatus(device.getStatus().name());
        resp.setImageUrl(device.getImageUrl());
        resp.setNotes(device.getNotes());
        resp.setLastMaintenanceDate(device.getLastMaintenanceDate());

        if (device.getCompany() != null) {
            resp.setCompanyId(device.getCompany().getId());
            resp.setCompanyName(device.getCompany().getName());
        }

        if (device.getDeviceType() != null) {
            DeviceType type = device.getDeviceType();
            resp.setTypeName(type.getTypeName());
            resp.setManufacturer(type.getManufacturer());
            resp.setModel(type.getModel());
            resp.setDefaultMaintenanceCycle(type.getDefaultMaintenanceCycle());
            resp.setTypeDescription(type.getTypeDescription());
            resp.setManualUrl(type.getManualUrl());
        }
        return resp;
    }

	public List<DeviceDTO> findAllDevices() {
        List<Device> devices = deviceRepository.findAll();

        return devices.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
	}
	
	public DeviceDetailResponseDTO getDeviceDetail(Integer id) {
        Device device = deviceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị!"));

        return mapToDetailDTO(device);
    }

    private DeviceDetailResponseDTO mapToDetailDTO(Device entity) {
        DeviceDetailResponseDTO dto = new DeviceDetailResponseDTO();
        dto.setId(entity.getId());
        dto.setSerialNumber(entity.getSerialNumber());
        dto.setLocation(entity.getLocation());
        dto.setStatus(entity.getStatus().name());
        dto.setImageUrl(entity.getImageUrl());
        dto.setNotes(entity.getNotes());

        if (entity.getLastMaintenanceDate() != null) {
        	dto.setLastMaintenanceDate(entity.getLastMaintenanceDate());
        }

        if (entity.getCompany() != null) {
            dto.setCompanyId(entity.getCompany().getId());
            dto.setCompanyName(entity.getCompany().getName());
        }

        if (entity.getDeviceType() != null) {
            DeviceType type = entity.getDeviceType();
            dto.setTypeName(type.getTypeName());
            dto.setManufacturer(type.getManufacturer());
            dto.setModel(type.getModel());
            dto.setDefaultMaintenanceCycle(type.getDefaultMaintenanceCycle());
            dto.setTypeDescription(type.getTypeDescription());
            dto.setManualUrl(type.getManualUrl());
        }
        return dto;
    }
}
