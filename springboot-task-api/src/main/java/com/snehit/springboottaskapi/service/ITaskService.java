package com.snehit.springboottaskapi.service;

import com.snehit.springboottaskapi.entity.Task;
import com.snehit.springboottaskapi.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ITaskService {

    Task createTask(Task task);

    Task getTaskById(Long id);

    Page<Task> getAllTasks(TaskStatus status, String title, Pageable pageable);

    Task updateTask(Long id, Task taskDetails);

    void deleteTask(Long id);

    void validateTask(Task task);


}

