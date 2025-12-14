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
        taskService.deleteTask(userId, taskId);
        return ResponseEntity.ok().build();
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
}
