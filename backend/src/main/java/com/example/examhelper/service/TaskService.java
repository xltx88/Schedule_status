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
import java.time.Instant;
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
        List<Task> tasks = taskRepository.findByUserIdOrUserIdIsNull(userId);
        User user = userRepository.findById(userId).orElse(null);

        if (user != null && user.getTaskOrder() != null && !user.getTaskOrder().isEmpty()) {
            String[] orderStrs = user.getTaskOrder().split(",");
            List<Long> orderList = new ArrayList<>();
            for (String s : orderStrs) {
                try {
                    orderList.add(Long.parseLong(s));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            Map<Long, Integer> orderMap = new HashMap<>();
            for (int i = 0; i < orderList.size(); i++) {
                orderMap.put(orderList.get(i), i);
            }

            tasks.sort((t1, t2) -> {
                Integer idx1 = orderMap.get(t1.getId());
                Integer idx2 = orderMap.get(t2.getId());

                if (idx1 != null && idx2 != null) {
                    return idx1.compareTo(idx2);
                } else if (idx1 != null) {
                    return -1; // t1 comes first
                } else if (idx2 != null) {
                    return 1; // t2 comes first
                } else {
                    // Both not in order list, fallback to default
                    return compareDefault(t1, t2);
                }
            });
        } else {
            tasks.sort(this::compareDefault);
        }

        return tasks;
    }

    private int compareDefault(Task t1, Task t2) {
        boolean t1Global = t1.getUserId() == null;
        boolean t2Global = t2.getUserId() == null;

        if (t1Global && !t2Global) return -1;
        if (!t1Global && t2Global) return 1;
        return t1.getId().compareTo(t2.getId());
    }

    @Transactional
    public void updateTaskOrder(Long userId, List<Long> taskIds) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String orderStr = taskIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        user.setTaskOrder(orderStr);
        userRepository.save(user);
    }

    public Task addTask(String name, Long userId) {
        Task task = new Task();
        task.setName(name);
        task.setUserId(userId);
        task.setDeleted(false);
        task.setRecordsTag(true); // Default to record
        return taskRepository.save(task);
    }

    @Transactional
    public User deleteTask(Long userId, Long taskId) {
        if (LEAVE_TASK_ID.equals(taskId)) {
            throw new RuntimeException("Cannot delete Leave task");
        }
        
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
                
        if (!userId.equals(task.getUserId())) {
            throw new RuntimeException("Unauthorized to delete this task");
        }

        // Check if the task to be deleted is the current task
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Remove task from sort order
        if (user.getTaskOrder() != null && !user.getTaskOrder().isEmpty()) {
            List<String> orderList = new ArrayList<>(Arrays.asList(user.getTaskOrder().split(",")));
            if (orderList.remove(String.valueOf(taskId))) {
                user.setTaskOrder(String.join(",", orderList));
                userRepository.save(user);
            }
        }
        
        if (taskId.equals(user.getCurrentTaskId())) {
            // Switch to Leave task to settle time and reset status
            user = switchTask(userId, LEAVE_TASK_ID);
        }
        
        task.setDeleted(true);
        taskRepository.save(task);
        return user;
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

    // Check mandatory task selection every 10 seconds between 8:00 and 24:00
    @Scheduled(cron = "*/10 * 8-23 * * ?")
    @Transactional
    public void checkMandatoryTask() {
        List<User> users = userRepository.findAll();
        long now = System.currentTimeMillis();
        
        for (User user : users) {
             if (user.getCurrentTaskId() == null) {
                 user.setCurrentTaskId(LEAVE_TASK_ID);
                 user.setCurrentTaskStartTime(now);
                 userRepository.save(user);
                 // Reduce log spam by checking if log level is debug or trace, or just remove info log for every 10s check
                 // log.info("Mandatory task enforcement: Set user {} to Leave status", user.getUsername());
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

        for (User user : users) {
            if (user.getCurrentTaskId() != null && user.getCurrentTaskStartTime() != null) {
                // Calculate duration
                long startTime = user.getCurrentTaskStartTime();
                long duration = now - startTime;

                // Use the start time to determine the record date to avoid issues with cron firing slightly off midnight
                String recordDate = Instant.ofEpochMilli(startTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DATE_FORMATTER);

                TimeRecord record = new TimeRecord();
                record.setUserId(user.getId());
                record.setTaskId(user.getCurrentTaskId());
                record.setStartTime(startTime);
                record.setEndTime(now);
                record.setDuration(duration);
                record.setRecordDate(recordDate);
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
        
        // Sort by value (duration) descending
        data.sort((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")));
        
        log.info("Pie Chart Data for user {} on {}: {}", userId, date, data);
        
        Map<String, Object> result = new HashMap<>();
        result.put("data", data);
        return result;
    }

    public Map<String, Object> getLineChartData(Long userId, String startDate, String endDate) {
        List<TimeRecord> records = timeRecordRepository.findByUserIdAndDateRange(userId, startDate, endDate);
        
        List<Task> allTasks = taskRepository.findAll();
        
        // Filter tasks based on recordsTag
        // If recordsTag is present: true -> include, false -> exclude
        // If recordsTag is null: fallback to old logic (exclude system tasks where userId is null)
        Set<Long> excludedTaskIds = allTasks.stream()
                .filter(t -> {
                    if (t.getRecordsTag() != null) {
                        return !t.getRecordsTag(); // Exclude if false (0)
                    }
                    return t.getUserId() == null; // Fallback: Exclude if system task
                })
                .map(Task::getId)
                .collect(Collectors.toSet());

        // Group by Date -> Sum Duration, filtering out excluded tasks
        Map<String, Long> durationByDate = records.stream()
                .filter(record -> !excludedTaskIds.contains(record.getTaskId()))
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
        result.put("durations", durations); // In ms
        return result;
    }

    public List<Map<String, Object>> getTimelineData(Long userId, String date) {
        List<TimeRecord> records = timeRecordRepository.findByUserIdAndRecordDate(userId, date);
        List<Task> allTasks = taskRepository.findAll();
        Map<Long, String> taskNames = allTasks.stream().collect(Collectors.toMap(Task::getId, Task::getName));
        Map<Long, Long> taskUserIds = allTasks.stream().collect(Collectors.toMap(Task::getId, t -> t.getUserId() == null ? 0L : t.getUserId()));

        List<Map<String, Object>> timeline = new ArrayList<>();
        
        for (TimeRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("taskName", taskNames.getOrDefault(record.getTaskId(), "Unknown"));
            item.put("startTime", record.getStartTime());
            item.put("endTime", record.getEndTime());
            item.put("duration", record.getDuration());
            item.put("isSystem", taskUserIds.get(record.getTaskId()) == 0L);
            timeline.add(item);
        }

        // Add current task if it's today
        if (LocalDate.now().format(DATE_FORMATTER).equals(date)) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getCurrentTaskId() != null && user.getCurrentTaskStartTime() != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("taskName", taskNames.getOrDefault(user.getCurrentTaskId(), "Unknown"));
                item.put("startTime", user.getCurrentTaskStartTime());
                item.put("endTime", System.currentTimeMillis());
                item.put("duration", System.currentTimeMillis() - user.getCurrentTaskStartTime());
                item.put("isSystem", taskUserIds.get(user.getCurrentTaskId()) == 0L);
                timeline.add(item);
            }
        }
        
        // Sort by start time
        timeline.sort((a, b) -> Long.compare((Long)a.get("startTime"), (Long)b.get("startTime")));
        
        return timeline;
    }

    public Map<String, Object> getCheckInStatus(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String endDate = LocalDate.now().format(DATE_FORMATTER);
        String startDate = LocalDate.now().minusDays(6).format(DATE_FORMATTER); // Last 7 days including today
        
        Map<String, Object> lineData = getLineChartData(userId, startDate, endDate);
        List<String> dates = (List<String>) lineData.get("dates");
        List<Long> durations = (List<Long>) lineData.get("durations");
        
        List<Map<String, Object>> statusList = new ArrayList<>();
        long goalMs = user.getDailyGoal() * 3600000L;
        
        for (int i = 0; i < dates.size(); i++) {
            Map<String, Object> status = new HashMap<>();
            status.put("date", dates.get(i));
            status.put("duration", durations.get(i));
            status.put("metGoal", durations.get(i) >= goalMs);
            statusList.add(status);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("dailyGoal", user.getDailyGoal());
        result.put("statusList", statusList);
        return result;
    }

    public void updateTaskRecordsTag(Long taskId, Boolean recordsTag) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        task.setRecordsTag(recordsTag);
        taskRepository.save(task);
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
