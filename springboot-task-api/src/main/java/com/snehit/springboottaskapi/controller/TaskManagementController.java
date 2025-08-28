package com.snehit.springboottaskapi.controller;

import com.snehit.springboottaskapi.entity.Task;
import com.snehit.springboottaskapi.enums.TaskStatus;
import com.snehit.springboottaskapi.service.ITaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
@CrossOrigin(origins = "*")
public class TaskManagementController {

  private final ITaskService taskService;

  @Autowired
  public TaskManagementController(ITaskService taskService) {
    this.taskService = taskService;
  }

  @PostMapping
  @Operation(summary = "Create a new task")
  public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
    Task createdTask = taskService.createTask(task);
    return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
  }

  @GetMapping
  @Operation(summary = "Get all tasks with pagination and filtering")
  public ResponseEntity<Page<Task>> getAllTasks(
      @Parameter(description = "Filter by task status") @RequestParam(required = false)
          TaskStatus status,
      @Parameter(description = "Filter by title containing text") @RequestParam(required = false)
          String title,
      @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
      @Parameter(description = "Sort by field") @RequestParam(defaultValue = "createdAt")
          String sortBy,
      @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc")
          String sortDir) {

    Sort.Direction direction =
        sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
    Page<Task> tasks = taskService.getAllTasks(status, title, pageable);

    return ResponseEntity.ok(tasks);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get task by ID")
  public ResponseEntity<Task> getTaskById(
      @Parameter(description = "Task ID") @PathVariable Long id) {
    Task task = taskService.getTaskById(id);
    return ResponseEntity.ok(task);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update an existing task")
  public ResponseEntity<Task> updateTask(
      @Parameter(description = "Task ID") @PathVariable Long id,
      @Valid @RequestBody Task taskDetails) {
    Task updatedTask = taskService.updateTask(id, taskDetails);
    return ResponseEntity.ok(updatedTask);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a task")
  public ResponseEntity<Void> deleteTask(
      @Parameter(description = "Task ID") @PathVariable Long id) {
    taskService.deleteTask(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/status/{status}")
  @Operation(summary = "Get tasks by status")
  public ResponseEntity<Page<Task>> getTasksByStatus(
      @Parameter(description = "Task status") @PathVariable TaskStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<Task> tasks = taskService.getAllTasks(status, null, pageable);

    return ResponseEntity.ok(tasks);
  }
}
