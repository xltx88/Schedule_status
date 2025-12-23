package com.example.examhelper.repository;

import com.example.examhelper.model.ChineseDictionary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChineseDictionaryRepository extends JpaRepository<ChineseDictionary, Long> {
    Page<ChineseDictionary> findByWordContaining(String word, Pageable pageable);

    @Query(value = "SELECT * FROM chinese_dictionary ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<ChineseDictionary> findRandom(int limit);
}
