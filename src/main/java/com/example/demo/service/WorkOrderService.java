package com.example.demo.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.demo.dto.TechnicianResponseDTO;
import com.example.demo.enums.DeviceStatus;
import com.example.demo.enums.MaintenanceStatus;
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
                    return 2;
                })
                .sum();
    }

    public WorkOrder createWorkOrderFromRequest(Long requestId, Long manualTechId) {
        MaintenanceRequests request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu!"));

        Users assignedTech;
        if (manualTechId != null) {
            assignedTech = usersRepository.findById(manualTechId)
                    .orElseThrow(() -> new RuntimeException("Kỹ thuật viên được chỉ định không tồn tại!"));
        } else {
            assignedTech = findBestAvailableTechnicianEntity();
        }

        WorkOrder order = new WorkOrder();
        order.setDevice(request.getDevice());
        order.setRequest(request);
        order.setTechnician(assignedTech); 
        order.setStatus(WorkOrderStatus.PROGRESSING);

        request.setStatus(RequestStatus.PROGRESSING);
        request.getDevice().setStatus(DeviceStatus.MAINTENANCE);
        requestRepository.save(request);

        WorkOrder savedOrder = workOrderRepository.save(order);
        sendEmail(assignedTech, savedOrder);

        return savedOrder;
    }


    @Transactional
    public void completeWorkOrder(Long workOrderId,
                                String currentUserName,
                                String result,
                                java.math.BigDecimal cost,
                                String imgBefore, 
                                String imgAfter) 
    {
        WorkOrder order = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu công việc!"));

        if (!order.getTechnician().getUsername().equals(currentUserName)) {
            throw new RuntimeException("Bạn không được phân công phiếu này!");
        }

        order.setEvidenceBeforeUrl(imgBefore);
        order.setEvidenceAfterUrl(imgAfter);
        order.setStatus(WorkOrderStatus.COMPLETED); 
        workOrderRepository.save(order);

        MaintenanceRequests request = order.getRequest();
        if (request != null) {
            request.setStatus(RequestStatus.COMPLETED);
            requestRepository.save(request);
        }

        MaintenanceHistory history = new MaintenanceHistory();
        history.setWorkOrder(order);
        history.setDevice(order.getDevice());
        history.setTechnician(order.getTechnician());
        history.setMaintenanceDate(java.time.LocalDateTime.now());
        history.setResult(result);
        history.setCost(cost);
        
        history.setBeforeImageUrl(imgBefore);
        history.setAfterImageUrl(imgAfter);
        
        if (order.getDevice() != null && order.getDevice().getCompany() != null) {
            history.setCompanyId(order.getDevice().getCompany()); 
        }

        boolean isSchedule = (order.getSchedule() != null);

        if (isSchedule) {
            history.setStatus(MaintenanceStatus.USER_ACCEPTED); 
        } else {
            history.setStatus(MaintenanceStatus.PENDING_ACCEPTANCE); 
        }

        historyRepository.save(history);

        if (!isSchedule) {
            String recipientEmail;
            if (order.getRequest() != null && order.getRequest().getRequester() != null) {
                recipientEmail = order.getRequest().getRequester().getEmail();
            } else {
                recipientEmail = "tran559@gmail.com";
            }

            notificationService.notifyUserForAcceptance(
                order.getId(), 
                order.getDevice().getDeviceType().getTypeName(), 
                order.getTechnician().getFullName(),
                recipientEmail
            );
        } else {
            notificationService.notifyAccountantForApproval(order.getId(), order.getDevice().getDeviceType().getTypeName(), order.getTechnician().getFullName());
        }
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

        Users bestTech = findBestAvailableTechnicianEntity();

        WorkOrder order = new WorkOrder();
        order.setDevice(device);
        order.setTechnician(bestTech);
        order.setSchedule(schedule);
        order.setRequest(null);
        order.setStatus(WorkOrderStatus.PROGRESSING);
        device.setStatus(DeviceStatus.MAINTENANCE);

        WorkOrder saved = workOrderRepository.save(order);

        sendEmail(bestTech, saved);

        return saved;
    }

    
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void autoCreateWorkOrderFromSchedule() {
        List<MaintenanceSchedule> schedules = scheduleRepository.findByIsActiveTrue();
        LocalDate today = LocalDate.now();
        int createdCount = 0; 

        for (MaintenanceSchedule schedule : schedules) {
            if (!isDue(schedule, today)) continue;

            boolean exists = workOrderRepository.existsByScheduleAndStatusIn(
                        schedule,
                        List.of(WorkOrderStatus.PROGRESSING)
                    );

            if (exists) continue;

            WorkOrder newOrder = createWorkOrderFromSchedule(schedule.getId());
            if (newOrder != null) {
                createdCount++;
            }
        }

        if (createdCount > 0) {
            notificationService.sendAdminAutoSummaryReport(createdCount);
        }
    }


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
        WorkOrder order = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("WorkOrder không tồn tại!"));

        if (order.getStatus() == WorkOrderStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy lệnh đã hoàn thành!");
        }

        Users technician = order.getTechnician();
        
        order.setStatus(WorkOrderStatus.CANCELLED);
        
        if (order.getRequest() != null) {
            order.getRequest().setStatus(RequestStatus.NEW);
        }
        if (order.getDevice() != null) {
            order.getDevice().setStatus(DeviceStatus.BROKEN);
        }

        WorkOrder savedOrder = workOrderRepository.save(order);

        if (technician != null) {
            sendCancellationEmail(technician, savedOrder);
        }

        return savedOrder;
    }


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
        
        String cleanDevice = (deviceSearch != null && !deviceSearch.trim().isEmpty()) ? deviceSearch.trim() : null;
        String cleanTech = (techSearch != null && !techSearch.trim().isEmpty()) ? techSearch.trim() : null;
        String cleanUser = (username != null && !username.trim().isEmpty()) ? username.trim() : null;

        return workOrderRepository.filterWorkOrders(
                cleanDevice, 
                cleanTech, 
                status, 
                cleanUser, 
                startDate, 
                endDate
        );
    }
    
    public List<WorkOrder> getAllActiveWorkOrders() {
        return workOrderRepository.findByStatusNot(WorkOrderStatus.CANCELLED);
    }

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
        return workOrderRepository.findByTechnician_UsernameAndStatusNot(username, WorkOrderStatus.CANCELLED);
    }
    
    private boolean isDue(MaintenanceSchedule s, LocalDate today) {
        LocalDate start = s.getStartDate();
        if (today.isBefore(start)) return false;

        int cycle = s.getCycleValue();
        if (cycle <= 0) return false;

        switch (s.getScheduleType()) {
            case DAILY:
                long daysBetween = ChronoUnit.DAYS.between(start, today);
                return daysBetween % cycle == 0;

            case WEEKLY:
                long weeksBetween = ChronoUnit.WEEKS.between(start, today);
                return weeksBetween % cycle == 0 && today.getDayOfWeek() == start.getDayOfWeek();

            case MONTHLY:
                long monthsBetween = ChronoUnit.MONTHS.between(start, today);
                return monthsBetween % cycle == 0 && today.getDayOfMonth() == start.getDayOfMonth();

            case YEARLY:
                long yearsBetween = ChronoUnit.YEARS.between(start, today);
                return yearsBetween % cycle == 0 && today.getMonth() == start.getMonth() 
                       && today.getDayOfMonth() == start.getDayOfMonth();

            default:
                return false;
        }
    }


}
