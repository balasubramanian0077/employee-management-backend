package com.bala.employeemanagement.service;

import com.bala.employeemanagement.model.Task;
import com.bala.employeemanagement.model.TaskStatus;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.repository.TaskRepository;
import com.bala.employeemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<Task> getTasksForCurrentUser() {
        User currentUser = getCurrentUser();
        switch (currentUser.getRole()) {
            case EMPLOYEE:
                return taskRepository.findByAssignedTo(currentUser);
            case MANAGER:
                return taskRepository.findByAssignedBy(currentUser);
            case ADMIN:
                return taskRepository.findAll();
            default:
                return List.of();
        }
    }

    public Task createTask(Task task) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() != com.bala.employeemanagement.model.Role.MANAGER) {
            throw new RuntimeException("Only managers can create tasks");
        }
        User assignedTo = userRepository.findById(task.getAssignedTo().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (assignedTo.getRole() != com.bala.employeemanagement.model.Role.EMPLOYEE) {
            throw new RuntimeException("Tasks can only be assigned to employees");
        }
        task.setAssignedBy(currentUser);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    public Task updateTaskStatus(Long id, TaskStatus status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == com.bala.employeemanagement.model.Role.EMPLOYEE &&
                !task.getAssignedTo().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only update your own tasks");
        }
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }
}