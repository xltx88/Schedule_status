package com.example.examhelper.repository;

import com.example.examhelper.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserIdOrUserIdIsNull(Long userId);
    List<Task> findByUserId(Long userId);
}
