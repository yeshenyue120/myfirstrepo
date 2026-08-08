package com.example.calorieserver.controller;

import com.example.calorieserver.dto.UserResponse;
import com.example.calorieserver.service.ExportService;
import com.example.calorieserver.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ExportService exportService;
    private final UserService userService;

    /**
     * 导出用户全部数据
     * GET /api/users/{userId}/export
     */
    @GetMapping("/{userId}/export")
    public ResponseEntity<Map<String, Object>> exportUser(@PathVariable Long userId) {
        return ResponseEntity.ok(exportService.exportUserData(userId));
    }

    /**
     * 获取最新用户资料（含重算后的热量/营养素目标，供前端体重操作后同步）
     * GET /api/users/{userId}/profile
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserResponse> getProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }
}
