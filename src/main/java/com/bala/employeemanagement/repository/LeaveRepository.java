package com.bala.employeemanagement.repository;

import com.bala.employeemanagement.model.Leave;
import com.bala.employeemanagement.model.LeaveStatus;
import com.bala.employeemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeaveRepository extends JpaRepository<Leave, Long> {
    List<Leave> findByUser(User user);
    List<Leave> findByStatus(LeaveStatus status);
    long countByUserAndStatus(User user, LeaveStatus status);
}