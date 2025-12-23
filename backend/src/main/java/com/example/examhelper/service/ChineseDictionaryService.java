package com.example.examhelper.service;

import com.example.examhelper.model.ChineseDictionary;
import com.example.examhelper.repository.ChineseDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChineseDictionaryService {

    @Autowired
    private ChineseDictionaryRepository repository;

    public Page<ChineseDictionary> getIdioms(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findAll(pageable);
    }

    public Page<ChineseDictionary> searchIdioms(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findByWordContaining(keyword, pageable);
    }

    public List<ChineseDictionary> getRandomIdioms(int limit) {
        return repository.findRandom(limit);
    }
}
