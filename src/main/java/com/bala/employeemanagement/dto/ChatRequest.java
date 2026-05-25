package com.bala.employeemanagement.dto;

public class ChatRequest {
    private String message;
    private Long userId; // optional, if you want to personalize

    // getters and setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}