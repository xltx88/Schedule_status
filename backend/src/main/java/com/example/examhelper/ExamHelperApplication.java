package com.example.examhelper;

import com.example.examhelper.model.Task;
import com.example.examhelper.repository.TaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExamHelperApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamHelperApplication.class, args);
    }

    @Bean
    public CommandLineRunner dataLoader(TaskRepository taskRepository) {
        return args -> {
            if (taskRepository.count() == 0) {
                Task leaveTask = new Task();
                leaveTask.setName("离岗");
                leaveTask.setUserId(null); // System default
                leaveTask.setIsActive(true);
                taskRepository.save(leaveTask);
                System.out.println("Initialized '离岗' task with ID: " + leaveTask.getId());
            }
        };
    }
}
