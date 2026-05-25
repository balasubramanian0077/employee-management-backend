package com.bala.employeemanagement.dto;

import java.math.BigDecimal;

public class AttendanceStatsDTO {
    private long totalDays;
    private long presentDays;
    private long lateDays;
    private long halfDays;
    private long absentDays;
    private BigDecimal totalWorkingHours = BigDecimal.ZERO;

    // Constructors
    public AttendanceStatsDTO() {}

    public AttendanceStatsDTO(long totalDays, long presentDays, long lateDays, long halfDays, long absentDays, BigDecimal totalWorkingHours) {
        this.totalDays = totalDays;
        this.presentDays = presentDays;
        this.lateDays = lateDays;
        this.halfDays = halfDays;
        this.absentDays = absentDays;
        this.totalWorkingHours = totalWorkingHours != null ? totalWorkingHours : BigDecimal.ZERO;
    }

    // Getters and Setters
    public long getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(long totalDays) {
        this.totalDays = totalDays;
    }

    public long getPresentDays() {
        return presentDays;
    }

    public void setPresentDays(long presentDays) {
        this.presentDays = presentDays;
    }

    public long getLateDays() {
        return lateDays;
    }

    public void setLateDays(long lateDays) {
        this.lateDays = lateDays;
    }

    public long getHalfDays() {
        return halfDays;
    }

    public void setHalfDays(long halfDays) {
        this.halfDays = halfDays;
    }

    public long getAbsentDays() {
        return absentDays;
    }

    public void setAbsentDays(long absentDays) {
        this.absentDays = absentDays;
    }

    public BigDecimal getTotalWorkingHours() {
        return totalWorkingHours;
    }

    public void setTotalWorkingHours(BigDecimal totalWorkingHours) {
        this.totalWorkingHours = totalWorkingHours != null ? totalWorkingHours : BigDecimal.ZERO;
    }
}