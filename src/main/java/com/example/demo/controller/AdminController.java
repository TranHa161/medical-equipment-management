package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.dto.MaintenanceRequestsResponseDTO;
import com.example.demo.enums.DeviceStatus;
import com.example.demo.model.MaintenanceHistory;
import com.example.demo.model.MaintenanceRequests;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.MaintenanceHistoryRepository;
import com.example.demo.repository.MaintenanceRequestsRepository;
import com.example.demo.repository.UsersRepository;
import com.example.demo.repository.WorkOrderRepository;

@Controller
public class AdminController {

    @Autowired
    private DeviceRepository deviceRepository; 

    @Autowired
    private UsersRepository userRepository;
    
    @Autowired
    private MaintenanceRequestsRepository maintenanceRequestRepository;
    
    @Autowired
    private MaintenanceHistoryRepository maintenanceHistoryRepository;
    
    @Autowired
    private WorkOrderRepository workOrderRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 1. Lấy các chỉ số thống kê từ Database
        long totalDevices = deviceRepository.count();
        long brokenDevices = deviceRepository.countByStatus(DeviceStatus.BROKEN);
        long maintenanceDevices = deviceRepository.countByStatus(DeviceStatus.MAINTENANCE);
        long activeStaff = userRepository.countActiveOperationalStaff();

        // 2. Đưa dữ liệu vào Model để hiển thị lên Thymeleaf
        model.addAttribute("totalDevices", totalDevices);
        model.addAttribute("brokenDevices", brokenDevices);
        model.addAttribute("maintenanceDevices", maintenanceDevices);
        model.addAttribute("activeStaff", activeStaff);
        
        List<MaintenanceRequestsResponseDTO> latestRequests = 
                maintenanceRequestRepository.findTop5ByStatusOrderByCreatedAtDesc(com.example.demo.enums.RequestStatus.NEW)
                    .stream()
                    .map(this::mapRequestToResponseDTO)
                    .toList();
        model.addAttribute("latestRequests", latestRequests);
        
        List<MaintenanceHistory> recentHistory = maintenanceHistoryRepository.findTop5ByOrderByMaintenanceDateDesc();
        model.addAttribute("recentHistory", recentHistory);
        
        // 1. Lấy số kỹ thuật viên đang làm việc
        long workingTechs = workOrderRepository.countWorkingTechnicians();
        model.addAttribute("workingTechs", workingTechs);

        // 2. Tính toán % quân số sẵn sàng
        long totalTechs = userRepository.countByRole_RoleName("TECHNICIAN");
        double readiness = 0;
        if (totalTechs > 0) {
            // Công thức: % Sẵn sàng = ((Tổng - Đang bận) / Tổng) * 100
            readiness = ((double) (totalTechs - workingTechs) / totalTechs) * 100;
        }
        model.addAttribute("readinessPercent", Math.round(readiness));

        // 3. Gửi thời gian cập nhật hiện tại
        model.addAttribute("now", java.time.LocalDateTime.now());

        return "dashboard"; 
    }
    
    private MaintenanceRequestsResponseDTO mapRequestToResponseDTO(MaintenanceRequests entity) {
        MaintenanceRequestsResponseDTO response = new MaintenanceRequestsResponseDTO();
        
        // Ánh xạ các thông tin cơ bản
        response.setId(entity.getId());
        response.setDescription(entity.getDescription());
        response.setSeverity(entity.getSeverity());
        response.setStatus(entity.getStatus());
        response.setRejectionReason(entity.getRejectionReason());

        // Ánh xạ thông tin Thiết bị
        if (entity.getDevice() != null) {
            response.setDeviceId(entity.getDevice().getId());
            // Trong hàm mapRequestToResponseDTO
            if (entity.getDevice() != null && entity.getDevice().getDeviceType() != null) {
                // Thêm một khoảng trắng hoặc dấu ngoặc để dễ nhìn hơn
                String fullName = entity.getDevice().getDeviceType().getTypeName() 
                                + " - " + entity.getDevice().getSerialNumber();
                response.setDeviceName(fullName);
                }
            response.setDeviceLocation(entity.getDevice().getLocation());
        }

        // Ánh xạ thông tin Người yêu cầu
        if (entity.getRequester() != null) {
            response.setRequesterId(entity.getRequester().getId());
            response.setRequesterUsername(entity.getRequester().getUsername());
        }

        return response;
    }
}