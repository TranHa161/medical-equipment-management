package com.example.demo.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.enums.WorkOrderStatus;
import com.example.demo.model.Device;
import com.example.demo.model.MaintenanceSchedule;
import com.example.demo.model.Users;
import com.example.demo.model.WorkOrder;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {
	List<WorkOrder> findByTechnicianAndStatus(Users technician, WorkOrderStatus status);
	// Kiểm tra xem đã có WorkOrder nào cho Schedule này với các trạng thái cụ thể chưa
    // Thường dùng Collection (List/Set) để linh hoạt kiểm tra nhiều trạng thái cùng lúc
    boolean existsByScheduleAndStatusIn(MaintenanceSchedule schedule, Collection<WorkOrderStatus> statuses);

    // Bổ sung thêm hàm kiểm tra thiết bị đang có WorkOrder hoạt động (Dùng cho UC08)
    @Query("SELECT COUNT(w) > 0 FROM WorkOrder w WHERE w.device = :device AND w.status = 'PROGRESSING'")
    boolean hasActiveWorkOrder(@Param("device") Device device);
    
    List<WorkOrder> findByStatusNot(WorkOrderStatus status);

    List<WorkOrder> findByTechnicianIdAndStatusNot(Long technicianId, WorkOrderStatus status);
    
    boolean existsByDevice_IdAndTechnician_Id(Integer deviceId, Long technicianId);
    
    @Query("SELECT COUNT(DISTINCT w.technician) FROM WorkOrder w WHERE w.status = 'PROGRESSING'")
    long countWorkingTechnicians();
    
    // Tìm danh sách WorkOrder theo username của Kỹ thuật viên
    List<WorkOrder> findByTechnician_UsernameAndStatusNot(String username, WorkOrderStatus status);
    
    @Query("SELECT w FROM WorkOrder w WHERE w.status <> 'CANCELLED' AND " +
    	       "(:username IS NULL OR w.technician.username = :username) AND " + 
    	       "(:deviceSearch IS NULL OR LOWER(w.device.serialNumber) LIKE LOWER(CONCAT('%', :deviceSearch, '%')) " +
    	       "OR LOWER(w.device.deviceType.typeName) LIKE LOWER(CONCAT('%', :deviceSearch, '%'))) AND " +
    	       "(:techSearch IS NULL OR LOWER(w.technician.fullName) LIKE LOWER(CONCAT('%', :techSearch, '%'))) AND " +
    	       "(:status IS NULL OR w.status = :status) AND " +
    	       "(:startDate IS NULL OR w.createdAt >= :startDate) AND " + // Lọc theo ngày bắt đầu
    	       "(:endDate IS NULL OR w.createdAt <= :endDate) " +         // Lọc theo ngày kết thúc
    	       "ORDER BY w.createdAt DESC") // Ưu tiên những phiếu mới tạo lên đầu bảng
   List<WorkOrder> filterWorkOrders(
    	        @Param("deviceSearch") String deviceSearch,
    	        @Param("techSearch") String techSearch,
    	        @Param("status") WorkOrderStatus status,
    	        @Param("username") String username,
    	        @Param("startDate") java.time.LocalDateTime startDate, // Chuyển kiểu dữ liệu sang LocalDateTime
    	        @Param("endDate") java.time.LocalDateTime endDate);     // Chuyển kiểu dữ liệu sang LocalDateTime
}
