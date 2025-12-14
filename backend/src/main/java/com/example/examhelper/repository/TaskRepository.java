package com.example.examhelper.repository;

import com.example.examhelper.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("SELECT t FROM Task t WHERE (t.userId = :userId OR t.userId IS NULL) AND t.deleted = false ORDER BY CASE WHEN t.id = 1 THEN 0 ELSE 1 END, t.id DESC")
    List<Task> findByUserIdOrUserIdIsNull(@Param("userId") Long userId);
    
    List<Task> findByUserId(Long userId);
}
