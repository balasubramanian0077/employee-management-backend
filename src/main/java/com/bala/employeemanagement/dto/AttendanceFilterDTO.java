package com.bala.employeemanagement.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AttendanceFilterDTO {
    private Long employeeId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String status;

    // getters and setters for all fields
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}