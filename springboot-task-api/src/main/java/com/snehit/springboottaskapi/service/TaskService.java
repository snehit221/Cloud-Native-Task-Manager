package com.snehit.springboottaskapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snehit.springboottaskapi.entity.Task;
import com.snehit.springboottaskapi.enums.TaskStatus;
import com.snehit.springboottaskapi.exception.TaskNotFoundException;
import com.snehit.springboottaskapi.repository.ITaskRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

@Service
@Transactional
public class TaskService implements ITaskService {

  private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

  private final ITaskRepository taskRepository;

  private final ObjectMapper objectMapper;

  private EventBridgeClient eventBridgeClient;

  @Autowired
  public TaskService(
      ITaskRepository taskRepository,
      EventBridgeClient eventBridgeClient,
      ObjectMapper objectMapper) {
    this.taskRepository = taskRepository;
    this.eventBridgeClient = eventBridgeClient;
    this.objectMapper = objectMapper;
  }

  /** Create a new task */
  @Override
  public Task createTask(Task task) {
    validateTask(task);
    Task updatedTask = taskRepository.save(task);
    if (updatedTask.getStatus() == TaskStatus.COMPLETED) {
      sendCompletedTaskToEventBridge(updatedTask);
    }
    return updatedTask;
  }

  /** Get all tasks with pagination and filtering */
  @Override
  @Transactional(readOnly = true)
  public Page<Task> getAllTasks(TaskStatus status, String title, Pageable pageable) {
    return taskRepository.findTasksWithFilters(status, title, pageable);
  }

  /** Get task by ID */
  @Override
  @Transactional(readOnly = true)
  public Task getTaskById(Long id) {
    return taskRepository
        .findById(id)
        .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));
  }

  /**
   * Update an existing task
   *
   * @implNote send to EventBridge rule if the status is COMPLETED
   */
  @Override
  public Task updateTask(Long id, Task taskDetails) {
    Task existingTask = getTaskById(id);

    // Update fields
    logger.info("Updating task with id: {}", id);

    existingTask.setTitle(taskDetails.getTitle());
    existingTask.setDescription(taskDetails.getDescription());
    existingTask.setStatus(taskDetails.getStatus());
    existingTask.setDueDate(taskDetails.getDueDate());

    validateTask(existingTask);
    Task updatedTask = taskRepository.save(existingTask);

    // checking status and sending to EventBridge
    if (updatedTask.getStatus() == TaskStatus.COMPLETED) {
      sendCompletedTaskToEventBridge(updatedTask);
    }
    return updatedTask;
  }

  /** Send TaskCompleted event to Amazon EventBridge */
  private void sendCompletedTaskToEventBridge(Task task) {
    try {
      // Build the detail map
      Map<String, Object> detailMap = new HashMap<>();
      detailMap.put("id", task.getId());
      detailMap.put("title", task.getTitle());
      detailMap.put("description", task.getDescription());
      detailMap.put("status", task.getStatus().name());
      detailMap.put("dueDate", task.getDueDate());
      detailMap.put("createdAt", task.getCreatedAt());
      detailMap.put("updatedAt", task.getUpdatedAt());

      // Convert to JSON string
      String detailJson = objectMapper.writeValueAsString(detailMap);

      // Build the request entry
      PutEventsRequestEntry requestEntry =
          PutEventsRequestEntry.builder()
              .source("task.manager")
              .detailType("Task Completed")
              .detail(detailJson)
              .time(Instant.now())
              .build();

      // Send the event to EventBridge
      PutEventsResponse response =
          eventBridgeClient.putEvents(PutEventsRequest.builder().entries(requestEntry).build());

      logger.info("Event sent to EventBridge for task {}: {}", task.getId(), response);
    } catch (Exception e) {
      logger.error("Failed to send event to EventBridge for task {}", task.getId(), e);
    }
  }

  /** Delete a task */
  @Override
  public void deleteTask(Long id) {
    Task task = getTaskById(id);
    taskRepository.delete(task);
  }

  /** Validate task business rules */
  @Override
  public void validateTask(Task task) {
    logger.info("Going to validate the task: {}", task.getTitle());
    if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("Due date must be in the future");
    }

    if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
      throw new IllegalArgumentException("Task title is required");
    }
  }
}
