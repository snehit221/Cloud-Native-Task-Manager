package com.snehit.springboottaskapi.service;


import com.snehit.springboottaskapi.entity.Task;
import com.snehit.springboottaskapi.enums.TaskStatus;
import com.snehit.springboottaskapi.exception.TaskNotFoundException;
import com.snehit.springboottaskapi.repository.ITaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

        @Mock
        private ITaskRepository taskRepository;

        @InjectMocks
        private TaskService taskService;

        private Task sampleTask;

        @BeforeEach
        void setUp() {
            sampleTask = new Task();
            sampleTask.setId(1L);
            sampleTask.setTitle("Sample Task");
            sampleTask.setDescription("Sample Description");
            sampleTask.setStatus(TaskStatus.TODO);
            sampleTask.setDueDate(LocalDateTime.now().plusDays(7));
            sampleTask.setCreatedAt(LocalDateTime.now());
            sampleTask.setUpdatedAt(LocalDateTime.now());
        }

        @Test
        void testCreateTask_Success() {
            // Given
            Task newTask = new Task();
            newTask.setTitle("New Task");
            newTask.setDescription("New Description");
            newTask.setStatus(TaskStatus.TODO);
            newTask.setDueDate(LocalDateTime.now().plusDays(5));

            when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

            // When
            Task createdTask = taskService.createTask(newTask);

            // Then
            assertNotNull(createdTask);
            assertEquals(sampleTask.getTitle(), createdTask.getTitle());
            verify(taskRepository, times(1)).save(any(Task.class));
        }

        @Test
        void testCreateTask_WithPastDueDate_ThrowsException() {
            // Given
            Task newTask = new Task();
            newTask.setTitle("New Task");
            newTask.setStatus(TaskStatus.TODO);
            newTask.setDueDate(LocalDateTime.now().minusDays(1)); // Past date

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> taskService.createTask(newTask)
            );
            assertEquals("Due date must be in the future", exception.getMessage());
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        void testCreateTask_WithEmptyTitle_ThrowsException() {
            // Given
            Task newTask = new Task();
            newTask.setTitle(""); // Empty title
            newTask.setStatus(TaskStatus.TODO);
            newTask.setDueDate(LocalDateTime.now().plusDays(5));

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> taskService.createTask(newTask)
            );
            assertEquals("Task title is required", exception.getMessage());
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        void testGetTaskById_Success() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

            // When
            Task foundTask = taskService.getTaskById(1L);

            // Then
            assertNotNull(foundTask);
            assertEquals(sampleTask.getId(), foundTask.getId());
            assertEquals(sampleTask.getTitle(), foundTask.getTitle());
            verify(taskRepository, times(1)).findById(1L);
        }

        @Test
        void testGetTaskById_NotFound_ThrowsException() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            TaskNotFoundException exception = assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.getTaskById(1L)
            );
            assertEquals("Task not found with id: 1", exception.getMessage());
            verify(taskRepository, times(1)).findById(1L);
        }

        @Test
        void testGetAllTasks_Success() {
            // Given

            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> taskPage = new PageImpl<>(Arrays.asList(sampleTask));
            when(taskRepository.findTasksWithFilters(any(), any(), any(Pageable.class)))
                    .thenReturn(taskPage);

            // When
            Page<Task> result = taskService.getAllTasks(null, null, pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(sampleTask.getTitle(), result.getContent().get(0).getTitle());
            verify(taskRepository, times(1)).findTasksWithFilters(any(), any(), any(Pageable.class));
        }

        @Test
        void testUpdateTask_Success() {
            // Given
            Task updateDetails = new Task();
            updateDetails.setTitle("Updated Task");
            updateDetails.setDescription("Updated Description");
            updateDetails.setStatus(TaskStatus.IN_PROGRESS);
            updateDetails.setDueDate(LocalDateTime.now().plusDays(10));

            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

            // When
            Task updatedTask = taskService.updateTask(1L, updateDetails);

            // Then
            assertNotNull(updatedTask);
            verify(taskRepository, times(1)).findById(1L);
            verify(taskRepository, times(1)).save(any(Task.class));
        }

        @Test
        void testUpdateTask_NotFound_ThrowsException() {
            // Given
            Task updateDetails = new Task();
            updateDetails.setTitle("Updated Task");

            when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            TaskNotFoundException exception = assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.updateTask(1L, updateDetails)
            );
            assertEquals("Task not found with id: 1", exception.getMessage());
            verify(taskRepository, times(1)).findById(1L);
            verify(taskRepository, never()).save(any(Task.class));
        }

        @Test
        void testDeleteTask_Success() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

            // When
            taskService.deleteTask(1L);

            // Then
            verify(taskRepository, times(1)).findById(1L);
            verify(taskRepository, times(1)).delete(sampleTask);
        }

        @Test
        void testDeleteTask_NotFound_ThrowsException() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            TaskNotFoundException exception = assertThrows(
                    TaskNotFoundException.class,
                    () -> taskService.deleteTask(1L)
            );
            assertEquals("Task not found with id: 1", exception.getMessage());
            verify(taskRepository, times(1)).findById(1L);
            verify(taskRepository, never()).delete(any(Task.class));
        }
    }

