package com.example.demo.model;

import com.example.demo.enums.MaintenanceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "maintenancehistory")
@NoArgsConstructor @AllArgsConstructor @Builder
public class MaintenanceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HistoryID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "WorkOrderID")
    private WorkOrder workOrder;

    @ManyToOne
    @JoinColumn(name = "DeviceID")
    private Device device;

    @ManyToOne
    @JoinColumn(name = "PerformedBy")
    private Users technician;

    @Column(name = "MaintenanceDate")
    private java.time.LocalDateTime maintenanceDate;

    @Column(name = "Result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "Cost")
    private java.math.BigDecimal cost;
    
    @Enumerated(EnumType.STRING)
	@Column(name = "Status")
	private MaintenanceStatus status;
    
    @ManyToOne
    @JoinColumn(name = "bulk_invoice_id")
    private BulkInvoice bulkInvoice;
    
    @ManyToOne
    @JoinColumn(name = "asset_approved_by")
    private Users approvedBy;
    
    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company companyId;

	@Column(name = "before_image_url", length = 500)
    private String beforeImageUrl;

    @Column(name = "after_image_url", length = 500)
    private String afterImageUrl;

    @Column(name = "user_accepted_at")
    private java.time.LocalDateTime userAcceptedAt;

    @Column(name = "accountant_approved_at")
    private java.time.LocalDateTime accountantApprovedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public WorkOrder getWorkOrder() {
		return workOrder;
	}

	public void setWorkOrder(WorkOrder workOrder) {
		this.workOrder = workOrder;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public Users getTechnician() {
		return technician;
	}

	public void setTechnician(Users technician) {
		this.technician = technician;
	}

	public java.time.LocalDateTime getMaintenanceDate() {
		return maintenanceDate;
	}

	public void setMaintenanceDate(java.time.LocalDateTime maintenanceDate) {
		this.maintenanceDate = maintenanceDate;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public java.math.BigDecimal getCost() {
		return cost;
	}

	public void setCost(java.math.BigDecimal cost) {
		this.cost = cost;
	}

	public BulkInvoice getBulkInvoice() {
		return bulkInvoice;
	}

	public void setBulkInvoice(BulkInvoice bulkInvoice) {
		this.bulkInvoice = bulkInvoice;
	}

	public Users getApprovedBy() {
		return approvedBy;
	}

	public void setApprovedBy(Users approvedBy) {
		this.approvedBy = approvedBy;
	}

	public Company getCompanyId() {
		return companyId;
	}

	public void setCompanyId(Company companyId) {
		this.companyId = companyId;
	}

	public String getBeforeImageUrl() {
		return beforeImageUrl;
	}

	public void setBeforeImageUrl(String beforeImageUrl) {
		this.beforeImageUrl = beforeImageUrl;
	}

	public String getAfterImageUrl() {
		return afterImageUrl;
	}

	public void setAfterImageUrl(String afterImageUrl) {
		this.afterImageUrl = afterImageUrl;
	}

	public java.time.LocalDateTime getUserAcceptedAt() {
		return userAcceptedAt;
	}

	public void setUserAcceptedAt(java.time.LocalDateTime userAcceptedAt) {
		this.userAcceptedAt = userAcceptedAt;
	}

	public java.time.LocalDateTime getAccountantApprovedAt() {
		return accountantApprovedAt;
	}

	public void setAccountantApprovedAt(java.time.LocalDateTime accountantApprovedAt) {
		this.accountantApprovedAt = accountantApprovedAt;
	}

	public MaintenanceStatus getStatus() {
		return status;
	}

	public void setStatus(MaintenanceStatus status) {
		this.status = status;
	}
}