package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    
    @Column(name = "Status")
    private String status;
    
    @ManyToOne
    @JoinColumn(name = "bulk_invoice_id")
    private BulkInvoice bulkInvoice;
    
    @ManyToOne
    @JoinColumn(name = "asset_approved_by")
    private Users approvedBy;
    
    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company companyId;

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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
}