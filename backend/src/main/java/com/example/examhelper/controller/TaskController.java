package com.example.examhelper.controller;

import com.example.examhelper.model.Task;
import com.example.examhelper.service.TaskService;
import com.example.examhelper.config.UserContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    private void checkUser(Long userId) {
        com.example.examhelper.model.User currentUser = UserContext.getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access");
        }
    }

    @GetMapping
    public ResponseEntity<List<Task>> getTasks(@RequestParam Long userId) {
        checkUser(userId);
        return ResponseEntity.ok(taskService.getTasks(userId));
    }

    @PostMapping
    public ResponseEntity<Task> addTask(@RequestBody AddTaskRequest request) {
        checkUser(request.getUserId());
        return ResponseEntity.ok(taskService.addTask(request.getName(), request.getUserId()));
    }

    @PostMapping("/switch")
    public ResponseEntity<?> switchTask(@RequestBody SwitchTaskRequest request) {
        checkUser(request.getUserId());
        return ResponseEntity.ok(taskService.switchTask(request.getUserId(), request.getTaskId()));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId, @RequestParam Long userId) {
        checkUser(userId);
        return ResponseEntity.ok(taskService.deleteTask(userId, taskId));
    }

    @GetMapping("/stats/pie")
    public ResponseEntity<Map<String, Object>> getPieChart(@RequestParam Long userId, @RequestParam(required = false) String date) {
        checkUser(userId);
        if (date == null) date = LocalDate.now().toString();
        return ResponseEntity.ok(taskService.getPieChartData(userId, date));
    }

    @GetMapping("/stats/line")
    public ResponseEntity<Map<String, Object>> getLineChart(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        checkUser(userId);
        
        if (endDate == null) endDate = LocalDate.now().toString();
        if (startDate == null) startDate = LocalDate.now().minusDays(6).toString(); // Default 1 week
        
        return ResponseEntity.ok(taskService.getLineChartData(userId, startDate, endDate));
    }

    @GetMapping("/stats/rankings")
    public ResponseEntity<Map<String, Object>> getRankings(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        checkUser(userId);
        return ResponseEntity.ok(taskService.getRankingStats(userId, startDate, endDate));
    }

    @GetMapping("/timeline")
    public ResponseEntity<List<Map<String, Object>>> getTimeline(@RequestParam Long userId, @RequestParam(required = false) String date) {
        checkUser(userId);
        if (date == null) date = LocalDate.now().toString();
        return ResponseEntity.ok(taskService.getTimelineData(userId, date));
    }

    @GetMapping("/checkin")
    public ResponseEntity<Map<String, Object>> getCheckInStatus(@RequestParam Long userId) {
        checkUser(userId);
        return ResponseEntity.ok(taskService.getCheckInStatus(userId));
    }

    @PostMapping("/order")
    public ResponseEntity<?> updateTaskOrder(@RequestBody UpdateOrderRequest request) {
        checkUser(request.getUserId());
        taskService.updateTaskOrder(request.getUserId(), request.getTaskIds());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{taskId}/records-tag")
    public ResponseEntity<?> updateTaskRecordsTag(@PathVariable Long taskId, @RequestBody UpdateRecordsTagRequest request) {
        com.example.examhelper.model.User currentUser = UserContext.getCurrentUser();
        taskService.updateTaskRecordsTag(currentUser.getId(), taskId, request.getRecordsTag());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/records/{recordId}")
    public ResponseEntity<?> updateTimeRecord(@PathVariable Long recordId, @RequestBody UpdateTimeRecordRequest request) {
        checkUser(request.getUserId());
        taskService.updateTimeRecord(request.getUserId(), recordId, request.getStartTime(), request.getEndTime());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/grant-edit")
    public ResponseEntity<?> grantTimeEditPermission(@RequestBody GrantPermissionRequest request) {
        com.example.examhelper.model.User currentUser = UserContext.getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
             return ResponseEntity.status(403).body("Access denied");
        }
        if (!currentUser.getId().equals(request.getAdminId())) {
             return ResponseEntity.status(403).body("Identity mismatch");
        }
        taskService.grantTimeEditPermission(request.getAdminId(), request.getTargetUserId(), request.getCanEdit());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settle")
    public ResponseEntity<?> settleDailyTask(@RequestBody SettleTaskRequest request) {
        checkUser(request.getUserId());
        taskService.settleUserDailyTask(request.getUserId());
        return ResponseEntity.ok().build();
    }

    @Data
    public static class AddTaskRequest {
        private String name;
        private Long userId;
    }

    @Data
    public static class SwitchTaskRequest {
        private Long userId;
        private Long taskId;
    }

    @Data
    public static class UpdateOrderRequest {
        private Long userId;
        private List<Long> taskIds;
    }

    @Data
    public static class UpdateRecordsTagRequest {
        private Boolean recordsTag;
    }

    @Data
    public static class UpdateTimeRecordRequest {
        private Long userId;
        private Long startTime;
        private Long endTime;
    }

    @Data
    public static class GrantPermissionRequest {
        private Long adminId;
        private Long targetUserId;
        private Boolean canEdit;
    }

    @Data
    public static class SettleTaskRequest {
        private Long userId;
    }
}
