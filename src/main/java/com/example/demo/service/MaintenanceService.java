package com.example.demo.service;

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
import com.example.demo.enums.RequestStatus;
import com.example.demo.enums.ScheduleType;
import com.example.demo.enums.SeverityLevel;
import com.example.demo.model.Device;
import com.example.demo.model.MaintenanceHistory;
import com.example.demo.model.MaintenanceRequests;
import com.example.demo.model.MaintenanceSchedule;
import com.example.demo.model.Users;
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
    private final UsersService usersService;
    
    public MaintenanceService(MaintenanceRequestsRepository requestRepository,
                              DeviceRepository deviceRepository,
                              UsersRepository usersRepository,
                              MaintenanceScheduleRepository scheduleRepository, UsersService usersService, MaintenanceHistoryRepository historyRepository) {
        this.requestRepository = requestRepository;
        this.deviceRepository = deviceRepository;
        this.usersRepository = usersRepository;
        this.scheduleRepository = scheduleRepository;
		this.historyRepository = historyRepository;
		this.usersService = usersService;
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
        // 1. Tìm phiếu cũ trong DB
        MaintenanceRequests request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu yêu cầu ID: " + id));

        // 2. CHẶN: Chỉ cho phép sửa khi trạng thái là NEW
        if (request.getStatus() != RequestStatus.NEW) {
            throw new RuntimeException("Không thể chỉnh sửa phiếu đã được duyệt hoặc từ chối!");
        }

        // 3. Cập nhật các trường được phép sửa
        request.setDescription(dto.getDescription());
        request.setSeverity(dto.getSeverity());

        // 4. Lưu và trả về DTO
        MaintenanceRequests saved = requestRepository.save(request);
        return mapRequestToResponseDTO(saved);
    }
    
    public MaintenanceRequestsResponseDTO getRequestById(Long id) {
        // 1. Truy vấn Database, ném lỗi nếu không tìm thấy ID
        MaintenanceRequests request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu yêu cầu ID: " + id));

        // 2. Chuyển đổi Entity sang ResponseDTO để trả về cho Controller
        return mapRequestToResponseDTO(request);
    }
    
    public List<MaintenanceRequestsResponseDTO> searchByDeviceSerial(String keyword) {
        List<MaintenanceRequests> requests;
        
        // Nếu có từ khóa thì tìm theo Serial, nếu không thì lấy tất cả
        if (keyword != null && !keyword.trim().isEmpty()) {
            requests = requestRepository.findByDevice_SerialNumberContainingIgnoreCase(keyword);
        } else {
            requests = requestRepository.findAll();
        }
        
        // Chuyển sang DTO và lọc bỏ phần tử lỗi
        return requests.stream()
                .map(this::mapRequestToResponseDTO)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * UC06: Xem lịch bảo trì đang hoạt động của thiết bị
     */
    public Optional<MaintenanceScheduleResponseDTO> getDeviceActiveSchedule(Integer deviceId) {
        return scheduleRepository
                .findByDevice_IdAndIsActiveTrue(deviceId)
                .map(this::mapScheduleToDTO);
    }

    /**
     * UC07: Thiết lập lịch bảo trì định kỳ
     */
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

        // 2. Chuẩn hóa từ khóa
        String searchKeyword = (keyword == null) ? "" : keyword.trim();

        // 3. Thực hiện truy vấn: Cả ADMIN và USER đều thấy toàn bộ lịch
        List<MaintenanceSchedule> entities;
        if (isAdmin || isUser) {
            // Tìm theo Device -> DeviceType -> typeName
            entities = scheduleRepository.findByDevice_DeviceType_TypeNameContainingIgnoreCaseOrDevice_SerialNumberContainingIgnoreCase(searchKeyword, searchKeyword);
        } else {
            // Nếu là role khác (không được phép), trả về danh sách trống
            return Collections.emptyList();
        }

        // 4. Chuyển đổi sang DTO
        return entities.stream()
                .map(this::mapScheduleToDTO)
                .collect(Collectors.toList());
    }

    public Optional<MaintenanceScheduleResponseDTO> getScheduleByDeviceId(Integer deviceId) {
        // Tìm kiếm schedule dựa trên ID của thiết bị liên kết
        return scheduleRepository.findByDevice_IdAndIsActiveTrue(deviceId)
                .map(this::mapScheduleToDTO); // Sử dụng hàm mapper để chuyển đổi sang DTO
    }
    
    @Transactional
    public MaintenanceScheduleResponseDTO saveSchedule(MaintenanceScheduleResponseDTO dto) {
        MaintenanceSchedule schedule;

        // 1. Kiểm tra xem là Cập nhật hay Thêm mới
        if (dto.getId() != null) {
            schedule = scheduleRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch trình ID: " + dto.getId()));
        } else {
            schedule = new MaintenanceSchedule();
            // Link với Device
            Device device = deviceRepository.findById(dto.getDeviceId())
                    .orElseThrow(() -> new RuntimeException("Thiết bị không tồn tại!"));
            schedule.setDevice(device);
        }

        // 2. Cập nhật các thông tin từ DTO vào Entity
        schedule.setScheduleType(dto.getScheduleType());
        schedule.setCycleValue(dto.getCycleValue());
        schedule.setStartDate(dto.getStartDate());
        schedule.setIsActive(dto.getIsActive());

        // 3. Lưu vào Database
        MaintenanceSchedule savedEntity = scheduleRepository.save(schedule);

        // 4. Trả về DTO sau khi lưu thành công
        return mapScheduleToDTO(savedEntity);
    }
    
    @Transactional
    public MaintenanceRequestsResponseDTO saveRequest(MaintenanceRequestsDTO dto) {
        MaintenanceRequests request;

        // 1. Kiểm tra ID để xác định Update hay Create (Giả định DTO có id hoặc pass qua param)
        // Nếu bạn chỉ dùng để báo hỏng mới, có thể bỏ qua bước kiểm tra id này.
        request = new MaintenanceRequests();

        // 2. Tìm và liên kết với Device (Sử dụng Integer deviceId)
        Device device = deviceRepository.findById(dto.getDeviceId())
                .orElseThrow(() -> new RuntimeException("Thiết bị không tồn tại với ID: " + dto.getDeviceId()));
        request.setDevice(device);

        // LOGIC QUAN TRỌNG: Cập nhật trạng thái máy sang BROKEN ngay khi báo hỏng
        device.setStatus(DeviceStatus.BROKEN);
        deviceRepository.save(device);

        // 3. Tìm và liên kết với Người gửi (Long requesterId)
        Users requester = usersRepository.findById(dto.getRequesterId())
                .orElseThrow(() -> new RuntimeException("Người dùng không hợp lệ!"));
        request.setRequester(requester);

        // 4. Cập nhật các thông tin mô tả từ DTO
        request.setDescription(dto.getDescription());
        request.setSeverity(dto.getSeverity()); // SeverityLevel enum
        request.setStatus(RequestStatus.NEW); // Mặc định là NEW cho phiếu mới

        // 5. Lưu vào Database
        MaintenanceRequests savedEntity = requestRepository.save(request);

        // 6. Trả về Response DTO
        return mapRequestToResponseDTO(savedEntity);
    }
    
    public void saveRequestWithUsername(MaintenanceRequestsDTO dto, String username) {
        // Tìm User dựa trên Username thay vì ID từ Frontend gửi lên
        Users user = usersRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng!"));

        MaintenanceRequests request = new MaintenanceRequests();
        request.setDescription(dto.getDescription());
        request.setSeverity(dto.getSeverity());
        request.setRequester(user); // Gán người báo hỏng chuẩn xác
        
        // Tìm thiết bị (Đảm bảo deviceId không null)
        Device device = deviceRepository.findById(dto.getDeviceId())
            .orElseThrow(() -> new RuntimeException("Thiết bị không tồn tại!"));
        request.setDevice(device);
        
        requestRepository.save(request);
    }
    
    private MaintenanceRequestsResponseDTO mapRequestToResponseDTO(MaintenanceRequests entity) {
        MaintenanceRequestsResponseDTO response = new MaintenanceRequestsResponseDTO();
        
        // Ánh xạ các thông tin cơ bản
        response.setId(entity.getId());
        response.setDescription(entity.getDescription());
        response.setSeverity(entity.getSeverity());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
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
        }

        // Ánh xạ thông tin Người yêu cầu
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
            // Nếu là Admin
            entities = hasKeyword ? 
                       requestRepository.searchAllByKeyword(keyword) : 
                       requestRepository.findAll();
        } else {
            // Nếu là User thường
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
        // 1. Tìm phiếu báo hỏng
        MaintenanceRequests req = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu yêu cầu ID: " + id));

        // 2. Cập nhật thông tin từ chối
        req.setStatus(RequestStatus.REJECTED);
        req.setRejectionReason(reason);

        // 3. Quan trọng: Đổi trạng thái thiết bị về ACTIVE để người khác có thể sử dụng
        Device device = req.getDevice();
        if (device != null) {
            device.setStatus(DeviceStatus.ACTIVE);
            deviceRepository.save(device);
        }

        requestRepository.save(req);
    }
    
    public List<MaintenanceHistoryResponseDTO> getAllHistory(String keyword, String techName, 
            String startDate, String endDate) {
		// 1. Làm sạch chuỗi
		String cleanKw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
		String cleanTech = (techName != null && !techName.trim().isEmpty()) ? techName.trim() : null;
		
		// 2. Chuyển đổi String sang LocalDateTime
		java.time.LocalDateTime start = null;
		java.time.LocalDateTime end = null;
		try {
		if (startDate != null && !startDate.isEmpty()) 
		start = java.time.LocalDate.parse(startDate).atStartOfDay();
		if (endDate != null && !endDate.isEmpty()) 
		end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
		} catch (Exception e) { /* Bỏ qua nếu format ngày sai */ }
		
		// 3. Truy vấn và Map sang DTO
		return historyRepository.searchAdvancedHistory(cleanKw, cleanTech, start, end)
		.stream()
		.map(this::mapToDTO)
		.toList();
	}
    
    /**
     * Lọc danh sách phiếu báo hỏng dựa trên nhiều tiêu chí
     */
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
    
    /**
     * Lọc danh sách lịch bảo trì định kỳ dựa trên nhiều tiêu chí
     */
    public List<MaintenanceScheduleResponseDTO> filterSchedules(
            String keyword, 
            ScheduleType type, 
            Integer cycle, 
            java.time.LocalDate start, 
            Boolean active) {
        
        // 1. Tiền xử lý từ khóa (Tránh lỗi tìm kiếm khi chỉ nhập khoảng trắng)
        // Chuyển chuỗi rỗng thành null để khớp với điều kiện (:kw IS NULL) trong Query
        String cleanKw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        // 2. Gọi Repository thực hiện truy vấn linh hoạt
        List<MaintenanceSchedule> schedules = scheduleRepository.filterSchedules(
                cleanKw, type, cycle, start, active);

        // 3. Chuyển đổi từ Entity sang ResponseDTO bằng hàm mapper bạn đã viết
        return schedules.stream()
                .map(this::mapScheduleToDTO)
                .toList();
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
        
        // Trích xuất TypeName từ DeviceType để làm deviceName
        if (history.getDevice() != null) {
            dto.setDeviceSerial(history.getDevice().getSerialNumber());
            if (history.getDevice().getDeviceType() != null) {
                dto.setDeviceName(history.getDevice().getDeviceType().getTypeName());
            }
        }
        
        // Gán thông tin kỹ thuật viên và kết quả
        dto.setTechnicianName(history.getTechnician() != null ? history.getTechnician().getFullName() : "N/A");
        dto.setMaintenanceDate(history.getMaintenanceDate());
        dto.setResult(history.getResult());
        dto.setCost(history.getCost());


        return dto;
    }
}

