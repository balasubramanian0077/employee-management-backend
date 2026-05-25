package com.bala.employeemanagement.repository;

import com.bala.employeemanagement.model.Attendance;
import com.bala.employeemanagement.model.AttendanceStatus;
import com.bala.employeemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {

    Optional<Attendance> findByEmployeeAndAttendanceDate(User employee, LocalDate date);

    List<Attendance> findByEmployeeOrderByAttendanceDateDesc(User employee);

    @Query("SELECT a FROM Attendance a WHERE a.employee = :employee AND a.attendanceDate BETWEEN :startDate AND :endDate")
    List<Attendance> findByEmployeeAndDateRange(@Param("employee") User employee,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
    long countByEmployeeAndAttendanceDateBetweenAndStatus(User employee, LocalDate start, LocalDate end, AttendanceStatus status);

}