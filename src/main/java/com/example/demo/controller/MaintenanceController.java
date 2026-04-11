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

    /**
     * UC05: Danh sách phiếu báo hỏng (Phân quyền Admin/User)
     */
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

        // Nếu Admin đang tìm kiếm, hãy trim() chuỗi trước khi gửi xuống Service
        String searchUser = (requester != null) ? requester.trim() : null;
        String filterUser = isAdmin ? searchUser : currentUsername;


        // Gọi Service lọc dữ liệu
        List<MaintenanceRequestsResponseDTO> requests = maintenanceService.filterRequests(keyword, filterUser, severity, status, date);

        model.addAttribute("requestList", requests);
        
        // Giữ lại giá trị trên form
        model.addAttribute("keyword", keyword);
        model.addAttribute("requester", requester);
        model.addAttribute("date", date);
        model.addAttribute("severity", severity != null ? severity.name() : "");
        model.addAttribute("status", status != null ? status.name() : "");

        return "requests";
    }
    
    @GetMapping("/requests/add")
    public String showAddRequestForm(Model model) {
        // 1. Phải khởi tạo đúng đối tượng Báo hỏng (Request), không dùng Schedule
        MaintenanceRequestsResponseDTO requestDTO = new MaintenanceRequestsResponseDTO();
        
        // 2. Đặt tên attribute là "requestDTO" để khớp với file HTML
        model.addAttribute("requestDTO", requestDTO);
        
        // 3. Lấy danh sách thiết bị (devices) để đổ vào dropdown
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
    
    /**
     * UC05: Xử lý lưu phiếu báo hỏng gửi từ Form
     */
    @PostMapping("/requests/save")
    @ResponseBody
    public ResponseEntity<?> saveRequest(@RequestBody MaintenanceRequestsDTO dto, Authentication authentication) {
        try {
            // 1. Lấy Username của người đang đăng nhập từ hệ thống
            String currentUsername = authentication.getName();
            
            // 2. Truyền Username vào Service để xử lý tìm User ID bên trong đó
            maintenanceService.saveRequestWithUsername(dto, currentUsername);
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/requests/reject/{id}")
    @ResponseBody // Trả về JSON để JavaScript xử lý
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


    /**
     * UC06: Xem lịch bảo trì active của thiết bị
     */
    @GetMapping("/schedules")
    public String listSchedules(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ScheduleType type,
            @RequestParam(required = false) Integer cycle,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) Boolean active,
            Model model) {

        // Parse ngày bắt đầu
        java.time.LocalDate start = (startDate != null && !startDate.isEmpty()) 
                                        ? java.time.LocalDate.parse(startDate) : null;

        List<MaintenanceScheduleResponseDTO> scheduleList = 
                maintenanceService.filterSchedules(keyword, type, cycle, start, active);

        model.addAttribute("scheduleList", scheduleList);
        
        // Đẩy lại model để giữ value trên các ô input
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type != null ? type.name() : "");
        model.addAttribute("cycle", cycle);
        model.addAttribute("startDate", startDate);
        model.addAttribute("active", active != null ? active.toString() : "");

        return "schedules";
    }
    /**
     * UC07: Thiết lập lịch bảo trì định kỳ
     */
    @GetMapping("/schedules/{deviceId}")
    public String showSetupScheduleForm(@PathVariable Integer deviceId, Model model) {
        // 1. Lấy thông tin thiết bị để hiển thị tiêu đề Form
        DeviceDTO device = deviceService.getDeviceById(deviceId);
        model.addAttribute("device", device);
        
        // 2. Kiểm tra xem thiết bị đã có lịch chưa, nếu chưa thì tạo mới DTO
        MaintenanceScheduleResponseDTO schedule = maintenanceService.getScheduleByDeviceId(deviceId)
                .orElse(new MaintenanceScheduleResponseDTO());
        
        // Đảm bảo gán deviceId vào DTO để khi lưu hệ thống biết là của máy nào
        schedule.setDeviceId(deviceId); 
        model.addAttribute("schedule", schedule);
        
        // 3. Lấy danh sách Kỹ thuật viên (Technicians) để hiện lên Dropdown phân công
        List<UsersResponseDTO> technicians = usersService.findAllTechnicians();
        model.addAttribute("technicians", technicians);
        
        // 4. Truyền thêm biến activePage để Sidebar tô màu đúng mục
        model.addAttribute("activePage", "schedules");
        
        return "schedule-form"; // Trả về file templates/admin/schedule-form.html
    }
    
    @GetMapping("/schedules/add")
    public String showAddScheduleForm(Model model) {
        // 1. Tạo một DTO rỗng để bind vào form
        MaintenanceScheduleResponseDTO schedule = new MaintenanceScheduleResponseDTO();
        schedule.setIsActive(true); // Mặc định là kích hoạt
        model.addAttribute("schedule", schedule);
        
        // 2. Lấy danh sách thiết bị để người dùng chọn máy cần lập lịch
        // Bạn nên ưu tiên lấy những máy chưa có lịch bảo trì
        List<DeviceDTO> devices = deviceService.findAllDevices(); 
        model.addAttribute("devices", devices);
        
        // 3. Lấy danh sách Kỹ thuật viên để phân công mặc định (nếu cần)
        List<UsersResponseDTO> technicians = usersService.findAllTechnicians();
        model.addAttribute("technicians", technicians);
        
        // 4. Đánh dấu mục active trên Sidebar
        model.addAttribute("activePage", "schedules");
        
        return "schedule-form";
    }
    
    
    @PostMapping("/schedules/save")
    @ResponseBody
    public ResponseEntity<?> saveSchedule(@RequestBody MaintenanceScheduleResponseDTO dto) {
        try {
            // Gọi Service để xử lý lưu dữ liệu
            MaintenanceScheduleResponseDTO savedDto = maintenanceService.saveSchedule(dto);
            return ResponseEntity.ok(savedDto);
        } catch (Exception e) {
            // Trả về lỗi 400 kèm message để hiện lên UI
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

        // 1. Gọi Service với đầy đủ 4 tham số lọc
        List<MaintenanceHistoryResponseDTO> historyList = 
                maintenanceService.getAllHistory(keyword, techName, startDate, endDate);

        // 2. Nạp dữ liệu vào Model để hiển thị lên bảng
        model.addAttribute("historyList", historyList);

        // 3. QUAN TRỌNG: Gửi lại các tham số tìm kiếm để giữ giá trị trên Form
        model.addAttribute("keyword", keyword);
        model.addAttribute("techName", techName);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        model.addAttribute("activePage", "history");

        return "history"; 
    }
    
}