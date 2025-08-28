package com.snehit.springboottaskapi.dataenrich;

import com.snehit.springboottaskapi.entity.Task;
import com.snehit.springboottaskapi.enums.TaskStatus;
import com.snehit.springboottaskapi.repository.ITaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    private final ITaskRepository taskRepository;

    @Autowired
    public DataLoader(ITaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Load sample data only if database is empty
        if (taskRepository.count() == 0) {
            loadSampleData();
        }
    }

    private void loadSampleData() {
        // Create sample tasks
        Task task1 = new Task();
        task1.setTitle("Complete Project Documentation");
        task1.setDescription("Write comprehensive documentation for the task manager project");
        task1.setStatus(TaskStatus.TODO);
        task1.setDueDate(LocalDateTime.now().plusDays(5));

        Task task2 = new Task();
        task2.setTitle("Review Code Changes");
        task2.setDescription("Review and approve pending pull requests");
        task2.setStatus(TaskStatus.IN_PROGRESS);
        task2.setDueDate(LocalDateTime.now().plusDays(2));

        Task task3 = new Task();
        task3.setTitle("Deploy to Production");
        task3.setDescription("Deploy the latest version to production environment");
        task3.setStatus(TaskStatus.TODO);
        task3.setDueDate(LocalDateTime.now().plusDays(7));

        Task task4 = new Task();
        task4.setTitle("Setup CI/CD Pipeline");
        task4.setDescription("Configure continuous integration and deployment");
        task4.setStatus(TaskStatus.COMPLETED);
        task4.setDueDate(LocalDateTime.now().plusDays(10));

        Task task5 = new Task();
        task5.setTitle("Database Migration");
        task5.setDescription("Migrate database schema to latest version");
        task5.setStatus(TaskStatus.IN_PROGRESS);
        task5.setDueDate(LocalDateTime.now().plusDays(3));

        // Save sample tasks
        taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(task3);
        taskRepository.save(task4);
        taskRepository.save(task5);

        System.out.println("Sample data loaded successfully!");
    }
}