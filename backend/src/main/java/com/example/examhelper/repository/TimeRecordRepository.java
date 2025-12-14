package com.example.examhelper.repository;

import com.example.examhelper.model.TimeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TimeRecordRepository extends JpaRepository<TimeRecord, Long> {
    List<TimeRecord> findByUserIdAndRecordDate(Long userId, String recordDate);
    
    @Query("SELECT t FROM TimeRecord t WHERE t.userId = :userId AND t.recordDate BETWEEN :startDate AND :endDate")
    List<TimeRecord> findByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);
}
