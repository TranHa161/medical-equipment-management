package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
	Optional<Users> findByUsername(String username);
	
	boolean existsByUsername(String username);
   
   Optional<Users> findByEmail(String email);
    
   List<Users> findByRole_RoleName(String roleName);
    
   List<Users> findByFullNameContainingIgnoreCase(String fullName);

   @Query(value = "SELECT u.* FROM users u " +
           "LEFT JOIN workorder w ON u.user_id = w.technician_id AND w.status = 'IN_PROGRESS' " +
           "INNER JOIN roles r ON u.role_id = r.role_id " +
           "WHERE r.role_name = 'TECHNICIAN' " +
           "GROUP BY u.user_id " +
           "ORDER BY SUM(CASE " +
           "  WHEN w.severity = 'URGENT' THEN 4 " +
           "  WHEN w.severity = 'HIGH' THEN 3 " +
           "  WHEN w.severity = 'MEDIUM' THEN 2 " +
           "  WHEN w.severity = 'LOW' THEN 1 " +
           "  ELSE 0 END) ASC " +
           "LIMIT 1", nativeQuery = true)
   Optional<Users> findTopAvailableTechnician();
    
   @Query("SELECT COUNT(u) FROM Users u WHERE u.isActive = true " +
           "AND (u.role.roleName = 'ADMIN' OR u.role.roleName = 'TECHNICIAN')")
   long countActiveOperationalStaff();
    
   long countByRole_RoleName(String roleName);
}
