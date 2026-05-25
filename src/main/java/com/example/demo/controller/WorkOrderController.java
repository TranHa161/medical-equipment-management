package com.example.demo.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.WorkOrderDTO;
import com.example.demo.enums.WorkOrderStatus;
import com.example.demo.model.WorkOrder;
import com.example.demo.service.WorkOrderService;
import com.example.demo.service.S3Service;

@Controller
@RequestMapping("/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final S3Service s3Service;

    public WorkOrderController(WorkOrderService workOrderService, S3Service s3Service) {
        this.workOrderService = workOrderService;
        this.s3Service = s3Service;
    }

    @GetMapping()
    public String listWorkOrders(
            @RequestParam(required = false) String deviceSearch,
            @RequestParam(required = false) String techSearch,  
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) String startDate, 
            @RequestParam(required = false) String endDate, 
            Authentication auth, Model model) {

        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                              .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String filterUsername = isAdmin ? null : currentUsername;
        String finalTechSearch = isAdmin ? techSearch : null;
        
        java.time.LocalDateTime start = (startDate != null && !startDate.isEmpty()) 
                                        ? java.time.LocalDate.parse(startDate).atStartOfDay() : null;
        java.time.LocalDateTime end = (endDate != null && !endDate.isEmpty()) 
                                      ? java.time.LocalDate.parse(endDate).atTime(23, 59, 59) : null;

        List<WorkOrder> orders = workOrderService.filterWorkOrders(
                deviceSearch, finalTechSearch, status, filterUsername, start, end);

        model.addAttribute("orderList", orders.stream().map(this::mapToDTO).toList());
        
        model.addAttribute("deviceSearch", deviceSearch);
        model.addAttribute("techSearch", finalTechSearch);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("status", status != null ? status.name() : "");

        return "work-orders";
    }

    @GetMapping("/from-request/{requestId}")
    public String createFromRequest(
            @PathVariable Long requestId,
            @RequestParam(required = false) Long techId) {

        workOrderService.createWorkOrderFromRequest(requestId, techId);

        return "redirect:/requests";
    }
    
    @PostMapping("/cancel/{id}")
    @ResponseBody
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            WorkOrder order = workOrderService.cancelWorkOrder(id);
            
            return ResponseEntity.ok(mapToDTO(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/complete/{workOrderId}")
    @ResponseBody
    public ResponseEntity<String> complete(
            @PathVariable Long workOrderId,
            @RequestParam("result") String result,
            @RequestParam("cost") String costStr,
            @RequestParam(value = "imgBefore", required = false) MultipartFile imgBefore,
            @RequestParam(value = "imgAfter", required = false) MultipartFile imgAfter,
            Authentication auth) {

        try {
            String currentUsername = auth.getName();
            BigDecimal cost = new BigDecimal(costStr);

            // 1. Upload ảnh lên S3 và lấy URL (nếu có ảnh gửi lên)
            String evidenceBeforeUrl = null;
            if (imgBefore != null && !imgBefore.isEmpty()) {
                evidenceBeforeUrl = s3Service.uploadFile(imgBefore);
            }

            String evidenceAfterUrl = null;
            if (imgAfter != null && !imgAfter.isEmpty()) {
                evidenceAfterUrl = s3Service.uploadFile(imgAfter);
            }

            // 2. Gọi service xử lý logic với các URL đã có
            workOrderService.completeWorkOrder(
                workOrderId, 
                currentUsername, 
                result, 
                cost, 
                evidenceBeforeUrl, 
                evidenceAfterUrl
            );

            return ResponseEntity.ok("Đã nộp báo cáo hoàn thành, đang chờ nghiệm thu!");

        } catch (Exception e) {
            // Log lỗi tại đây
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Lỗi xử lý: " + e.getMessage());
        }
    }

    private WorkOrderDTO mapToDTO(WorkOrder order) {
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setId(order.getId());
        
        if (order.getDevice() != null) {
            dto.setDeviceId(order.getDevice().getId());
            String deviceName = order.getDevice().getDeviceType().getTypeName() 
                              + " - " + order.getDevice().getSerialNumber();
            dto.setDeviceName(deviceName);
        }

        if (order.getTechnician() != null) {
            dto.setTechnicianId(order.getTechnician().getId());
            dto.setTechnicianName(order.getTechnician().getFullName());
        }

        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }
}