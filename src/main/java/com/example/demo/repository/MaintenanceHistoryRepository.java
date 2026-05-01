package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.BulkInvoice;
import com.example.demo.model.MaintenanceHistory;

@Repository
public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, Long> {
    List<MaintenanceHistory> findTop5ByOrderByMaintenanceDateDesc();
    
    @Query("SELECT h FROM MaintenanceHistory h WHERE " +
    	       "(:kw IS NULL OR LOWER(h.device.serialNumber) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
    	       "LOWER(h.device.deviceType.typeName) LIKE LOWER(CONCAT('%', :kw, '%')) OR " + // Tìm theo tên loại máy
    	       "LOWER(h.technician.fullName) LIKE LOWER(CONCAT('%', :kw, '%')))")
    	List<MaintenanceHistory> searchAllHistory(@Param("kw") String keyword);
    
    @Query("SELECT h FROM MaintenanceHistory h WHERE " +
    	       "(:kw IS NULL OR LOWER(h.device.serialNumber) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
    	       "LOWER(h.device.deviceType.typeName) LIKE LOWER(CONCAT('%', :kw, '%'))) AND " +
    	       "(:techName IS NULL OR LOWER(h.technician.fullName) LIKE LOWER(CONCAT('%', :techName, '%'))) AND " +
    	       "(:startDate IS NULL OR h.maintenanceDate >= :startDate) AND " +
    	       "(:endDate IS NULL OR h.maintenanceDate <= :endDate) " +
    	       "ORDER BY h.maintenanceDate DESC")
    	List<MaintenanceHistory> searchAdvancedHistory(
    	    @Param("kw") String keyword, 
    	    @Param("techName") String techName,
    	    @Param("startDate") java.time.LocalDateTime startDate,
    	    @Param("endDate") java.time.LocalDateTime endDate);
    
    @Query("SELECT mh FROM MaintenanceHistory mh " +
    	       "JOIN mh.workOrder wo " +
    	       "WHERE wo.device.company.id = :companyId " +
    	       "AND mh.status = 'APPROVED' " +
    	       "AND mh.bulkInvoice IS NULL")
     List<MaintenanceHistory> findPendingHistoryByCompany(@Param("companyId") Integer companyId);

	List<MaintenanceHistory> findByBulkInvoice(BulkInvoice invoice);
}