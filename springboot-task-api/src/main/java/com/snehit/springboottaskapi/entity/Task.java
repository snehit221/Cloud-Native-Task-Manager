package com.snehit.springboottaskapi.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.snehit.springboottaskapi.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.*;

/**
 * @Entity class Task that defines the data structure object of a task
 * Using Lombok to generate boilerplate code for getters, setters, toString() and constructor methods
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "tasks")
@Schema(description = "Task entity representing a task in the system")
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(description = "Unique identifier of the task", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
  private Long id;

  @NotBlank(message = "Title is required")
  @Size(max = 255, message = "Title must not exceed 255 characters")
  @Column(nullable = false)
  @Schema(description = "Title of the task", example = "Study Kubernetes Topic and Deploy to EKS", required = true)
  private String title;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  @Schema(description = "Detailed description of the task", example = "This task involves architecting cloud-native fault tolerant distributed application")
  private String description;

  @NotNull(message = "Status is required")
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Schema(description = "Current status of the task", example = "TODO", required = true)
  private TaskStatus status;

  @NotNull(message = "Due date is required")
  @Future(message = "Due date must be in the future")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  @Column(name = "due_date", nullable = false)
  @Schema(description = "Due date and time for the task", example = "2025-12-31T23:59:59", required = true)
  private LocalDateTime dueDate;

  @Column(name = "created_at", updatable = false)
  @Schema(description = "Timestamp when the task was created", example = "2025-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  @Schema(description = "Timestamp when the task was last updated", example = "2025-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
