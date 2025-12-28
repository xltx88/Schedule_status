package com.example.examhelper.controller;

import com.example.examhelper.model.Task;
import com.example.examhelper.service.TaskService;
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

    @GetMapping
    public ResponseEntity<List<Task>> getTasks(@RequestParam Long userId) {
        return ResponseEntity.ok(taskService.getTasks(userId));
    }

    @PostMapping
    public ResponseEntity<Task> addTask(@RequestBody AddTaskRequest request) {
        return ResponseEntity.ok(taskService.addTask(request.getName(), request.getUserId()));
    }

    @PostMapping("/switch")
    public ResponseEntity<?> switchTask(@RequestBody SwitchTaskRequest request) {
        return ResponseEntity.ok(taskService.switchTask(request.getUserId(), request.getTaskId()));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId, @RequestParam Long userId) {
        return ResponseEntity.ok(taskService.deleteTask(userId, taskId));
    }

    @GetMapping("/stats/pie")
    public ResponseEntity<Map<String, Object>> getPieChart(@RequestParam Long userId, @RequestParam(required = false) String date) {
        if (date == null) date = LocalDate.now().toString();
        return ResponseEntity.ok(taskService.getPieChartData(userId, date));
    }

    @GetMapping("/stats/line")
    public ResponseEntity<Map<String, Object>> getLineChart(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        if (endDate == null) endDate = LocalDate.now().toString();
        if (startDate == null) startDate = LocalDate.now().minusDays(6).toString(); // Default 1 week
        
        return ResponseEntity.ok(taskService.getLineChartData(userId, startDate, endDate));
    }

    @GetMapping("/stats/rankings")
    public ResponseEntity<Map<String, Object>> getRankings(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(taskService.getRankingStats(userId, startDate, endDate));
    }

    @GetMapping("/timeline")
    public ResponseEntity<List<Map<String, Object>>> getTimeline(@RequestParam Long userId, @RequestParam(required = false) String date) {
        if (date == null) date = LocalDate.now().toString();
        return ResponseEntity.ok(taskService.getTimelineData(userId, date));
    }

    @GetMapping("/checkin")
    public ResponseEntity<Map<String, Object>> getCheckInStatus(@RequestParam Long userId) {
        return ResponseEntity.ok(taskService.getCheckInStatus(userId));
    }

    @PostMapping("/order")
    public ResponseEntity<?> updateTaskOrder(@RequestBody UpdateOrderRequest request) {
        taskService.updateTaskOrder(request.getUserId(), request.getTaskIds());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{taskId}/records-tag")
    public ResponseEntity<?> updateTaskRecordsTag(@PathVariable Long taskId, @RequestBody UpdateRecordsTagRequest request) {
        taskService.updateTaskRecordsTag(taskId, request.getRecordsTag());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/records/{recordId}")
    public ResponseEntity<?> updateTimeRecord(@PathVariable Long recordId, @RequestBody UpdateTimeRecordRequest request) {
        taskService.updateTimeRecord(request.getUserId(), recordId, request.getStartTime(), request.getEndTime());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/grant-edit")
    public ResponseEntity<?> grantTimeEditPermission(@RequestBody GrantPermissionRequest request) {
        taskService.grantTimeEditPermission(request.getAdminId(), request.getTargetUserId(), request.getCanEdit());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settle")
    public ResponseEntity<?> settleDailyTask(@RequestBody SettleTaskRequest request) {
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
