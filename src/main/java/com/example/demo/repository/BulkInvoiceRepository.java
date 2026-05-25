package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.BulkInvoice;
import com.example.demo.model.Company;

public interface BulkInvoiceRepository extends JpaRepository<BulkInvoice, Long> {
    List<BulkInvoice> findByStatus(String status);

    List<BulkInvoice> findByCompanyAndStatus(Company company, String status);
}
