package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.BulkInvoice;
import com.example.demo.repository.BulkInvoiceRepository;
import com.example.demo.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BulkInvoiceRepository bulkInvoiceRepo;

    public PaymentController(PaymentService paymentService, BulkInvoiceRepository bulkInvoiceRepo) {
        this.paymentService = paymentService;
        this.bulkInvoiceRepo = bulkInvoiceRepo;
    }

    @GetMapping("/pending/{companyId}")
    public ResponseEntity<?> getPendingOrders(@PathVariable Integer companyId) {
        return ResponseEntity.ok(paymentService.getPendingHistoryByCompany(companyId));
    }

    @PostMapping("/create-bulk-invoice/{companyId}")
    public ResponseEntity<?> createBulkInvoice(@PathVariable Integer companyId) {
        try {
            // Hàm này sẽ gom các MaintenanceHistory chưa thanh toán thành 1 BulkInvoice
            BulkInvoice invoice = paymentService.createBulkInvoice(companyId);
            return ResponseEntity.ok(invoice);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi tạo hóa đơn: " + e.getMessage());
        }
    }

    @PostMapping("/vnpay-callback")
    public ResponseEntity<?> handlePaymentCallback(@RequestParam Map<String, String> vnpayParams) {
        String responseCode = vnpayParams.get("vnp_ResponseCode");
        Long invoiceId = Long.parseLong(vnpayParams.get("vnp_TxnRef"));

        if ("00".equals(responseCode)) {
            paymentService.processPaymentSuccess(invoiceId);
            return ResponseEntity.ok("Cập nhật trạng thái thanh toán thành công!");
        }
        return ResponseEntity.badRequest().body("Thanh toán thất bại hoặc bị hủy.");
    }
    
    @GetMapping("/{invoiceId}/qr-link")
    public ResponseEntity<?> getPaymentQr(@PathVariable Long invoiceId, HttpServletRequest request) {
        BulkInvoice invoice = bulkInvoiceRepo.findById(invoiceId).orElseThrow();
        
        String paymentUrl = paymentService.generateVNPayUrl(invoice, request);
        
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }
}