package com.example.examhelper.service;

import com.example.examhelper.model.User;
import com.example.examhelper.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(u -> u.getPassword().equals(password))
                .orElse(null);
    }

    public User register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setCreatedAt(LocalDateTime.now());
        user.setDailyGoal(8); // Default
        return userRepository.save(user);
    }

    public User updateDailyGoal(Long userId, Integer goal) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setDailyGoal(goal);
        return userRepository.save(user);
    }


    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
