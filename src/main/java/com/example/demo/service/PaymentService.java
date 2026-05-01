package com.example.demo.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.BulkInvoice;
import com.example.demo.model.Company;
import com.example.demo.model.MaintenanceHistory;
import com.example.demo.repository.BulkInvoiceRepository;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.MaintenanceHistoryRepository;
import com.example.demo.repository.WorkOrderRepository;
import com.example.demo.security.VNPayConfig;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class PaymentService {
    @Autowired private BulkInvoiceRepository bulkInvoiceRepo;
    @Autowired private MaintenanceHistoryRepository historyRepo;
    @Autowired private CompanyRepository companyRepo;

    @Transactional
    public BulkInvoice createBulkInvoice(Integer companyId) {
        List<MaintenanceHistory> histories = historyRepo.findPendingHistoryByCompany(companyId);
        
        if (histories.isEmpty()) {
            throw new RuntimeException("Không có phiếu bảo trì nào chờ thanh toán cho công ty này!");
        }

        BigDecimal total = histories.stream()
                .map(h -> h.getCost() != null ? h.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Công ty không tồn tại"));

        BulkInvoice invoice = new BulkInvoice();
        invoice.setCompany(company);
        invoice.setTotalAmount(total);
        invoice.setStatus("PENDING");
        invoice.setCreatedAt(LocalDateTime.now());
        
        BulkInvoice savedInvoice = bulkInvoiceRepo.save(invoice);

        histories.forEach(h -> {
            h.setBulkInvoice(savedInvoice);
            historyRepo.save(h);
        });

        return savedInvoice;
    }
    
    @Transactional
    public void processPaymentSuccess(Long bulkInvoiceId) {

        BulkInvoice invoice = bulkInvoiceRepo.findById(bulkInvoiceId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại"));
        invoice.setStatus("PAID");
        bulkInvoiceRepo.save(invoice);

        List<MaintenanceHistory> histories = historyRepo.findByBulkInvoice(invoice);
        
        for (MaintenanceHistory history : histories) {
            history.setStatus("PAID"); 
            historyRepo.save(history);
        }
    }
    
    public List<MaintenanceHistory> getPendingHistoryByCompany(Integer companyId) {
        return historyRepo.findPendingHistoryByCompany(companyId);
    }
    
    public String generateVNPayUrl(BulkInvoice invoice, HttpServletRequest req) {
        
        String vnp_Addr = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        
        try {
            String paymentUrl = VNPayConfig.createPaymentUrl(invoice, req);
            invoice.setQrCodeUrl(paymentUrl);
            bulkInvoiceRepo.save(invoice);
            return paymentUrl;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi mã hóa URL thanh toán: " + e.getMessage());
        }       
    }
}