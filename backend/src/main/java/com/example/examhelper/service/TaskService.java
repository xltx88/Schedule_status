package com.example.examhelper.service;

import com.example.examhelper.model.Task;
import com.example.examhelper.model.TimeRecord;
import com.example.examhelper.model.User;
import com.example.examhelper.repository.TaskRepository;
import com.example.examhelper.repository.TimeRecordRepository;
import com.example.examhelper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TimeRecordRepository timeRecordRepository;
    private final UserRepository userRepository;

    private static final Long LEAVE_TASK_ID = 1L;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<Task> getTasks(Long userId) {
        return taskRepository.findByUserIdOrUserIdIsNull(userId);
    }

    public Task addTask(String name, Long userId) {
        Task task = new Task();
        task.setName(name);
        task.setUserId(userId);
        task.setDeleted(false);
        return taskRepository.save(task);
    }

    public void deleteTask(Long userId, Long taskId) {
        if (LEAVE_TASK_ID.equals(taskId)) {
            throw new RuntimeException("Cannot delete Leave task");
        }
        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
                
        if (!userId.equals(task.getUserId())) {
            throw new RuntimeException("Unauthorized to delete this task");
        }
        
        task.setDeleted(true);
        taskRepository.save(task);
    }

    @Transactional
    public User switchTask(Long userId, Long newTaskId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        long now = System.currentTimeMillis();

        // 1. Settle current task if exists
        if (user.getCurrentTaskId() != null && user.getCurrentTaskStartTime() != null) {
            saveTimeRecord(user, now);
        }

        // 2. Start new task
        user.setCurrentTaskId(newTaskId);
        user.setCurrentTaskStartTime(now);
        return userRepository.save(user);
    }

    private void saveTimeRecord(User user, long endTime) {
        long startTime = user.getCurrentTaskStartTime();
        long duration = endTime - startTime;
        
        if (duration < 1000) return; // Ignore very short durations (< 1s)

        TimeRecord record = new TimeRecord();
        record.setUserId(user.getId());
        record.setTaskId(user.getCurrentTaskId());
        record.setStartTime(startTime);
        record.setEndTime(endTime);
        record.setDuration(duration);
        record.setRecordDate(LocalDate.now().format(DATE_FORMATTER));
        record.setCreatedAt(LocalDateTime.now());
        
        timeRecordRepository.save(record);
    }

    // 8:00 AM Schedule
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkMorningTasks() {
        log.info("Running 8 AM Task Check");
        List<User> users = userRepository.findAll();
        long now = System.currentTimeMillis();

        for (User user : users) {
            // If task is empty, set to Leave
            if (user.getCurrentTaskId() == null) {
                user.setCurrentTaskId(LEAVE_TASK_ID);
                user.setCurrentTaskStartTime(now);
                userRepository.save(user);
                log.info("Set user {} to Leave status", user.getUsername());
            }
            // If task is not empty (user started early), do nothing, keep counting.
        }
    }

    // Check mandatory task selection every minute between 8:00 and 24:00
    @Scheduled(cron = "0 * 8-23 * * ?")
    @Transactional
    public void checkMandatoryTask() {
        List<User> users = userRepository.findAll();
        long now = System.currentTimeMillis();
        
        for (User user : users) {
             if (user.getCurrentTaskId() == null) {
                 user.setCurrentTaskId(LEAVE_TASK_ID);
                 user.setCurrentTaskStartTime(now);
                 userRepository.save(user);
                 log.info("Mandatory task enforcement: Set user {} to Leave status", user.getUsername());
             }
        }
    }

    // 24:00 (Midnight) Schedule
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void settleDailyTasks() {
        log.info("Running Midnight Settlement");
        List<User> users = userRepository.findAll();
        long now = System.currentTimeMillis();
        // The date for the record should be "Yesterday" because it's 00:00:00 of the new day
        String yesterdayDate = LocalDate.now().minusDays(1).format(DATE_FORMATTER);

        for (User user : users) {
            if (user.getCurrentTaskId() != null && user.getCurrentTaskStartTime() != null) {
                // Calculate duration
                long startTime = user.getCurrentTaskStartTime();
                long duration = now - startTime;

                TimeRecord record = new TimeRecord();
                record.setUserId(user.getId());
                record.setTaskId(user.getCurrentTaskId());
                record.setStartTime(startTime);
                record.setEndTime(now);
                record.setDuration(duration);
                record.setRecordDate(yesterdayDate); // Explicitly set to yesterday
                record.setCreatedAt(LocalDateTime.now());
                timeRecordRepository.save(record);

                // Clear user status
                user.setCurrentTaskId(null);
                user.setCurrentTaskStartTime(null);
                userRepository.save(user);
            }
        }
    }

    // Chart Data Helpers
    public Map<String, Object> getPieChartData(Long userId, String date) {
        List<TimeRecord> records = timeRecordRepository.findByUserIdAndRecordDate(userId, date);
        Map<Long, Long> durationByTask = records.stream()
                .collect(Collectors.groupingBy(TimeRecord::getTaskId, Collectors.summingLong(TimeRecord::getDuration)));

        List<Map<String, Object>> data = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : durationByTask.entrySet()) {
            Task task = taskRepository.findById(entry.getKey()).orElse(null);
            String taskName = (task != null) ? task.getName() : "Unknown Task";
            
            Map<String, Object> item = new HashMap<>();
            item.put("name", taskName);
            item.put("value", entry.getValue()); // Duration in ms
            item.put("formatted", formatDuration(entry.getValue()));
            data.add(item);
        }
        
        log.info("Pie Chart Data for user {} on {}: {}", userId, date, data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        return result;
    }

    public Map<String, Object> getLineChartData(Long userId, String startDate, String endDate) {
        List<TimeRecord> records = timeRecordRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        // Group by Date -> Sum Duration
        Map<String, Long> durationByDate = records.stream()
                .collect(Collectors.groupingBy(TimeRecord::getRecordDate, Collectors.summingLong(TimeRecord::getDuration)));

        List<String> dates = new ArrayList<>();
        List<Long> durations = new ArrayList<>();
        
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            String dateStr = date.format(DATE_FORMATTER);
            dates.add(dateStr);
            durations.add(durationByDate.getOrDefault(dateStr, 0L));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dates);
        result.put("durations", durations); // In ms, frontend can convert to hours
        return result;
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = (seconds % 3600) / 60;
        long hours = seconds / 3600;
        
        if (hours > 0) {
            return String.format("%d小时%d分", hours, minutes);
        } else {
            return String.format("%d分%d秒", minutes, seconds % 60);
        }
    }
}
