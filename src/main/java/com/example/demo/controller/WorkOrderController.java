package com.example.demo.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.dto.WorkOrderDTO;
import com.example.demo.enums.WorkOrderStatus;
import com.example.demo.model.WorkOrder;
import com.example.demo.service.WorkOrderService;

@Controller
@RequestMapping("/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
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

        // 1. Phân quyền truy cập dữ liệu
        String filterUsername = isAdmin ? null : currentUsername;
        String finalTechSearch = isAdmin ? techSearch : null;
        
        // Chuyển đổi String từ giao diện sang LocalDateTime để truyền vào Repository
        java.time.LocalDateTime start = (startDate != null && !startDate.isEmpty()) 
                                        ? java.time.LocalDate.parse(startDate).atStartOfDay() : null;
        java.time.LocalDateTime end = (endDate != null && !endDate.isEmpty()) 
                                      ? java.time.LocalDate.parse(endDate).atTime(23, 59, 59) : null;

        // 2. Gọi Service với đầy đủ các tham số lọc, bao gồm cả ngày tháng
        // Lưu ý: Trân Trân cần cập nhật signature của hàm filterWorkOrders trong Service nhé
        List<WorkOrder> orders = workOrderService.filterWorkOrders(
                deviceSearch, finalTechSearch, status, filterUsername, start, end);

        // 3. Đưa dữ liệu đã map sang DTO lên giao diện
        model.addAttribute("orderList", orders.stream().map(this::mapToDTO).toList());
        
        // 4. Giữ lại các giá trị lọc trên Form (Để các ô mm/dd/yyyy không bị mất giá trị sau khi Load)
        model.addAttribute("deviceSearch", deviceSearch);
        model.addAttribute("techSearch", finalTechSearch);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("status", status != null ? status.name() : "");

        return "work-orders";
    }
    
    /* =====================================================
     * ADMIN LOGIC
     * ===================================================== */

    /**
     * Admin duyệt báo hỏng.
     * Hỗ trợ: /work-orders/from-request/5 (Tự động chọn Best Tech)
     * Hoặc: /work-orders/from-request/5?techId=10 (Admin chỉ định người làm)
     */
    @GetMapping("/from-request/{requestId}")
    public String createFromRequest(
            @PathVariable Long requestId,
            @RequestParam(required = false) Long techId) {

        // Gọi Service xử lý logic Hybrid (Thủ công hoặc Tự động)
        workOrderService.createWorkOrderFromRequest(requestId, techId);

        // Sau khi duyệt, quay lại trang danh sách phiếu báo hỏng
        return "redirect:/requests";
    }
    
    @PostMapping("/cancel/{id}")
    @ResponseBody // Để trả về JSON cho JavaScript xử lý thông báo thành công
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        try {
            WorkOrder order = workOrderService.cancelWorkOrder(id);
            
            // Trả về DTO sau khi đã cập nhật
            return ResponseEntity.ok(mapToDTO(order));
        } catch (Exception e) {
            // Trả về thông báo lỗi nếu không hủy được (ví dụ: phiếu đã hoàn thành)
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /* =====================================================
     * TECHNICIAN LOGIC
     * ===================================================== */

    /**
     * Kỹ thuật viên: Hoàn thành phiếu công việc
     */
    @PostMapping("/complete/{workOrderId}")
    @ResponseBody
    public ResponseEntity<String> complete(
            @PathVariable Long workOrderId,
            @RequestBody Map<String, Object> payload, // Nhận dữ liệu từ SweetAlert2
            Authentication auth) {

        // 1. Lấy thông tin người dùng đang đăng nhập
        String currentUsername = auth.getName();
        
        // 2. Lấy kết quả và chi phí từ payload
        String result = (String) payload.get("result");
        BigDecimal cost = new BigDecimal(payload.get("cost").toString());

        // 3. Gọi Service xử lý nghiệp vụ (Cập nhật Status máy, lưu lịch sử)
        workOrderService.completeWorkOrder(workOrderId, currentUsername, result, cost);

        return ResponseEntity.ok("Hoàn thành công việc thành công!");
    }

    /* =====================================================
     * HELPER MAPPERS (Nên tách ra Class riêng nếu dự án lớn hơn)
     * ===================================================== */

    private WorkOrderDTO mapToDTO(WorkOrder order) {
        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setId(order.getId());
        
        // Định dạng tên máy theo kiểu "Tên loại (Serial)" đồng bộ với Dashboard
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