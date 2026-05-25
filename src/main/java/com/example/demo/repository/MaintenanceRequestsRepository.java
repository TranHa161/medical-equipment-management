package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.enums.RequestStatus;
import com.example.demo.enums.SeverityLevel;
import com.example.demo.model.Device;
import com.example.demo.model.MaintenanceRequests;

public interface MaintenanceRequestsRepository extends JpaRepository<MaintenanceRequests, Long>{
	List<MaintenanceRequests> findByStatus(RequestStatus status);
    @Query("SELECT r.device FROM MaintenanceRequests r WHERE r.id = :requestId")
    Device findDeviceByRequestId(@Param("requestId") Long requestId);
    
    List<MaintenanceRequests> findTop5ByOrderByCreatedAtDesc();
    
    List<MaintenanceRequests> findByRequester_Id(Long requesterId);
    
    List<MaintenanceRequests> findByDevice_SerialNumberContainingIgnoreCase(String serialNumber);
    
    @Query("SELECT r FROM MaintenanceRequests r WHERE " +
           "(LOWER(r.device.deviceType.typeName) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(r.device.serialNumber) LIKE LOWER(CONCAT('%', :kw, '%')))")
    List<MaintenanceRequests> searchAllByKeyword(@Param("kw") String keyword);

    @Query("SELECT r FROM MaintenanceRequests r WHERE " +
           "r.requester.username = :username AND " +
           "(LOWER(r.device.deviceType.typeName) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(r.device.serialNumber) LIKE LOWER(CONCAT('%', :kw, '%')))")
    List<MaintenanceRequests> searchMyRequestsByKeyword(@Param("kw") String keyword, @Param("username") String username);

    List<MaintenanceRequests> findByRequester_Username(String username);
    
    @Query("SELECT r FROM MaintenanceRequests r WHERE " +
    	       "(:kw IS NULL OR LOWER(r.device.serialNumber) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
    	       "LOWER(r.device.deviceType.typeName) LIKE LOWER(CONCAT('%', :kw, '%'))) AND " +
    	       "(:user IS NULL OR r.requester.username = :user OR LOWER(r.requester.fullName) LIKE LOWER(CONCAT('%', :user, '%'))) AND " +
    	       "(:sev IS NULL OR r.severity = :sev) AND " +
    	       "(:stat IS NULL OR r.status = :stat) AND " +
    	       "(:start IS NULL OR (r.createdAt >= :start AND r.createdAt <= :end)) " + 
    	       "ORDER BY r.createdAt DESC")
    	List<MaintenanceRequests> filterRequests(
    	        @Param("kw") String keyword, 
    	        @Param("user") String user, 
    	        @Param("sev") SeverityLevel severity, 
    	        @Param("stat") RequestStatus status,
    	        @Param("start") java.time.LocalDateTime start,
    	        @Param("end") java.time.LocalDateTime end);
    
    List<MaintenanceRequests> findTop5ByStatusOrderByCreatedAtDesc(com.example.demo.enums.RequestStatus status);
}
