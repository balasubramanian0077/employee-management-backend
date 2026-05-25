package com.bala.employeemanagement.service;

import com.bala.employeemanagement.dto.AttendanceFilterDTO;
import com.bala.employeemanagement.dto.AttendanceStatsDTO;
import com.bala.employeemanagement.model.Attendance;
import com.bala.employeemanagement.model.AttendanceStatus;
import com.bala.employeemanagement.model.Role;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.repository.AttendanceRepository;
import com.bala.employeemanagement.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    private static final LocalTime OFFICE_START = LocalTime.of(9, 30); // 9:30 AM

    // ========== Existing methods (unchanged) ==========
    @Transactional
    public Attendance checkIn(User employee) {
        LocalDate today = LocalDate.now();
        Attendance existing = attendanceRepository.findByEmployeeAndAttendanceDate(employee, today).orElse(null);
        if (existing != null && existing.getCheckInTime() != null) {
            throw new RuntimeException("Already checked in today");
        }

        Attendance attendance = (existing != null) ? existing : new Attendance();
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(today);
        attendance.setCheckInTime(LocalDateTime.now());

        LocalTime checkInTime = attendance.getCheckInTime().toLocalTime();
        attendance.setStatus(checkInTime.isAfter(OFFICE_START) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);

        return attendanceRepository.save(attendance);
    }

    @Transactional
    public Attendance checkOut(User employee) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByEmployeeAndAttendanceDate(employee, today)
                .orElseThrow(() -> new RuntimeException("No check-in found for today"));

        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("Already checked out today");
        }

        LocalDateTime checkOut = LocalDateTime.now();
        attendance.setCheckOutTime(checkOut);

        long minutes = ChronoUnit.MINUTES.between(attendance.getCheckInTime(), checkOut);
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        attendance.setTotalHours(hours);

        if (hours.compareTo(BigDecimal.valueOf(4)) < 0) {
            attendance.setStatus(AttendanceStatus.HALF_DAY);
        }

        return attendanceRepository.save(attendance);
    }

    public Attendance getTodayAttendance(User employee) {
        return attendanceRepository.findByEmployeeAndAttendanceDate(employee, LocalDate.now()).orElse(null);
    }

    public List<Attendance> getEmployeeHistory(User employee) {
        return attendanceRepository.findByEmployeeOrderByAttendanceDateDesc(employee);
    }

    public List<Attendance> getAllAttendances(AttendanceFilterDTO filter) {
        Specification<Attendance> spec = Specification.where(null);

        if (filter.getEmployeeId() != null) {
            User employee = userRepository.findById(filter.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("employee"), employee));
        }
        if (filter.getFromDate() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("attendanceDate"), filter.getFromDate()));
        }
        if (filter.getToDate() != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("attendanceDate"), filter.getToDate()));
        }
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            AttendanceStatus status = AttendanceStatus.valueOf(filter.getStatus().toUpperCase());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return attendanceRepository.findAll(spec);
    }

    public List<Attendance> getEmployeeAttendance(Long employeeId) {
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return attendanceRepository.findByEmployeeOrderByAttendanceDateDesc(employee);
    }

    public AttendanceStatsDTO getEmployeeMonthlyStats(User employee) {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = LocalDate.now();
        List<Attendance> attendances = attendanceRepository.findByEmployeeAndDateRange(employee, start, end);
        return calculateStats(attendances);
    }

    private AttendanceStatsDTO calculateStats(List<Attendance> attendances) {
        AttendanceStatsDTO stats = new AttendanceStatsDTO();
        stats.setTotalDays(attendances.size());
        for (Attendance a : attendances) {
            switch (a.getStatus()) {
                case PRESENT -> stats.setPresentDays(stats.getPresentDays() + 1);
                case LATE -> stats.setLateDays(stats.getLateDays() + 1);
                case HALF_DAY -> stats.setHalfDays(stats.getHalfDays() + 1);
                case ABSENT -> stats.setAbsentDays(stats.getAbsentDays() + 1);
            }
            if (a.getTotalHours() != null) {
                stats.setTotalWorkingHours(stats.getTotalWorkingHours().add(a.getTotalHours()));
            }
        }
        return stats;
    }

    @Transactional
    public void markAbsentForPreviousDays() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<User> employees = userRepository.findAllByRole(Role.EMPLOYEE);
        for (User emp : employees) {
            Attendance attendance = attendanceRepository.findByEmployeeAndAttendanceDate(emp, yesterday).orElse(null);
            if (attendance == null || attendance.getCheckInTime() == null) {
                if (attendance == null) {
                    attendance = new Attendance();
                    attendance.setEmployee(emp);
                    attendance.setAttendanceDate(yesterday);
                }
                attendance.setStatus(AttendanceStatus.ABSENT);
                attendanceRepository.save(attendance);
            }
        }
    }

    // ========== New method: Excel export ==========
    public byte[] generateAttendanceExcel(List<Attendance> attendances) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance Report");

        // Create styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd-MM-yyyy"));
        dateStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle timeStyle = workbook.createCellStyle();
        timeStyle.setDataFormat(createHelper.createDataFormat().getFormat("hh:mm AM/PM"));
        timeStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle hourStyle = workbook.createCellStyle();
        hourStyle.setDataFormat(createHelper.createDataFormat().getFormat("0.00"));
        hourStyle.setAlignment(HorizontalAlignment.RIGHT);

        // Headers row
        String[] columns = {"Employee", "Date", "Check In", "Check Out", "Hours", "Status"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        int rowNum = 1;
        for (Attendance att : attendances) {
            Row row = sheet.createRow(rowNum++);

            // Employee name
            row.createCell(0).setCellValue(att.getEmployee().getName());

            // Date
            Cell dateCell = row.createCell(1);
            if (att.getAttendanceDate() != null) {
                dateCell.setCellValue(att.getAttendanceDate());
                dateCell.setCellStyle(dateStyle);
            } else {
                dateCell.setCellValue("");
            }

            // Check In
            Cell checkInCell = row.createCell(2);
            if (att.getCheckInTime() != null) {
                checkInCell.setCellValue(att.getCheckInTime());
                checkInCell.setCellStyle(timeStyle);
            } else {
                checkInCell.setCellValue("-");
            }

            // Check Out
            Cell checkOutCell = row.createCell(3);
            if (att.getCheckOutTime() != null) {
                checkOutCell.setCellValue(att.getCheckOutTime());
                checkOutCell.setCellStyle(timeStyle);
            } else {
                checkOutCell.setCellValue("-");
            }

            // Hours
            Cell hoursCell = row.createCell(4);
            if (att.getTotalHours() != null) {
                hoursCell.setCellValue(att.getTotalHours().doubleValue());
                hoursCell.setCellStyle(hourStyle);
            } else {
                hoursCell.setCellValue("-");
            }

            // Status
            row.createCell(5).setCellValue(att.getStatus().toString());
        }

        // Auto-size columns (add extra width to avoid ###)
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(currentWidth + 500, 15000));
        }

        // Write to byte array
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            workbook.write(out);
            workbook.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
        return out.toByteArray();
    }
}