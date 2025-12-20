package com.example.examhelper.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "current_task_id")
    private Long currentTaskId;

    @Column(name = "current_task_start_time")
    private Long currentTaskStartTime;

    @Column(name = "daily_goal")
    private Integer dailyGoal = 8; // Default 8 hours

    @Column(name = "task_order", columnDefinition = "TEXT")
    private String taskOrder;
}
