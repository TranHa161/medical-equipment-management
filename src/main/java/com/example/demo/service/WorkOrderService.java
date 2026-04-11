package com.example.demo.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.dto.TechnicianResponseDTO;
import com.example.demo.enums.DeviceStatus;
import com.example.demo.enums.RequestStatus;
import com.example.demo.enums.WorkOrderStatus;
import com.example.demo.model.Device;
import com.example.demo.model.MaintenanceHistory;
import com.example.demo.model.MaintenanceRequests;
import com.example.demo.model.MaintenanceSchedule;
import com.example.demo.model.Users;
import com.example.demo.model.WorkOrder;
import com.example.demo.repository.MaintenanceHistoryRepository;
import com.example.demo.repository.MaintenanceRequestsRepository;
import com.example.demo.repository.MaintenanceScheduleRepository;
import com.example.demo.repository.UsersRepository;
import com.example.demo.repository.WorkOrderRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final UsersRepository usersRepository;
    private final MaintenanceRequestsRepository requestRepository;
    private final MaintenanceHistoryRepository historyRepository;
    private final NotificationService notificationService;
    private final MaintenanceScheduleRepository scheduleRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            UsersRepository usersRepository,
                            MaintenanceRequestsRepository requestRepository,
                            MaintenanceHistoryRepository historyRepository,
                            NotificationService notificationService,
                            MaintenanceScheduleRepository scheduleRepository) {
        this.workOrderRepository = workOrderRepository;
        this.usersRepository = usersRepository;
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * THUẬT TOÁN: Weighted Scoring - chọn kỹ thuật viên rảnh nhất
     */
    public TechnicianResponseDTO findBestAvailableTechnician() {
        Users tech = findBestAvailableTechnicianEntity();

        return TechnicianResponseDTO.builder()
                .id(tech.getId())
                .username(tech.getUsername())
                .fullName(tech.getFullName())
                .email(tech.getEmail())
                .build();
    }

    private int calculateWorkloadScore(Users tech) {

        return workOrderRepository
                .findByTechnicianAndStatus(tech, WorkOrderStatus.PROGRESSING)
                .stream()
                .mapToInt(order -> {
                    if (order.getRequest() != null) {
                        switch (order.getRequest().getSeverity()) {
                            case URGENT: return 4;
                            case HIGH:   return 3;
                            case MEDIUM: return 2;
                            default:     return 1;
                        }
                    }
                    return 2; // bảo trì định kỳ
                })
                .sum();
    }

    /**
     * UC08: Admin duyệt và có thể chỉ định Kỹ thuật viên cụ thể
     */
    public WorkOrder createWorkOrderFromRequest(Long requestId, Long manualTechId) {
        // 1. Kiểm tra tồn tại
        MaintenanceRequests request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu!"));

        // 2. Quyết định chọn ai: Nếu Admin chọn người thì dùng, không thì chạy thuật toán
        Users assignedTech;
        if (manualTechId != null) {
            assignedTech = usersRepository.findById(manualTechId)
                    .orElseThrow(() -> new RuntimeException("Kỹ thuật viên được chỉ định không tồn tại!"));
        } else {
            assignedTech = findBestAvailableTechnicianEntity(); // Chạy thuật toán tự động
        }

        // 3. Tạo WorkOrder (giữ nguyên logic cũ)
        WorkOrder order = new WorkOrder();
        order.setDevice(request.getDevice());
        order.setRequest(request);
        order.setTechnician(assignedTech); 
        order.setStatus(WorkOrderStatus.PROGRESSING);

        request.setStatus(RequestStatus.PROGRESSING);
        request.getDevice().setStatus(DeviceStatus.MAINTENANCE);
        requestRepository.save(request);

        WorkOrder savedOrder = workOrderRepository.save(order);
        sendEmail(assignedTech, savedOrder); // Gửi mail cho người được chọn

        return savedOrder;
    }


    /**
     * UC09: Kỹ thuật viên hoàn thành công việc
     */
    public void completeWorkOrder(Long workOrderId,
                                  String currentUserName,
                                  String result,
                                  java.math.BigDecimal cost) {

        WorkOrder order = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu công việc!"));

        if (!order.getTechnician().getUsername().equals(currentUserName)) {
            throw new RuntimeException("Bạn không được phân công phiếu này!");
        }

        // Hoàn thành WorkOrder
        order.setStatus(WorkOrderStatus.COMPLETED);
        workOrderRepository.save(order);

        // Đóng Request (nếu có)
        MaintenanceRequests request = order.getRequest();
        if (request != null) {
            request.setStatus(RequestStatus.COMPLETED);
            requestRepository.save(request);
        }

        // Cập nhật trạng thái thiết bị
        Device device = order.getDevice();
        device.setStatus(DeviceStatus.ACTIVE);
        device.setLastMaintenanceDate(LocalDate.now());

        // Lưu lịch sử bảo trì
        MaintenanceHistory history = new MaintenanceHistory();
        history.setWorkOrder(order);
        history.setDevice(device);
        history.setTechnician(order.getTechnician());
        history.setMaintenanceDate(java.time.LocalDateTime.now());
        history.setResult(result);
        history.setCost(cost);

        historyRepository.save(history);
    }
    
    public WorkOrder createWorkOrderFromSchedule(Long scheduleId) {

        MaintenanceSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch bảo trì!"));

        if (!schedule.getIsActive()) {
            throw new RuntimeException("Lịch bảo trì không còn hiệu lực!");
        }

        Device device = schedule.getDevice();

        if (device.getStatus() != DeviceStatus.ACTIVE) {
            throw new RuntimeException("Thiết bị không sẵn sàng bảo trì!");
        }

        // chọn kỹ thuật viên rảnh nhất
        Users bestTech = findBestAvailableTechnicianEntity();

        WorkOrder order = new WorkOrder();
        order.setDevice(device);
        order.setTechnician(bestTech);
        order.setSchedule(schedule);      // ⭐ liên kết schedule
        order.setRequest(null);            // ⭐ không có request
        order.setStatus(WorkOrderStatus.PROGRESSING);
        device.setStatus(DeviceStatus.MAINTENANCE);

        WorkOrder saved = workOrderRepository.save(order);

        // gửi mail
        sendEmail(bestTech, saved);

        return saved;
    }

    
    @Scheduled(cron = "0 0 1 * * ?") // Chạy tự động lúc 1h sáng hàng ngày
    @Transactional // Đảm bảo tính toàn vẹn dữ liệu khi tạo nhiều phiếu cùng lúc
    public void autoCreateWorkOrderFromSchedule() {
        // 1. Lấy tất cả lịch bảo trì đang còn hiệu lực
        List<MaintenanceSchedule> schedules = scheduleRepository.findByIsActiveTrue();
        LocalDate today = LocalDate.now();
        int createdCount = 0; 

        for (MaintenanceSchedule schedule : schedules) {
            // 2. Kiểm tra xem thiết bị đã đến ngày bảo trì chưa
            if (!isDue(schedule, today)) continue;

            // 3. Kiểm tra xem thiết bị này đã có phiếu "Đang xử lý" chưa để tránh tạo trùng
            boolean exists = workOrderRepository.existsByScheduleAndStatusIn(
                        schedule,
                        List.of(WorkOrderStatus.PROGRESSING)
                    );

            if (exists) continue;

            // 4. Tạo WorkOrder và tăng biến đếm
            // Hàm này bên trong đã có logic đổi trạng thái máy sang MAINTENANCE
            WorkOrder newOrder = createWorkOrderFromSchedule(schedule.getId());
            if (newOrder != null) {
                createdCount++;
            }
        }

        // 5. Gửi báo cáo tổng hợp cho Admin qua NotificationService
        if (createdCount > 0) {
            // Phải gọi qua 'notificationService' đã được inject vào
            notificationService.sendAdminAutoSummaryReport(createdCount);
        }
    }


    /**
     * Gửi email thông báo (không ảnh hưởng transaction chính)
     */
    @org.springframework.scheduling.annotation.Async
    public void sendEmail(Users bestTech, WorkOrder order) {
        try {
            notificationService.sendWorkOrderNotification(
                    bestTech.getEmail(),
                    order.getDevice().getSerialNumber(),
                    "Bạn có công việc mới được phân công.",
                    false
            );
        } catch (Exception e) {
            System.err.println("Mail error: " + e.getMessage());
        }
    }
    
    @Transactional
    public WorkOrder cancelWorkOrder(Long workOrderId) {
        // 1. Tìm và kiểm tra WorkOrder
        WorkOrder order = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder không tồn tại!"));

        if (order.getStatus() == WorkOrderStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy lệnh đã hoàn thành!");
        }

        // 2. Lưu lại thông tin kỹ thuật viên trước khi xử lý (để gửi mail)
        Users technician = order.getTechnician();

        // 3. Thực hiện Soft Delete và hoàn tác trạng thái
        order.setStatus(WorkOrderStatus.CANCELLED);
        
        if (order.getRequest() != null) {
            order.getRequest().setStatus(RequestStatus.NEW); // Trả về hàng đợi duyệt
        }
        if (order.getDevice() != null) {
            order.getDevice().setStatus(DeviceStatus.BROKEN); // Máy vẫn đang hỏng
        }

        WorkOrder savedOrder = workOrderRepository.save(order);

        // 4. GỬI MAIL THÔNG BÁO HỦY
        if (technician != null) {
            sendCancellationEmail(technician, savedOrder);
        }

        return savedOrder;
    }

    /**
     * Gửi email thông báo hủy công việc (chạy bất đồng bộ)
     */
    @org.springframework.scheduling.annotation.Async
    public void sendCancellationEmail(Users tech, WorkOrder order) {
        try {
            String deviceName = order.getDevice().getDeviceType().getTypeName() 
                              + " (" + order.getDevice().getSerialNumber() + ")";
            
            String message = String.format(
                "Chào %s, lệnh sửa chữa thiết bị [%s] của bạn đã bị hủy bởi Quản trị viên. " +
                "Vui lòng kiểm tra lại danh sách công việc trên hệ thống.",
                tech.getFullName(), deviceName
            );

            notificationService.sendWorkOrderNotification(
                tech.getEmail(),
                order.getDevice().getSerialNumber(),
                message,
                true
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail thông báo hủy: " + e.getMessage());
        }
    }
    
    public List<WorkOrder> filterWorkOrders(
            String deviceSearch, 
            String techSearch, 
            WorkOrderStatus status, 
            String username,
            java.time.LocalDateTime startDate, 
            java.time.LocalDateTime endDate) {   
        
        // 1. Tiền xử lý các chuỗi String (Giữ nguyên logic IS NULL OR ...)
        String cleanDevice = (deviceSearch != null && !deviceSearch.trim().isEmpty()) ? deviceSearch.trim() : null;
        String cleanTech = (techSearch != null && !techSearch.trim().isEmpty()) ? techSearch.trim() : null;
        String cleanUser = (username != null && !username.trim().isEmpty()) ? username.trim() : null;

        // 2. Gọi Repository thực hiện truy vấn với đầy đủ 6 tham số
        // Lưu ý: startDate và endDate đã được Controller parse sang LocalDateTime nên truyền trực tiếp
        return workOrderRepository.filterWorkOrders(
                cleanDevice, 
                cleanTech, 
                status, 
                cleanUser, 
                startDate, 
                endDate
        );
    }
    
    // Lấy tất cả WorkOrder chưa bị hủy (dành cho Admin)
    public List<WorkOrder> getAllActiveWorkOrders() {
        return workOrderRepository.findByStatusNot(WorkOrderStatus.CANCELLED);
    }

    // Lấy WorkOrder của 1 technician (chưa bị hủy)
    public List<WorkOrder> getWorkOrdersByTechnician(Long technicianId) {
        return workOrderRepository.findByTechnicianIdAndStatusNot(technicianId, WorkOrderStatus.CANCELLED);
    }
    
    private Users findBestAvailableTechnicianEntity() {

        List<Users> technicians = usersRepository.findByRole_RoleName("TECHNICIAN");
        if (technicians.isEmpty()) {
            throw new RuntimeException("Không có kỹ thuật viên!");
        }

        return technicians.stream()
                .min(Comparator.comparingInt(this::calculateWorkloadScore))
                .orElseThrow();
    }
    
    public List<WorkOrder> getWorkOrdersByTechnicianUsername(String username) {
        // Tìm User trước để lấy ID hoặc gọi trực tiếp từ Repository nếu có hỗ trợ
        return workOrderRepository.findByTechnician_UsernameAndStatusNot(username, WorkOrderStatus.CANCELLED);
    }
    
    private boolean isDue(MaintenanceSchedule s, LocalDate today) {
        LocalDate start = s.getStartDate();
        if (today.isBefore(start)) return false;

        int cycle = s.getCycleValue();
        if (cycle <= 0) return false; // Tránh lỗi chia cho 0

        switch (s.getScheduleType()) {
            case DAILY:
                // Tính tổng số ngày thực tế giữa 2 mốc thời gian
                long daysBetween = ChronoUnit.DAYS.between(start, today);
                return daysBetween % cycle == 0;

            case WEEKLY:
                // Cách X tuần bảo trì 1 lần (1 tuần = 7 ngày)
                long weeksBetween = ChronoUnit.WEEKS.between(start, today);
                return weeksBetween % cycle == 0 && today.getDayOfWeek() == start.getDayOfWeek();

            case MONTHLY:
                // Cách X tháng bảo trì 1 lần vào cùng 1 ngày trong tháng
                long monthsBetween = ChronoUnit.MONTHS.between(start, today);
                return monthsBetween % cycle == 0 && today.getDayOfMonth() == start.getDayOfMonth();

            case YEARLY:
                // Cách X năm bảo trì 1 lần vào đúng ngày đó
                long yearsBetween = ChronoUnit.YEARS.between(start, today);
                return yearsBetween % cycle == 0 && today.getMonth() == start.getMonth() 
                       && today.getDayOfMonth() == start.getDayOfMonth();

            default:
                return false;
        }
    }


}
