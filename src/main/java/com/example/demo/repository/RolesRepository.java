package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Roles;

@Repository
public interface RolesRepository extends JpaRepository<Roles, Integer> {
    boolean existsByRoleName(String roleName);
    Optional<Roles> findByRoleName(String roleName);
}

