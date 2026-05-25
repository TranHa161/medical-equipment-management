package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.enums.ScheduleType;
import com.example.demo.model.MaintenanceSchedule;

public interface MaintenanceScheduleRepository extends JpaRepository<MaintenanceSchedule, Long> {
    Optional<MaintenanceSchedule> findByDevice_IdAndIsActiveTrue(Integer deviceId);
    List<MaintenanceSchedule> findByIsActiveTrue();
    List<MaintenanceSchedule> findByDevice_DeviceType_TypeNameContainingIgnoreCaseOrDevice_SerialNumberContainingIgnoreCase(String typeKeyword, String serialKeyword);
    @Query("SELECT s FROM MaintenanceSchedule s WHERE " +
    	       "(:kw IS NULL OR LOWER(s.device.serialNumber) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
    	       "LOWER(s.device.deviceType.typeName) LIKE LOWER(CONCAT('%', :kw, '%'))) AND " +
    	       "(:type IS NULL OR s.scheduleType = :type) AND " +
    	       "(:cycle IS NULL OR s.cycleValue = :cycle) AND " +
    	       "(:start IS NULL OR s.startDate >= :start) AND " +
    	       "(:active IS NULL OR s.isActive = :active)")
    	List<MaintenanceSchedule> filterSchedules(
    	        @Param("kw") String keyword, 
    	        @Param("type") ScheduleType type, 
    	        @Param("cycle") Integer cycle, 
    	        @Param("start") java.time.LocalDate start, 
    	        @Param("active") Boolean active);
}
