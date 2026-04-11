package com.example.demo.enums;

public enum RequestStatus {
	NEW,          // Phiếu mới tạo
    PROGRESSING,   // Đang xử lý (đã có WorkOrder)
    COMPLETED,    // Đã sửa xong
    CANCELLED,     // Đã hủy
    REJECTED
}
