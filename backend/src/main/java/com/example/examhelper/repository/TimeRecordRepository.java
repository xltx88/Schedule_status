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

    @Query(value = "SELECT tr.user_id, SUM(tr.duration) FROM time_records tr " +
                   "JOIN tasks t ON tr.task_id = t.id " +
                   "WHERE tr.record_date = :date " +
                   "AND (t.records_tag = 1 OR (t.records_tag IS NULL AND t.user_id IS NOT NULL)) " +
                   "GROUP BY tr.user_id " +
                   "ORDER BY SUM(tr.duration) DESC", nativeQuery = true)
    List<Object[]> findUserDurationsByDate(@Param("date") String date);

    @Query(value = "SELECT SUM(tr.duration) FROM time_records tr " +
                   "JOIN tasks t ON tr.task_id = t.id " +
                   "WHERE tr.user_id = :userId " +
                   "AND (t.records_tag = 1 OR (t.records_tag IS NULL AND t.user_id IS NOT NULL))", nativeQuery = true)
    Long getTotalDurationByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT SUM(tr.duration) FROM time_records tr " +
                   "JOIN tasks t ON tr.task_id = t.id " +
                   "WHERE tr.user_id = :userId " +
                   "AND tr.record_date BETWEEN :startDate AND :endDate " +
                   "AND (t.records_tag = 1 OR (t.records_tag IS NULL AND t.user_id IS NOT NULL))", nativeQuery = true)
    Long getTotalDurationByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);
}
