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
import java.time.ZonedDateTime;
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
        
        // Use logical date (4 AM rule)
        record.setRecordDate(getLogicalDate(startTime));
        
        record.setCreatedAt(LocalDateTime.now());
        
        timeRecordRepository.save(record);
    }

    private String getLogicalDate(long timestamp) {
        ZonedDateTime zdt = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault());
        if (zdt.getHour() < 4) {
            zdt = zdt.minusDays(1);
        }
        return zdt.toLocalDate().format(DATE_FORMATTER);
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

    // Check mandatory task selection every 10 seconds between 8:00 and 23:00 (stops at 23:00)
    // Modified to 8-22 to avoid conflict with manual settlement after 23:00
    @Scheduled(cron = "*/10 * 8-22 * * ?")
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

    // 24:00 (Midnight) Schedule -> Changed to 04:00 AM
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    public void settleDailyTasks() {
        log.info("Running 4 AM Settlement");
        List<User> users = userRepository.findAll();
        long now = System.currentTimeMillis();

        for (User user : users) {
            settleUserTask(user, now);
        }
    }

    @Transactional
    public void settleUserDailyTask(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        
        // Validate time: Must be after 23:00
        LocalTime time = LocalTime.now();
        if (time.getHour() < 23 && time.getHour() >= 4) {
             throw new RuntimeException("未到23:00");
        }
        
        settleUserTask(user, System.currentTimeMillis());
    }

    private void settleUserTask(User user, long now) {
        if (user.getCurrentTaskId() != null && user.getCurrentTaskStartTime() != null) {
            // Calculate duration
            long startTime = user.getCurrentTaskStartTime();
            long duration = now - startTime;

            // Use the start time to determine the record date
            // With 4 AM settlement, tasks starting before 4 AM belong to the previous logical day
            String recordDate = getLogicalDate(startTime);

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
        Map<Long, Boolean> taskRecordsTags = allTasks.stream().collect(Collectors.toMap(Task::getId, t -> {
            if (Boolean.TRUE.equals(t.getRecordsTag())) return true;
            if (t.getRecordsTag() == null && t.getUserId() != null) return true;
            return false;
        }));

        List<Map<String, Object>> timeline = new ArrayList<>();
        
        for (TimeRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("taskName", taskNames.getOrDefault(record.getTaskId(), "Unknown"));
            item.put("startTime", record.getStartTime());
            item.put("endTime", record.getEndTime());
            item.put("duration", record.getDuration());
            item.put("isSystem", taskUserIds.get(record.getTaskId()) == 0L);
            item.put("recordsTag", taskRecordsTags.getOrDefault(record.getTaskId(), false));
            timeline.add(item);
        }

        // Add current task if it's today
        if (LocalDate.now().format(DATE_FORMATTER).equals(date)) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getCurrentTaskId() != null && user.getCurrentTaskStartTime() != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", "CURRENT");
                item.put("taskName", taskNames.getOrDefault(user.getCurrentTaskId(), "Unknown"));
                item.put("startTime", user.getCurrentTaskStartTime());
                item.put("endTime", System.currentTimeMillis());
                item.put("duration", System.currentTimeMillis() - user.getCurrentTaskStartTime());
                item.put("isSystem", taskUserIds.get(user.getCurrentTaskId()) == 0L);
                item.put("recordsTag", taskRecordsTags.getOrDefault(user.getCurrentTaskId(), false));
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

    public void updateTaskRecordsTag(Long userId, Long taskId, Boolean recordsTag) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        if (task.getUserId() == null || !task.getUserId().equals(userId)) {
             throw new RuntimeException("Unauthorized to modify this task");
        }
        task.setRecordsTag(recordsTag);
        taskRepository.save(task);
    }

    public Map<String, Object> getRankingStats(Long userId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. Today's Ranking
        // Logic: Ranking is based on "record_date". Since we changed settlement to 4 AM,
        // records generated between yesterday 4 AM and today 4 AM are considered "yesterday" (or previous day)
        // BUT, our TimeRecord has a "recordDate" field which is set at the time of creation/settlement.
        // If we settle at 4 AM, the recordDate should logically belong to the "previous day" if it represents the previous day's work.
        // However, the current implementation sets recordDate based on startTime.
        // "String recordDate = Instant.ofEpochMilli(startTime)...toLocalDate().format..."
        // So if a task started at 23:00 yesterday and ended at 4:00 today, startTime is yesterday, so recordDate is yesterday.
        // This is consistent with "Daily" stats.
        
        // So "Today's Ranking" means ranking for the current logical day.
        // If it's 2 AM now, we are still in "Yesterday" logically until 4 AM settlement?
        // The user requirement says "Yesterday's ranking... should follow settlement time".
        // Actually, since we store recordDate based on StartTime, the date is already "correct" for most tasks.
        // The only ambiguity is tasks starting after 00:00 but before 04:00.
        // If I start a task at 01:00 AM, it belongs to "Yesterday" logically if settlement is 4 AM.
        // But currently Instant.ofEpochMilli(startTime) will give Today's date.
        
        // To fix this properly, we should adjust how recordDate is calculated in settleUserTask/settleDailyTasks.
        // But for now, let's assume recordDate in DB is the source of truth for "Day".
        
        // Wait, the user asked to check if yesterday's ranking is correct with new settlement time.
        // If we just query by record_date, we rely on how record_date was saved.
        // Let's look at settleUserTask again.
        
        String today = LocalDate.now().format(DATE_FORMATTER);
        // If current time < 4 AM, "Today" for display might actually be "Yesterday" in calendar?
        // Or does the user mean "Current active day"?
        // Usually "Today" means the date of the day we are working on.
        // If I work at 1 AM on Dec 28, it's still Dec 27's work session until 4 AM Dec 28.
        // So we should shift the date if LocalTime.now().getHour() < 4.
        
        LocalDate logicalToday = LocalDate.now();
        if (LocalTime.now().getHour() < 4) {
            logicalToday = logicalToday.minusDays(1);
        }
        String logicalTodayStr = logicalToday.format(DATE_FORMATTER);
        
        List<Object[]> todayStats = timeRecordRepository.findUserDurationsByDate(logicalTodayStr);
        long totalUsers = userRepository.count();
        
        int myRank = -1;
        for (int i = 0; i < todayStats.size(); i++) {
            Long uid = ((Number) todayStats.get(i)[0]).longValue();
            if (uid.equals(userId)) {
                myRank = i + 1;
                break;
            }
        }
        
        if (myRank != -1) {
            result.put("todayRank", myRank + "/" + totalUsers);
        } else {
            result.put("todayRank", "-/" + totalUsers);
        }

        // 2. Cumulative Duration (Range based)
        // If startDate/endDate provided, use them. Else total.
        Long totalDuration;
        if (startDate != null && endDate != null) {
             totalDuration = timeRecordRepository.getTotalDurationByUserIdAndDateRange(userId, startDate, endDate);
        } else {
             totalDuration = timeRecordRepository.getTotalDurationByUserId(userId);
        }
        
        if (totalDuration == null) totalDuration = 0L;
        result.put("totalDuration", formatDuration(totalDuration));

        // 3. Yesterday's Top 3
        // Yesterday relative to logicalToday
        String logicalYesterdayStr = logicalToday.minusDays(1).format(DATE_FORMATTER);
        List<Object[]> yesterdayStats = timeRecordRepository.findUserDurationsByDate(logicalYesterdayStr);
        
        List<Map<String, Object>> top3 = new ArrayList<>();
        int limit = Math.min(yesterdayStats.size(), 3);
        
        for (int i = 0; i < limit; i++) {
            Long uid = ((Number) yesterdayStats.get(i)[0]).longValue();
            User u = userRepository.findById(uid).orElse(null);
            String name = (u != null) ? u.getUsername() : "Unknown";
            
            Map<String, Object> entry = new HashMap<>();
            entry.put("rank", i + 1);
            entry.put("name", name);
            top3.add(entry);
        }
        result.put("yesterdayTop3", top3);
        
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

    @Transactional
    public void updateTimeRecord(Long userId, Long recordId, Long newStartTime, Long newEndTime) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (!Boolean.TRUE.equals(user.getCanEditTime())) {
            throw new RuntimeException("Permission denied: Cannot edit time records");
        }

        TimeRecord record = timeRecordRepository.findById(recordId).orElseThrow(() -> new RuntimeException("Record not found"));
        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("Permission denied: Record does not belong to user");
        }

        if (newStartTime >= newEndTime) {
            throw new RuntimeException("Start time must be before end time");
        }

        long oldStartTime = record.getStartTime();
        long oldEndTime = record.getEndTime();
        String recordDate = record.getRecordDate();

        record.setStartTime(newStartTime);
        record.setEndTime(newEndTime);
        record.setDuration(newEndTime - newStartTime);
        
        // Update recordDate based on newStartTime (Logical Day)
        ZonedDateTime zdt = Instant.ofEpochMilli(newStartTime).atZone(ZoneId.systemDefault());
        if (zdt.getHour() < 4) {
            zdt = zdt.minusDays(1);
        }
        record.setRecordDate(zdt.toLocalDate().format(DATE_FORMATTER));
        
        timeRecordRepository.save(record);

        List<TimeRecord> dayRecords = timeRecordRepository.findByUserIdAndRecordDate(userId, recordDate);
        
        if (newEndTime != oldEndTime) {
            for (TimeRecord r : dayRecords) {
                if (r.getId().equals(record.getId())) continue;
                if (Math.abs(r.getStartTime() - oldEndTime) < 5000) {
                    r.setStartTime(newEndTime);
                    r.setDuration(r.getEndTime() - r.getStartTime());
                    timeRecordRepository.save(r);
                }
            }
            if (user.getCurrentTaskId() != null && user.getCurrentTaskStartTime() != null) {
                 if (Math.abs(user.getCurrentTaskStartTime() - oldEndTime) < 5000) {
                     user.setCurrentTaskStartTime(newEndTime);
                     userRepository.save(user);
                 }
            }
        }

        if (newStartTime != oldStartTime) {
             for (TimeRecord r : dayRecords) {
                if (r.getId().equals(record.getId())) continue;
                if (Math.abs(r.getEndTime() - oldStartTime) < 5000) {
                    r.setEndTime(newStartTime);
                    r.setDuration(r.getEndTime() - r.getStartTime());
                    timeRecordRepository.save(r);
                }
            }
        }
    }

    public void grantTimeEditPermission(Long adminId, Long targetUserId, Boolean canEdit) {
        User admin = userRepository.findById(adminId).orElseThrow(() -> new RuntimeException("Admin not found"));
        if (!"ADMIN".equals(admin.getRole())) {
             throw new RuntimeException("Permission denied: Not an admin");
        }
        
        User target = userRepository.findById(targetUserId).orElseThrow(() -> new RuntimeException("User not found"));
        target.setCanEditTime(canEdit);
        userRepository.save(target);
    }
}
