package com.example.examhelper.controller;

import com.example.examhelper.model.ChineseDictionary;
import com.example.examhelper.service.ChineseDictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/idioms")
public class ChineseDictionaryController {

    @Autowired
    private ChineseDictionaryService service;

    @GetMapping
    public ResponseEntity<?> getAllIdioms(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        try {
            System.out.println("Received request for idioms with keyword: " + keyword + ", page: " + page + ", size: " + size);
            Page<ChineseDictionary> result;
            if (keyword != null && !keyword.isEmpty()) {
                result = service.searchIdioms(keyword, page, size);
            } else {
                result = service.getIdioms(page, size);
            }
            return ResponseEntity.ok(result);
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching idioms: " + e.getMessage());
        }
    }

    @GetMapping("/random")
    public ResponseEntity<?> getRandomIdioms(@RequestParam(defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(service.getRandomIdioms(limit));
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching random idioms: " + e.getMessage());
        }
    }
}
