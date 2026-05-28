# HUST MEDICAL - Medical Equipment Maintenance Management System

HUST MEDICAL là hệ thống quản lý và bảo trì thiết bị y tế được xây dựng theo kiến trúc Monolithic nhằm hỗ trợ bệnh viện/phòng khám tối ưu hóa quy trình báo hỏng, điều phối kỹ thuật viên, theo dõi bảo trì và quản lý thanh toán chi phí sửa chữa.

Hệ thống tập trung vào:

* Quản lý vòng đời thiết bị y tế
* Tự động hóa quy trình xử lý sự cố
* Phân quyền người dùng chặt chẽ
* Tích hợp DevOps & Cloud Deployment hiện đại

---

# 🌐 Demo

**Demo URL:**
http://13.214.145.4:8080/login

> ⚠️ Lưu ý:
>
> * Đây là môi trường demo cá nhân.
> * Hệ thống hiện chạy trên giao thức HTTP và chưa cấu hình SSL/HTTPS.
> * Không sử dụng mật khẩu thật hoặc thông tin nhạy cảm khi đăng nhập thử nghiệm.

---

# ✨ Tính năng chính

## 📊 Dashboard Monitoring

* Thống kê tổng số thiết bị
* Theo dõi số lượng thiết bị đang hỏng / đang sửa chữa
* Hiển thị số lượng kỹ thuật viên sẵn sàng
* Biểu đồ yêu cầu báo hỏng gần nhất theo thời gian thực

---

## 🏥 Medical Equipment Lifecycle Management

* Quản lý thông tin thiết bị theo Serial Number
* Theo dõi vị trí và trạng thái hoạt động
* Lưu lịch sử bảo trì và sửa chữa
* Quản lý hình ảnh thiết bị

---

## 🔧 Maintenance Workflow System

Tự động hóa quy trình xử lý sự cố:

1. Người dùng cuối tạo yêu cầu báo hỏng
2. Admin tiếp nhận và phê duyệt
3. Phân công kỹ thuật viên xử lý
4. Kỹ thuật viên cập nhật tiến độ sửa chữa
5. Kế toán xử lý hóa đơn và thanh toán

---

## 💳 VNPay Payment Integration

* Tích hợp VNPay Sandbox
* Hỗ trợ thanh toán hóa đơn sửa chữa trực tuyến
* Tự động cập nhật trạng thái Work Order sau khi thanh toán thành công
* Hỗ trợ mô phỏng QR Code và ATM Banking

---

# 🛠️ Technologies Used

## Backend

* Java 17
* Spring Boot 3.x
* Spring Security
* Spring Data JPA

---

## Frontend

* Thymeleaf
* HTML5
* CSS3
* JavaScript

---

## Database

* MySQL 8.x

---

# ☁️ DevOps & Cloud Infrastructure

## 🐳 Containerization

* Docker
* Build ứng dụng thành Docker Image thống nhất môi trường triển khai

---

## 🔄 CI/CD Pipeline

* GitHub Actions
* Tự động:

  * Build project
  * Run test
  * Build Docker Image
  * Deploy lên AWS EC2 khi push lên nhánh `main`

---

## ☁️ Cloud Hosting

* AWS EC2 (Ubuntu Server)

---

## 🗂️ Cloud Storage

* AWS S3
* Lưu trữ ảnh thiết bị y tế
* Giảm tải dung lượng cho EC2 server

---

# 🚀 Local Setup Guide

## 📌 Prerequisites

Trước khi chạy project, cần cài đặt:

* Java 17+
* MySQL Server 8.x
* Maven
* Docker (optional)

---

# 📥 Clone Repository

```bash
git clone https://github.com/your-username/medical-equipment-management.git
cd medical-equipment-management
```

---

# ⚙️ Configure Database

Tạo database MySQL:

```sql
CREATE DATABASE hust_medical;
```

Cập nhật file:

```properties
src/main/resources/application.properties
```

Ví dụ:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hust_medical
spring.datasource.username=root
spring.datasource.password=your_password
aws.s3.bucket-name=your-s3-bucket-name
aws.access-key=your-aws-access-key
aws.secret-key=your-aws-secret-key
vnpay.tmn-code=your_vnpay_tmn_code
vnpay.hash-secret=your_vnpay_hash_secret
vnpay.pay-url=[https://sandbox.vnpayment.vn/paymentv2/vpcpay.html](https://sandbox.vnpayment.vn/paymentv2/vpcpay.html)
vnpay.return-url=http://localhost:8080/api/v1/vnpay-callback
```

---

# ▶️ Run Application

## Using Maven

```bash
./mvnw spring-boot:run
```

Hoặc:

```bash
mvn spring-boot:run
```

---

## Using Docker

Build image:

```bash
docker build -t hust-medical .
```

Run container:

```bash
docker run -p 8080:8080 hust-medical
```
---

# 📄 License

This project is developed for educational and portfolio purposes.

---

# 👨‍💻 Author

Developed by Ly Ha Tran Tran
Medical Equipment Lifecycle & Maintenance System Project
