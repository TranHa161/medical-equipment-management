package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.DeviceDTO;
import com.example.demo.dto.DeviceDetailResponseDTO;
import com.example.demo.model.Device;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceTypeRepository;
import com.example.demo.service.DeviceService;

@Controller
@RequestMapping("/devices")
public class DeviceController {

    @Autowired private DeviceService deviceService;
    @Autowired private DeviceTypeRepository typeRepository;
    @Autowired private DeviceRepository deviceRepository;

    // --- CÁC ENDPOINT HIỂN THỊ GIAO DIỆN (GET) ---

    @GetMapping
    public String listDevices(Model model, 
                              @RequestParam(required = false) String keyword,
                              Authentication auth) {
        List<DeviceDTO> devices = deviceService.getDevicesForUser(auth, keyword);
        model.addAttribute("deviceList", devices);
        model.addAttribute("keyword", keyword);
        return "devices"; 
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        DeviceDTO device = new DeviceDTO();
        device.setStatus("ACTIVE");
        model.addAttribute("device", device);
        model.addAttribute("deviceTypes", typeRepository.findAll());
        return "device-form"; 
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Device entity = deviceRepository.findById(id).orElseThrow();
        
        // Tạo DTO "phẳng" từ Entity
        DeviceDTO dto = new DeviceDTO();
        dto.setId(entity.getId());
        dto.setSerialNumber(entity.getSerialNumber());
        dto.setLocation(entity.getLocation());
        dto.setNotes(entity.getNotes());
        dto.setStatus(entity.getStatus().name());
        dto.setImageUrl(entity.getImageUrl()); // Để hiện ảnh cũ

        if (entity.getDeviceType() != null) {
            dto.setTypeId(entity.getDeviceType().getId());
            // Lấy link PDF từ Entity DeviceType bỏ vào trường manualUrl của DTO
            dto.setManualUrl(entity.getDeviceType().getManualUrl()); 
        }

        model.addAttribute("device", dto); // Truyền DTO ra ngoài
        model.addAttribute("deviceTypes", typeRepository.findAll());
        return "device-form";
    }

    @GetMapping("/detail/{id}")
    public String showDetail(@PathVariable Integer id, Model model) {
        // Lấy DTO đầy đủ thông tin nhất (bao gồm cả Manufacturer, Model, ManualUrl...)
        DeviceDetailResponseDTO deviceDetail = deviceService.getDeviceDetail(id);
        model.addAttribute("device", deviceDetail);
        return "device-detail";
    }

    // --- CÁC ENDPOINT XỬ LÝ DỮ LIỆU (POST) ---

    // 1. Thêm mới thiết bị (Hỗ trợ Ảnh + Tài liệu hướng dẫn)
    @PostMapping("/add")
    public ResponseEntity<?> createDevice(
            @ModelAttribute DeviceDTO dto, 
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "manualFile", required = false) MultipartFile manualFile) { 
        try {
            // Service trả về DetailDTO để Frontend có thể hiển thị thông báo tóm tắt
            DeviceDetailResponseDTO created = deviceService.createDevice(dto, imageFile, manualFile);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 2. Cập nhật thiết bị (Phân quyền + Hỗ trợ Ảnh & Tài liệu)
    @PostMapping("/edit/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id, 
            @ModelAttribute DeviceDTO dto, 
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "manualFile", required = false) MultipartFile manualFile, 
            Authentication auth) { 
        try {
            // Truyền ID và Auth để check quyền sở hữu/phân công
            DeviceDetailResponseDTO updated = deviceService.updateDevice(id, dto, imageFile, manualFile, auth);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}