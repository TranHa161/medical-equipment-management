package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.dto.DeviceDTO;
import com.example.demo.dto.MaintenanceHistoryResponseDTO;
import com.example.demo.dto.MaintenanceRequestsDTO;
import com.example.demo.dto.MaintenanceRequestsResponseDTO;
import com.example.demo.dto.MaintenanceScheduleResponseDTO;
import com.example.demo.dto.UsersResponseDTO;
import com.example.demo.enums.RequestStatus;
import com.example.demo.enums.ScheduleType;
import com.example.demo.enums.SeverityLevel;
import com.example.demo.service.DeviceService;
import com.example.demo.service.MaintenanceService;
import com.example.demo.service.UsersService;

@Controller
public class MaintenanceController {

	private final MaintenanceService maintenanceService;
    private final DeviceService deviceService;
    private final UsersService usersService;
    
    public MaintenanceController(MaintenanceService maintenanceService,
                                 DeviceService deviceService,
                                 UsersService usersService) {
        this.maintenanceService = maintenanceService;
        this.deviceService = deviceService;
        this.usersService = usersService;
    }

    @GetMapping("/requests")
    public String listRequests(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String requester,
            @RequestParam(required = false) SeverityLevel severity,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date, // 👈 Thêm dòng này
            Authentication auth, Model model) {

        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                              .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        
        String searchUser = (requester != null) ? requester.trim() : null;
        String filterUser = isAdmin ? searchUser : currentUsername;

        List<MaintenanceRequestsResponseDTO> requests = maintenanceService.filterRequests(keyword, filterUser, severity, status, date);

        model.addAttribute("requestList", requests);
        
        model.addAttribute("keyword", keyword);
        model.addAttribute("requester", requester);
        model.addAttribute("date", date);
        model.addAttribute("severity", severity != null ? severity.name() : "");
        model.addAttribute("status", status != null ? status.name() : "");

        return "requests";
    }
    
    @GetMapping("/requests/add")
    public String showAddRequestForm(Model model) {
        MaintenanceRequestsResponseDTO requestDTO = new MaintenanceRequestsResponseDTO();
        
        model.addAttribute("requestDTO", requestDTO);
        
        model.addAttribute("devices", deviceService.findAllDevices());
        
        model.addAttribute("activePage", "requests");
        return "request-form";
    }

    @GetMapping("/requests/edit/{id}")
    public String showEditReportForm(@PathVariable Long id, Model model) {
        MaintenanceRequestsResponseDTO request = maintenanceService.getRequestById(id);
        model.addAttribute("requestDTO", request);
        
        model.addAttribute("devices", deviceService.findAllDevices());
        model.addAttribute("activePage", "requests");
        return "request-form";
    }
    
    @PostMapping("/requests/update/{id}")
    @ResponseBody
    public ResponseEntity<?> updateRequest(@PathVariable Long id, @RequestBody MaintenanceRequestsDTO dto) {
        maintenanceService.updateRequest(id, dto);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/requests/save")
    @ResponseBody
    public ResponseEntity<?> saveRequest(@RequestBody MaintenanceRequestsDTO dto, Authentication authentication) {
        try {
            String currentUsername = authentication.getName();

            maintenanceService.saveRequestWithUsername(dto, currentUsername);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/requests/reject/{id}")
    @ResponseBody
    public ResponseEntity<?> rejectRequest(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String reason = payload.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Lý do từ chối không được để trống!");
            }
            maintenanceService.rejectRequest(id, reason);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/schedules")
    public String listSchedules(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ScheduleType type,
            @RequestParam(required = false) Integer cycle,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) Boolean active,
            Model model) {

        java.time.LocalDate start = (startDate != null && !startDate.isEmpty()) 
                                        ? java.time.LocalDate.parse(startDate) : null;

        List<MaintenanceScheduleResponseDTO> scheduleList = 
                maintenanceService.filterSchedules(keyword, type, cycle, start, active);

        model.addAttribute("scheduleList", scheduleList);
        
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type != null ? type.name() : "");
        model.addAttribute("cycle", cycle);
        model.addAttribute("startDate", startDate);
        model.addAttribute("active", active != null ? active.toString() : "");

        return "schedules";
    }

    @GetMapping("/schedules/{deviceId}")
    public String showSetupScheduleForm(@PathVariable Integer deviceId, Model model) {
        DeviceDTO device = deviceService.getDeviceById(deviceId);
        model.addAttribute("device", device);
        
        MaintenanceScheduleResponseDTO schedule = maintenanceService.getScheduleByDeviceId(deviceId)
                .orElse(new MaintenanceScheduleResponseDTO());
        
        schedule.setDeviceId(deviceId); 
        model.addAttribute("schedule", schedule);
        
        List<UsersResponseDTO> technicians = usersService.findAllTechnicians();
        model.addAttribute("technicians", technicians);
        
        model.addAttribute("activePage", "schedules");
        
        return "schedule-form";
    }
    
    @GetMapping("/schedules/add")
    public String showAddScheduleForm(Model model) {
        MaintenanceScheduleResponseDTO schedule = new MaintenanceScheduleResponseDTO();
        schedule.setIsActive(true);
        model.addAttribute("schedule", schedule);

        List<DeviceDTO> devices = deviceService.findAllDevices(); 
        model.addAttribute("devices", devices);
        
        List<UsersResponseDTO> technicians = usersService.findAllTechnicians();
        model.addAttribute("technicians", technicians);
        
        model.addAttribute("activePage", "schedules");
        
        return "schedule-form";
    }
    
    
    @PostMapping("/schedules/save")
    @ResponseBody
    public ResponseEntity<?> saveSchedule(@RequestBody MaintenanceScheduleResponseDTO dto) {
        try {
            MaintenanceScheduleResponseDTO savedDto = maintenanceService.saveSchedule(dto);
            return ResponseEntity.ok(savedDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/history")
    public String viewHistory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String techName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        List<MaintenanceHistoryResponseDTO> historyList = 
                maintenanceService.getAllHistory(keyword, techName, startDate, endDate);

        model.addAttribute("historyList", historyList);

        model.addAttribute("keyword", keyword);
        model.addAttribute("techName", techName);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        model.addAttribute("activePage", "history");

        return "history"; 
    }
    
    @PostMapping("/history/accountant-approve/{historyId}")
    @ResponseBody
    public ResponseEntity<String> accountantApproveHistory(
            @PathVariable Long historyId,
            Authentication auth) {
        
        try {
            String currentUsername = auth.getName();

            maintenanceService.accountantApproveAndInvoiced(historyId, currentUsername);
            
            return ResponseEntity.ok("Kế toán đã duyệt thành công! Phiếu đã được đưa vào danh sách chờ quyết toán.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi duyệt hóa đơn: " + e.getMessage());
        }
    }

    @PostMapping("/history/user-accept/{historyId}")
    @ResponseBody
    public ResponseEntity<String> userAcceptMaintenance(
            @PathVariable Long historyId) {
        try {
            maintenanceService.userAccept(historyId);
            return ResponseEntity.ok("Xác nhận nghiệm thu thiết bị thành công! Đang chờ kế toán duyệt chi phí.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}