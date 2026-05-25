package com.example.demo.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import com.example.demo.model.BulkInvoice;
import com.example.demo.model.Company;
import com.example.demo.model.MaintenanceHistory;
import com.example.demo.repository.BulkInvoiceRepository;
import com.example.demo.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final BulkInvoiceRepository bulkInvoiceRepo;

    public PaymentController(PaymentService paymentService, BulkInvoiceRepository bulkInvoiceRepo) {
        this.paymentService = paymentService;
        this.bulkInvoiceRepo = bulkInvoiceRepo;
    }

    @GetMapping("") 
    public String showPaymentPage(Model model) {
        Map<Company, List<MaintenanceHistory>> pendingMap = paymentService.getGroupedPendingHistory();
        model.addAttribute("companyPendingMap", pendingMap);

        Map<Integer, java.math.BigDecimal> totals = pendingMap.entrySet().stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().getId().intValue(), // Key: Integer ID
            entry -> entry.getValue().stream()
                    .map(MaintenanceHistory::getCost)
                    .filter(cost -> cost != null)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add) 
        ));
        model.addAttribute("totals", totals);

        model.addAttribute("pendingInvoices", bulkInvoiceRepo.findByStatus("PENDING"));

        return "payment";
    }

    @GetMapping("/pending/{companyId}")
    @ResponseBody
    public ResponseEntity<?> getPendingOrders(@PathVariable Integer companyId) {
        return ResponseEntity.ok(paymentService.getPendingHistoryByCompany(companyId));
    }

    @PostMapping("/create-bulk-invoice/{companyId}")
    @ResponseBody
    public ResponseEntity<?> createBulkInvoice(@PathVariable Integer companyId) {
        try {
            BulkInvoice invoice = paymentService.createBulkInvoice(companyId);
            return ResponseEntity.ok(invoice);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/vnpay-callback")
    public String handlePaymentCallback(@RequestParam Map<String, String> vnpayParams) {

        String responseCode = vnpayParams.get("vnp_ResponseCode");
        String txnRef = vnpayParams.get("vnp_TxnRef");

        try {

            Long invoiceId = Long.parseLong(txnRef.split("_")[0]);

            if ("00".equals(responseCode)) {

                paymentService.processPaymentSuccess(invoiceId);

                return "redirect:/payment?success=true";

            } else {

                return "redirect:/payment?error=failed";
            }

        } catch (Exception e) {

            return "redirect:/payment?error=system";
        }
    }
    
    @GetMapping("/{invoiceId}/qr-link")
    @ResponseBody
    public ResponseEntity<?> getPaymentQr(@PathVariable Long invoiceId, HttpServletRequest request) {
        return bulkInvoiceRepo.findById(invoiceId)
            .map(invoice -> {
                String paymentUrl = paymentService.generateVNPayUrl(invoice, request);
                return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{invoiceId}/check-status")
    public ResponseEntity<String> checkInvoiceStatus(@PathVariable Long invoiceId) {
        try {
            paymentService.processPaymentSuccess(invoiceId);
            
            return ResponseEntity.ok("Thanh toán hóa đơn tổng #" + invoiceId + " thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Lỗi xử lý thanh toán: " + e.getMessage());
        }
    }
}