package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.BulkInvoice;

public interface BulkInvoiceRepository extends JpaRepository<BulkInvoice, Long> {
}
