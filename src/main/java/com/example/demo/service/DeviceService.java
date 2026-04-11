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
import com.example.demo.model.Device;
import com.example.demo.model.DeviceType;
import com.example.demo.model.Users;
import com.example.demo.model.WorkOrder;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceTypeRepository;
import com.example.demo.repository.UsersRepository;
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
    
    // UC02: Tra cứu (Đã có, giữ nguyên)
    public List<DeviceDTO> searchDevices(String keyword, DeviceStatus excludeStatus) {
        List<Device> devices;
        boolean hasKeyword = keyword != null && !keyword.isEmpty();

        if (excludeStatus == null) {
            // Nếu không cần loại trừ trạng thái (thường dành cho Admin)
            devices = hasKeyword ? 
                deviceRepository.findBySerialNumberContainingIgnoreCase(keyword) : 
                deviceRepository.findAll();
        } else {
            // Nếu cần loại trừ trạng thái (thường dành cho User)
            devices = hasKeyword ? 
                deviceRepository.findBySerialNumberContainingIgnoreCaseAndStatusNot(keyword, excludeStatus) : 
                deviceRepository.findAllByStatusNot(excludeStatus);
        }
        return devices.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // UC03: Thêm thông tin thiết bị (STT 5-6)
    @Transactional
    public DeviceDetailResponseDTO createDevice(DeviceDTO dto, MultipartFile imageFile, MultipartFile manualFile) {
        // 1. Kiểm tra trùng Serial
        if (deviceRepository.existsBySerialNumber(dto.getSerialNumber())) {
            throw new RuntimeException("Số Serial này đã tồn tại trên hệ thống!");
        }

        // 2. Xử lý Loại thiết bị (Linh hoạt: Cũ hoặc Mới)
        DeviceType type;
        if (dto.getTypeId() != null) {
            // Trường hợp dùng loại máy có sẵn
            type = typeRepository.findById(dto.getTypeId())
                    .orElseThrow(() -> new RuntimeException("Loại thiết bị không hợp lệ!"));
        } else if (dto.getNewTypeName() != null && !dto.getNewTypeName().trim().isEmpty()) {
            // Trường hợp thêm loại máy mới: Kiểm tra trùng tên loại trước khi tạo
            String cleanTypeName = dto.getNewTypeName().trim();
            Optional<DeviceType> existingType = typeRepository.findByTypeName(cleanTypeName);
            
            if (existingType.isPresent()) {
                type = existingType.get(); // Nếu tên đã có, dùng luôn cái cũ
            } else {
                // Tạo mới hoàn toàn DeviceType
                DeviceType newType = new DeviceType();
                newType.setTypeName(cleanTypeName);
                newType.setManufacturer(dto.getManufacturer());
                newType.setModel(dto.getModel());
                newType.setDefaultMaintenanceCycle(dto.getDefaultMaintenanceCycle());
                newType.setTypeDescription(dto.getTypeDescription());
                
                // Xử lý lưu file tài liệu hướng dẫn vào subDir 'manuals'
                if (manualFile != null && !manualFile.isEmpty()) {
                	try {
                        String manualUrl = s3Service.uploadFile(manualFile); // Đẩy lên S3
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

        // 3. Tạo Entity Device và liên kết với Type vừa xử lý
        Device device = new Device();
        device.setSerialNumber(dto.getSerialNumber());
        device.setLocation(dto.getLocation());
        device.setNotes(dto.getNotes());       
        device.setDeviceType(type);
        device.setStatus(DeviceStatus.valueOf(dto.getStatus()));
        
        // Xử lý lưu ảnh máy vào subDir 'devices'
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = s3Service.uploadFile(imageFile); // Đẩy lên S3
                device.setImageUrl(imageUrl); 
            } catch (Exception e) {
                throw new RuntimeException("Lỗi upload ảnh lên S3: " + e.getMessage());
            }
        }
        
        // Thiết lập ngày bảo trì mặc định là ngày tạo máy
        device.setLastMaintenanceDate(java.time.LocalDate.now());

        // 4. Lưu Device và chuyển đổi sang Response DTO chi tiết
        Device savedDevice = deviceRepository.save(device);
        return convertToDetailDTO(savedDevice);
    }

    @Transactional
    public DeviceDetailResponseDTO updateDevice(Integer id, DeviceDTO dto, 
                                                MultipartFile imageFile, 
                                                MultipartFile manualFile, // 👈 Thêm tham số này
                                                Authentication auth) {
        // 1. Tìm thiết bị hiện tại
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thiết bị!"));

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
        	// 1. Cập nhật thông tin cơ bản
            if (!device.getSerialNumber().equals(dto.getSerialNumber())) {
                if (deviceRepository.existsBySerialNumber(dto.getSerialNumber())) {
                    throw new RuntimeException("Số Serial mới này đã tồn tại!");
                }
                device.setSerialNumber(dto.getSerialNumber());
            }
            device.setLocation(dto.getLocation());
            device.setNotes(dto.getNotes()); 

            // 2. XỬ LÝ ẢNH MÁY
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String newImageUrl = s3Service.uploadFile(imageFile); 
                    device.setImageUrl(newImageUrl); 
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi cập nhật ảnh S3");
                }
            }

            // 3. XỬ LÝ LOẠI THIẾT BỊ (Xác định loại máy trước)
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

        // 4. Lưu Device và trả về DTO chi tiết đầy đủ thông tin nhất
        Device savedDevice = deviceRepository.save(device);
        return convertToDetailDTO(savedDevice);
    }
    
    /**
     * Hỗ trợ cập nhật trạng thái an toàn
     */
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
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty(); // Dùng trim() để an toàn hơn
        
        // 1. ADMIN: Thấy toàn bộ, có hỗ trợ search
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return searchDevices(keyword, null); 
        } 
        
        // 2. TECHNICIAN: Lấy máy được giao VÀ lọc theo keyword
        else if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"))) {
            List<WorkOrder> myOrders = workOrderRepository.findByTechnician_UsernameAndStatusNot(username, WorkOrderStatus.CANCELLED);
            return myOrders.stream()
                    .map(WorkOrder::getDevice)
                    .filter(d -> !hasKeyword || d.getSerialNumber().toLowerCase().contains(keyword.toLowerCase()))
                    .distinct()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
        
        // 3. USER: Thấy tất cả trừ trạng thái UNAVAILABLE
        List<Device> devices;
        if (hasKeyword) {
            // Tìm theo Serial và loại trừ máy đã hủy/không khả dụng
            devices = deviceRepository.findBySerialNumberContainingIgnoreCaseAndStatusNot(keyword, DeviceStatus.UNAVAILABLE);
        } else {
            // Lấy tất cả máy đang vận hành, bảo trì hoặc hỏng (trừ UNAVAILABLE)
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
        // Map Device fields
        resp.setId(device.getId());
        resp.setSerialNumber(device.getSerialNumber());
        resp.setLocation(device.getLocation());
        resp.setStatus(device.getStatus().name());
        resp.setImageUrl(device.getImageUrl());
        resp.setNotes(device.getNotes());
        resp.setLastMaintenanceDate(device.getLastMaintenanceDate());

        // Map DeviceType fields (Fetch an toàn)
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
		// 1. Lấy toàn bộ danh sách Entity từ Database
        List<Device> devices = deviceRepository.findAll();

        // 2. Sử dụng Stream để convert từng Entity sang DTO
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

        // Map thông tin từ DeviceType
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
