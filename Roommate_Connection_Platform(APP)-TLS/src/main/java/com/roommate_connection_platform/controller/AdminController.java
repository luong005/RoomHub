package com.roommate_connection_platform.controller;

import com.roommate_connection_platform.DTO.Admin.AdminUserResponseDTO;
import com.roommate_connection_platform.services.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final IUserService userService;

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponseDTO>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<AdminUserResponseDTO> users = userService.getUsers(page, size);
        return ResponseEntity.ok(users);
    }
}
