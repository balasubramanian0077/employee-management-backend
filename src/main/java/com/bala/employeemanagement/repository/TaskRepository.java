package com.bala.employeemanagement.repository;

import com.bala.employeemanagement.model.Task;
import com.bala.employeemanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedTo(User user);
    List<Task> findByAssignedBy(User user);
}