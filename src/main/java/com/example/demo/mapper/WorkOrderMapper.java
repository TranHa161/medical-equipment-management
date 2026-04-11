package com.example.demo.mapper;

import com.example.demo.dto.WorkOrderDTO;
import com.example.demo.model.WorkOrder;

public class WorkOrderMapper {

    public static WorkOrderDTO toDTO(WorkOrder order) {
        if (order == null) return null;

        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setId(order.getId());
        dto.setDeviceId(order.getDevice().getId());
        dto.setDeviceName(order.getDevice().getDeviceType().getTypeName());
        dto.setTechnicianId(order.getTechnician() != null ? order.getTechnician().getId() : null);
        dto.setTechnicianName(order.getTechnician() != null ? order.getTechnician().getFullName() : null);
        dto.setScheduleId(order.getSchedule() != null ? order.getSchedule().getId() : null);
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());

        return dto;
    }
}
