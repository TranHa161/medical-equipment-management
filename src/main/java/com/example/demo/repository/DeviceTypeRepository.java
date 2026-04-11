package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.DeviceType;

@Repository
public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long>{
	List<DeviceType> findAllByOrderByTypeNameAsc();
	
	Optional<DeviceType> findByTypeName(String typeName);
	
	Optional<DeviceType> findById(Integer integer);
}
