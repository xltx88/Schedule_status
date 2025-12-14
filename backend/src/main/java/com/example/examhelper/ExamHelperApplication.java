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
            Task leaveTask = taskRepository.findById(1L).orElse(null);
            if (leaveTask == null) {
                leaveTask = new Task();
                leaveTask.setId(1L); // Force ID 1
                leaveTask.setName("离岗");
                leaveTask.setUserId(null); // System default
                leaveTask.setIsActive(true);
                leaveTask.setDeleted(false);
                taskRepository.save(leaveTask);
                System.out.println("Initialized '离岗' task with ID: 1");
            } else {
                // Force update to ensure correct state (especially userId = NULL)
                boolean needsUpdate = false;
                
                // Ensure name is correct
                if (!"离岗".equals(leaveTask.getName())) {
                    leaveTask.setName("离岗");
                    needsUpdate = true;
                }
                
                // Ensure userId is NULL (Global task)
                // Note: In DB, if it is not null, we must set it to null.
                if (leaveTask.getUserId() != null) {
                    System.out.println("Found '离岗' task with non-null userId: " + leaveTask.getUserId() + ". Fixing to NULL.");
                    leaveTask.setUserId(null);
                    needsUpdate = true;
                }
                
                // Ensure not deleted
                if (Boolean.TRUE.equals(leaveTask.getDeleted())) {
                    leaveTask.setDeleted(false);
                    needsUpdate = true;
                }
                
                if (needsUpdate) {
                    taskRepository.save(leaveTask);
                    System.out.println("Updated '离岗' task to correct state: userId=NULL, deleted=false");
                } else {
                    System.out.println("'离岗' task is already in correct state.");
                }
            }
        };
    }
}
