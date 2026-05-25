package com.bala.employeemanagement.controller;

import com.bala.employeemanagement.model.Role;
import com.bala.employeemanagement.model.User;
import com.bala.employeemanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (user.getRole() != Role.MANAGER && user.getRole() != Role.EMPLOYEE) {
            return ResponseEntity.badRequest().body("Only MANAGER and EMPLOYEE can register");
        }
        User registeredUser = userService.register(user);
        return ResponseEntity.ok(registeredUser);
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<User>> getAllEmployees() {
        List<User> employees = userService.findAllByRole(Role.EMPLOYEE);
        return ResponseEntity.ok(employees);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
            "name", user.getName(),
            "email", user.getEmail(),
            "role", user.getRole().name()
        ));
    }

    // Get user profile (with profile picture)
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        String pictureUrl = user.getProfilePicture() != null ? "/uploads/profile/" + user.getProfilePicture() : null;
        return ResponseEntity.ok(Map.of(
            "name", user.getName(),
            "email", user.getEmail(),
            "role", user.getRole().name(),
            "profilePicture", pictureUrl
        ));
    }

    // Upload profile picture
    @PostMapping("/upload-profile-picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadProfilePicture(@RequestParam("file") MultipartFile file,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/jpg") || contentType.equals("image/png"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only JPG, JPEG and PNG files are allowed"));
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size must be less than 5MB"));
            }

            // Create directory if not exists
            String profileDir = uploadDir + "/profile/";
            File dir = new File(profileDir);
            if (!dir.exists()) dir.mkdirs();

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = user.getId() + "_" + UUID.randomUUID() + extension;
            String filePath = profileDir + filename;

            // Save file
            file.transferTo(new File(filePath));

            // Delete old profile picture if exists
            if (user.getProfilePicture() != null) {
                File oldFile = new File(profileDir + user.getProfilePicture());
                if (oldFile.exists()) oldFile.delete();
            }

            // Update database
            user.setProfilePicture(filename);
            userService.updateUser(user);

            return ResponseEntity.ok(Map.of(
                "message", "Profile picture updated successfully",
                "profilePictureUrl", "/uploads/profile/" + filename
            ));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

    // Remove profile picture
    @DeleteMapping("/remove-profile-picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> removeProfilePicture(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user.getProfilePicture() != null) {
            String profileDir = uploadDir + "/profile/";
            File oldFile = new File(profileDir + user.getProfilePicture());
            if (oldFile.exists()) oldFile.delete();
            user.setProfilePicture(null);
            userService.updateUser(user);
        }
        return ResponseEntity.ok(Map.of("message", "Profile picture removed"));
    }
}