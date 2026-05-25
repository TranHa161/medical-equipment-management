package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.demo.dto.MaintenanceHistoryResponseDTO;
import com.example.demo.dto.MaintenanceRequestsDTO;
import com.example.demo.dto.MaintenanceRequestsResponseDTO;
import com.example.demo.dto.MaintenanceScheduleResponseDTO;
import com.example.demo.enums.DeviceStatus;
import com.example.demo.enums.MaintenanceStatus;
import com.example.demo.enums.RequestStatus;
import com.example.demo.enums.ScheduleType;
import com.example.demo.enums.SeverityLevel;
import com.example.demo.model.BulkInvoice;
import com.example.demo.model.Company;
import com.example.demo.model.Device;
import com.example.demo.model.MaintenanceHistory;
import com.example.demo.model.MaintenanceRequests;
import com.example.demo.model.MaintenanceSchedule;
import com.example.demo.model.Users;
import com.example.demo.model.WorkOrder;
import com.example.demo.repository.BulkInvoiceRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.MaintenanceHistoryRepository;
import com.example.demo.repository.MaintenanceRequestsRepository;
import com.example.demo.repository.MaintenanceScheduleRepository;
import com.example.demo.repository.UsersRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class MaintenanceService {

    private final MaintenanceRequestsRepository requestRepository;
    private final DeviceRepository deviceRepository;
    private final UsersRepository usersRepository;
    private final MaintenanceScheduleRepository scheduleRepository;
    private final MaintenanceHistoryRepository historyRepository;
    private final BulkInvoiceRepository bulkInvoiceRepository;
    
    public MaintenanceService(MaintenanceRequestsRepository requestRepository,
                              DeviceRepository deviceRepository,
                              UsersRepository usersRepository,
                              MaintenanceScheduleRepository scheduleRepository, BulkInvoiceRepository bulkInvoiceRepository, MaintenanceHistoryRepository historyRepository) {
        this.requestRepository = requestRepository;
        this.deviceRepository = deviceRepository;
        this.usersRepository = usersRepository;
        this.scheduleRepository = scheduleRepository;
		this.historyRepository = historyRepository;
		this.bulkInvoiceRepository = bulkInvoiceRepository;
    }

    public MaintenanceRequestsResponseDTO createFailureReport(MaintenanceRequestsDTO dto) {

        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Thiết bị không tồn tại!"));

        Users requester = usersRepository.findById(dto.getRequesterId())
                .orElseThrow(() -> new RuntimeException("Người dùng không hợp lệ!"));

        device.setStatus(DeviceStatus.MAINTENANCE);
        deviceRepository.save(device);

        MaintenanceRequests request = new MaintenanceRequests();
        request.setDevice(device);
        request.setRequester(requester);
        request.setDescription(dto.getDescription());
        request.setSeverity(dto.getSeverity());
        request.setStatus(RequestStatus.PROGRESSING);

        MaintenanceRequests saved = requestRepository.save(request);
        
        String fullDeviceName = (device.getDeviceType() != null ? device.getDeviceType().getTypeName() : "N/A") 
                                + " - " + device.getSerialNumber();

        MaintenanceRequestsResponseDTO response = new MaintenanceRequestsResponseDTO();
        
        response.setId(saved.getId());
        response.setDeviceId(device.getId());
        response.setDeviceName(fullDeviceName);
        response.setRequesterId(requester.getId());
        response.setRequesterUsername(requester.getUsername());
        response.setDescription(saved.getDescription());
        response.setSeverity(saved.getSeverity());
        response.setStatus(saved.getStatus());

        return response;
    }
    
    @Transactional
    public MaintenanceRequestsResponseDTO updateRequest(Long id, MaintenanceRequestsDTO dto) {
        MaintenanceRequests request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu yêu cầu ID: " + id));

        if (request.getStatus() != RequestStatus.NEW) {
            throw new RuntimeException("Không thể chỉnh sửa phiếu đã được duyệt hoặc từ chối!");
        }

        request.setDescription(dto.getDescription());
        request.setSeverity(dto.getSeverity());

        MaintenanceRequests saved = requestRepository.save(request);
        return mapRequestToResponseDTO(saved);
    }
    
    public MaintenanceRequestsResponseDTO getRequestById(Long id) {
        MaintenanceRequests request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu yêu cầu ID: " + id));

        return mapRequestToResponseDTO(request);
    }
    
    public List<MaintenanceRequestsResponseDTO> searchByDeviceSerial(String keyword) {
        List<MaintenanceRequests> requests;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            requests = requestRepository.findByDevice_SerialNumberContainingIgnoreCase(keyword);
        } else {
            requests = requestRepository.findAll();
        }
        
        return requests.stream()
                .map(this::mapRequestToResponseDTO)
                .filter(Objects::nonNull)
                .toList();
    }

    public Optional<MaintenanceScheduleResponseDTO> getDeviceActiveSchedule(Integer deviceId) {
        return scheduleRepository
                .findByDevice_IdAndIsActiveTrue(deviceId)
                .map(this::mapScheduleToDTO);
    }

    public MaintenanceScheduleResponseDTO setupSchedule(Integer deviceId, MaintenanceSchedule dto) {

        if (dto.getStartDate().isBefore(java.time.LocalDate.now())) {
            throw new RuntimeException("Ngày bắt đầu không hợp lệ!");
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Thiết bị không tồn tại!"));

        MaintenanceSchedule schedule = scheduleRepository
                .findByDevice_IdAndIsActiveTrue(deviceId)
                .orElse(new MaintenanceSchedule());

        schedule.setDevice(device);
        schedule.setScheduleType(dto.getScheduleType());
        schedule.setCycleValue(dto.getCycleValue());
        schedule.setStartDate(dto.getStartDate());
        schedule.setIsActive(true);

        MaintenanceSchedule saved = scheduleRepository.save(schedule);

        return mapScheduleToDTO(saved);
    }
    
    public List<MaintenanceScheduleResponseDTO> getSchedulesForUser(Authentication auth, String keyword) {
    	boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));
        boolean isUser = auth.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals("ROLE_ENDUSER"));

        String searchKeyword = (keyword == null) ? "" : keyword.trim();

        List<MaintenanceSchedule> entities;
        if (isAdmin || isUser) {
            entities = scheduleRepository.findByDevice_DeviceType_TypeNameContainingIgnoreCaseOrDevice_SerialNumberContainingIgnoreCase(searchKeyword, searchKeyword);
        } else {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::mapScheduleToDTO)
                .collect(Collectors.toList());
    }

    public Optional<MaintenanceScheduleResponseDTO> getScheduleByDeviceId(Integer deviceId) {
        return scheduleRepository.findByDevice_IdAndIsActiveTrue(deviceId)
                .map(this::mapScheduleToDTO);
    }
    
    @Transactional
    public MaintenanceScheduleResponseDTO saveSchedule(MaintenanceScheduleResponseDTO dto) {
        MaintenanceSchedule schedule;

        if (dto.getId() != null) {
            schedule = scheduleRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch trình ID: " + dto.getId()));
        } else {
            schedule = new MaintenanceSchedule();
            Device device = deviceRepository.findById(dto.getDeviceId())
                    .orElseThrow(() -> new RuntimeException("Thiết bị không tồn tại!"));
            schedule.setDevice(device);
        }

        schedule.setScheduleType(dto.getScheduleType());
        schedule.setCycleValue(dto.getCycleValue());
        schedule.setStartDate(dto.getStartDate());
        schedule.setIsActive(dto.getIsActive());

        MaintenanceSchedule savedEntity = scheduleRepository.save(schedule);

        return mapScheduleToDTO(savedEntity);
    }
    
    @Transactional
    public MaintenanceRequestsResponseDTO saveRequest(MaintenanceRequestsDTO dto) {
        MaintenanceRequests request;

        request = new MaintenanceRequests();

        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Thiết bị không tồn tại với ID: " + dto.getDeviceId()));
        request.setDevice(device);

        device.setStatus(DeviceStatus.BROKEN);
        deviceRepository.save(device);

        Users requester = usersRepository.findById(dto.getRequesterId())
                .orElseThrow(() -> new RuntimeException("Người dùng không hợp lệ!"));
        request.setRequester(requester);

        request.setDescription(dto.getDescription());
        request.setSeverity(dto.getSeverity());
        request.setStatus(RequestStatus.NEW);

        MaintenanceRequests savedEntity = requestRepository.save(request);

        return mapRequestToResponseDTO(savedEntity);
    }
    
    public void saveRequestWithUsername(MaintenanceRequestsDTO dto, String username) {
        Users user = usersRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        MaintenanceRequests request = new MaintenanceRequests();
        request.setDescription(dto.getDescription());
        request.setSeverity(dto.getSeverity());
        request.setRequester(user);
        
        Device device = deviceRepository.findById(dto.getDeviceId())
            .orElseThrow(() -> new RuntimeException("Thiết bị không tồn tại!"));
        request.setDevice(device);
        
        requestRepository.save(request);
    }
    
    private MaintenanceRequestsResponseDTO mapRequestToResponseDTO(MaintenanceRequests entity) {
        MaintenanceRequestsResponseDTO response = new MaintenanceRequestsResponseDTO();
        
        response.setId(entity.getId());
        response.setDescription(entity.getDescription());
        response.setSeverity(entity.getSeverity());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setRejectionReason(entity.getRejectionReason());

        if (entity.getDevice() != null) {
            response.setDeviceId(entity.getDevice().getId());
            if (entity.getDevice() != null && entity.getDevice().getDeviceType() != null) {
                String fullName = entity.getDevice().getDeviceType().getTypeName() 
                                + " - " + entity.getDevice().getSerialNumber();
                response.setDeviceName(fullName);
                }
        }

        if (entity.getRequester() != null) {
            response.setRequesterId(entity.getRequester().getId());
            response.setRequesterUsername(entity.getRequester().getUsername());
            response.setRequesterFullName(entity.getRequester().getFullName());
        }

        return response;
    }
    
    
    public List<MaintenanceRequestsResponseDTO> getRequestsByContext(String username, boolean isAdmin, String keyword) {
        List<MaintenanceRequests> entities;
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        if (isAdmin) {
            entities = hasKeyword ? 
                       requestRepository.searchAllByKeyword(keyword) : 
                       requestRepository.findAll();
        } else {
            entities = hasKeyword ? 
                       requestRepository.searchMyRequestsByKeyword(keyword, username) : 
                       requestRepository.findByRequester_Username(username);
        }

        return entities.stream()
                .map(this::mapRequestToResponseDTO) // Dùng hàm mapper của bạn
                .toList();
    }
    
    @Transactional
    public void rejectRequest(Long id, String reason) {
        MaintenanceRequests req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu yêu cầu ID: " + id));

        req.setStatus(RequestStatus.REJECTED);
        req.setRejectionReason(reason);

        Device device = req.getDevice();
        if (device != null) {
            device.setStatus(DeviceStatus.ACTIVE);
            deviceRepository.save(device);
        }

        requestRepository.save(req);
    }
    
    public List<MaintenanceHistoryResponseDTO> getAllHistory(String keyword, String techName, 
            String startDate, String endDate) {
		String cleanKw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
		String cleanTech = (techName != null && !techName.trim().isEmpty()) ? techName.trim() : null;
		
		java.time.LocalDateTime start = null;
		java.time.LocalDateTime end = null;
		try {
		if (startDate != null && !startDate.isEmpty()) 
		start = java.time.LocalDate.parse(startDate).atStartOfDay();
		if (endDate != null && !endDate.isEmpty()) 
		end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
		} catch (Exception e) {}
		
		return historyRepository.searchAdvancedHistory(cleanKw, cleanTech, start, end)
		.stream()
		.map(this::mapToDTO)
		.toList();
	}
    
    public List<MaintenanceRequestsResponseDTO> filterRequests(
            String keyword, String user, SeverityLevel severity, RequestStatus status, 
            java.time.LocalDate date) {
        
        java.time.LocalDateTime startOfDay = null;
        java.time.LocalDateTime endOfDay = null;

        if (date != null) {
            startOfDay = date.atStartOfDay(); // 00:00:00
            endOfDay = date.atTime(23, 59, 59); // 23:59:59
        }

        List<MaintenanceRequests> entities = requestRepository.filterRequests(
                keyword, user, severity, status, startOfDay, endOfDay);

        return entities.stream().map(this::mapRequestToResponseDTO).toList();
    }
    
    public List<MaintenanceScheduleResponseDTO> filterSchedules(
            String keyword, 
            ScheduleType type, 
            Integer cycle, 
            java.time.LocalDate start, 
            Boolean active) {
        
        String cleanKw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        List<MaintenanceSchedule> schedules = scheduleRepository.filterSchedules(
                cleanKw, type, cycle, start, active);

        return schedules.stream()
                .map(this::mapScheduleToDTO)
                .toList();
    }
    
    @Transactional
    public void accountantApproveAndInvoiced(Long historyId, String accountantUsername) {
        MaintenanceHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu!"));

        if (history.getStatus() != MaintenanceStatus.USER_ACCEPTED) {
            throw new RuntimeException("Phiếu chưa được người dùng nghiệm thu, kế toán chưa thể duyệt!");
        }

        WorkOrder order = history.getWorkOrder();
        if (order.getEvidenceBeforeUrl() == null || order.getEvidenceAfterUrl() == null) {
            throw new RuntimeException("Thiếu minh chứng hình ảnh kỹ thuật!");
        }

        Users accountant = usersRepository.findByUsername(accountantUsername).orElseThrow();
        history.setStatus(MaintenanceStatus.ACCOUNTANT_APPROVED);
        history.setApprovedBy(accountant);

        if (history.getDevice() != null) {
            history.getDevice().setStatus(DeviceStatus.ACTIVE); 
            history.getDevice().setLastMaintenanceDate(LocalDate.now());
        }

        Company company = history.getDevice().getCompany(); 
        BulkInvoice bulkInvoice = bulkInvoiceRepository.findByCompanyAndStatus(company, "PENDING")
            .stream().findFirst()
            .orElseGet(() -> {
                BulkInvoice newInv = new BulkInvoice();
                newInv.setCompany(company);
                newInv.setStatus("PENDING");
                newInv.setTotalAmount(BigDecimal.ZERO);
                return bulkInvoiceRepository.save(newInv);
            });

        history.setBulkInvoice(bulkInvoice);
        history.setStatus(MaintenanceStatus.INVOICED);
        
        BigDecimal newTotal = bulkInvoice.getTotalAmount().add(
            history.getCost() != null ? history.getCost() : BigDecimal.ZERO
        );
        bulkInvoice.setTotalAmount(newTotal);

        bulkInvoiceRepository.save(bulkInvoice);
        historyRepository.save(history);
    }

    @Transactional
    public void userAccept(Long historyId) {
        MaintenanceHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Phiếu bảo trì không tồn tại"));

        if (history.getStatus() != MaintenanceStatus.PENDING_ACCEPTANCE) {
            throw new RuntimeException("Thiết bị chưa được kỹ thuật viên xử lý xong!");
        }

        history.setStatus(MaintenanceStatus.USER_ACCEPTED);
        
        historyRepository.save(history);
    }
	    
    private MaintenanceScheduleResponseDTO mapScheduleToDTO(MaintenanceSchedule schedule) {
        return MaintenanceScheduleResponseDTO.builder()
                .id(schedule.getId())
                .deviceId(schedule.getDevice().getId())
                .deviceName(
                    schedule.getDevice().getDeviceType().getTypeName()
                    + " - "
                    + schedule.getDevice().getSerialNumber()
                )
                .scheduleType(schedule.getScheduleType())
                .cycleValue(schedule.getCycleValue())
                .startDate(schedule.getStartDate())
                .isActive(schedule.getIsActive())
                .build();
    }
    
    private MaintenanceHistoryResponseDTO mapToDTO(MaintenanceHistory history) {
        MaintenanceHistoryResponseDTO dto = new MaintenanceHistoryResponseDTO();
        dto.setId(history.getId());
        
        if (history.getDevice() != null) {
            dto.setDeviceSerial(history.getDevice().getSerialNumber());
            if (history.getDevice().getDeviceType() != null) {
                dto.setDeviceName(history.getDevice().getDeviceType().getTypeName());
            }
        }
        
        dto.setTechnicianName(history.getTechnician() != null ? history.getTechnician().getFullName() : "N/A");
        dto.setMaintenanceDate(history.getMaintenanceDate());
        dto.setResult(history.getResult());
        dto.setCost(history.getCost());
        dto.setStatus(history.getStatus());
        dto.setEvidenceBeforeUrl(history.getWorkOrder().getEvidenceBeforeUrl());
        dto.setEvidenceAfterUrl(history.getWorkOrder().getEvidenceAfterUrl());

        return dto;
    }
}

