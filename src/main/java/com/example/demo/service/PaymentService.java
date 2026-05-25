package com.example.demo.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.enums.MaintenanceStatus;
import com.example.demo.model.BulkInvoice;
import com.example.demo.model.Company;
import com.example.demo.model.MaintenanceHistory;
import com.example.demo.repository.BulkInvoiceRepository;
import com.example.demo.repository.CompanyRepository;
import com.example.demo.repository.MaintenanceHistoryRepository;
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
        List<MaintenanceHistory> histories = historyRepo.findApprovedByCompany(companyId);
        
        if (histories.isEmpty()) {
            throw new RuntimeException("Không có phiếu bảo trì nào đã được duyệt để lập hóa đơn!");
        }

        BigDecimal total = histories.stream()
                .map(h -> h.getCost() != null ? h.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Công ty không tồn tại với ID: " + companyId));

        BulkInvoice invoice = new BulkInvoice();
        invoice.setCompany(company);
        invoice.setTotalAmount(total);
        invoice.setStatus("PENDING");
        invoice.setCreatedAt(LocalDateTime.now());
        
        BulkInvoice savedInvoice = bulkInvoiceRepo.save(invoice);

        histories.forEach(h -> {
            h.setBulkInvoice(savedInvoice);
            h.setStatus(MaintenanceStatus.INVOICED); 
        });
        historyRepo.saveAll(histories);

        return savedInvoice;
    }

    public Map<Company, List<MaintenanceHistory>> getGroupedPendingHistory() {
        List<MaintenanceHistory> pendingHistories = historyRepo.findPendingForInvoicing();

        return pendingHistories.stream()
                .filter(m -> m.getCompanyId() != null)
                .collect(Collectors.groupingBy(MaintenanceHistory::getCompanyId));
    }
    
    @Transactional
    public void processPaymentSuccess(Long bulkInvoiceId) {
        BulkInvoice invoice = bulkInvoiceRepo.findById(bulkInvoiceId)
                .orElseThrow(() -> new RuntimeException("Hóa đơn không tồn tại"));

        if ("PAID".equals(invoice.getStatus())) {
            return;
        }

        invoice.setStatus("PAID");
        bulkInvoiceRepo.save(invoice);

        List<MaintenanceHistory> histories = historyRepo.findByBulkInvoice(invoice);
        histories.forEach(h -> h.setStatus(MaintenanceStatus.PAID));
        historyRepo.saveAll(histories);
    }

    @Transactional
    public void accountantApproveHistory(Long historyId, com.example.demo.model.Users accountant) {
        MaintenanceHistory history = historyRepo.findById(historyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu bảo trì"));

        if (history.getStatus() != MaintenanceStatus.USER_ACCEPTED) {
            throw new RuntimeException("Chỉ có thể duyệt những phiếu đã được người dùng nghiệm thu!");
        }

        history.setStatus(MaintenanceStatus.ACCOUNTANT_APPROVED);
        history.setApprovedBy(accountant);
        historyRepo.save(history);
    }

   public String generateVNPayUrl(BulkInvoice invoice, HttpServletRequest req) {
    String vnp_Version = "2.1.0";
    String vnp_Command = "pay";
    String vnp_TxnRef = invoice.getId() + "_" + VNPayConfig.getRandomNumber(8);
    String vnp_IpAddr = "127.0.0.1";
    String vnp_TmnCode = VNPayConfig.vnp_TmnCode;

    Map<String, String> vnp_Params = new HashMap<>();
    vnp_Params.put("vnp_Version", vnp_Version);
    vnp_Params.put("vnp_Command", vnp_Command);
    vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
    
    long amountValue = invoice.getTotalAmount().multiply(new BigDecimal(100)).longValue();
    vnp_Params.put("vnp_Amount", String.valueOf(amountValue));
    
    vnp_Params.put("vnp_CurrCode", "VND");
    vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
    vnp_Params.put("vnp_OrderInfo", "Thanhtoanhoadonso:" + invoice.getId());
    vnp_Params.put("vnp_OrderType", "other");
    vnp_Params.put("vnp_Locale", "vn");
    vnp_Params.put("vnp_ReturnUrl", VNPayConfig.getVnpReturnurl());
    vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

    Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
    String vnp_CreateDate = formatter.format(cld.getTime());
    vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
    
    List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
    Collections.sort(fieldNames);

    StringBuilder hashData = new StringBuilder();
    StringBuilder query = new StringBuilder();
    
    Iterator<String> itr = fieldNames.iterator();
    while (itr.hasNext()) {
        String fieldName = itr.next();
        String fieldValue = vnp_Params.get(fieldName);
        if ((fieldValue != null) && (fieldValue.length() > 0)) {
            try {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    String queryUrl = query.toString();
    String vnp_SecureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());
    queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

    String finalPaymentUrl = VNPayConfig.vnp_PayUrl + "?" + queryUrl;
    
    invoice.setQrCodeUrl(finalPaymentUrl);
    bulkInvoiceRepo.save(invoice);
    
    System.out.println("--- DEBUG ---");
    System.out.println("HashData: " + hashData.toString());
    System.out.println("Final URL: " + finalPaymentUrl);
    
    return finalPaymentUrl;
}

    public List<MaintenanceHistory> getPendingHistoryByCompany(Integer companyId) {
        return historyRepo.findApprovedByCompany(companyId);
    }
}