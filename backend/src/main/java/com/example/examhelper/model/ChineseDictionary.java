package com.example.examhelper.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chinese_dictionary")
public class ChineseDictionary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String word;

    @Column(name = "part_of_speech")
    private String partOfSpeech;

    @Column(columnDefinition = "TEXT")
    private String definition;

    @Column(columnDefinition = "TEXT")
    private String example;

    @Column(columnDefinition = "TEXT")
    private String synonyms;

    @Column(columnDefinition = "TEXT")
    private String antonyms;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.sql.Timestamp createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private java.sql.Timestamp updatedAt;
}
