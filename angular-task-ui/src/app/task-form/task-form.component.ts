import { Component, OnInit, Inject, Optional } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { Task, TaskStatus, ValidationError } from '../models/task.model';
import { TaskService } from '../services/task.service';


@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './task-form.component.html',
  styleUrls: ['./task-form.component.scss']
})

export class TaskFormComponent implements OnInit {
  taskForm: FormGroup;
  isEditMode: boolean = false;
  taskStatuses = Object.values(TaskStatus);
  isSubmitting = false;
  validationErrors: { [key: string]: string } = {};

  constructor(
    private fb: FormBuilder,
    private taskService: TaskService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<TaskFormComponent>,
    @Optional() @Inject(MAT_DIALOG_DATA) public data: { task?: Task }
  ) {
    this.taskForm = this.createForm();
  }

  ngOnInit(): void {
    if (this.data?.task) {
      this.isEditMode = true;
      this.populateForm(this.data.task);
    } else {
      // Set default due date to tomorrow
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      this.taskForm.patchValue({
        dueDate: tomorrow.toISOString().slice(0, 16)
      });
    }
  }

  private createForm(): FormGroup {
    return this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(255)]],
      description: ['', [Validators.maxLength(1000)]],
      status: [TaskStatus.TODO, [Validators.required]],
      dueDate: ['', [Validators.required, this.futureDateValidator]]
    });
  }

  private populateForm(task: Task): void {
    this.taskForm.patchValue({
      title: task.title,
      description: task.description || '',
      status: task.status,
      dueDate: task.dueDate ? new Date(task.dueDate).toISOString().slice(0, 16) : ''
    });
  }

  private futureDateValidator(control: any) {
    if (!control.value) return null;
    
    const selectedDate = new Date(control.value);
    const now = new Date();
    
    // Allow dates from current time onwards
    if (selectedDate <= now) {
      return { pastDate: true };
    }
    return null;
  }

  getStatusDisplayName(status: TaskStatus): string {
    return this.taskService.getStatusDisplayName(status);
  }

  onSubmit(): void {
    if (this.taskForm.valid) {
      this.isSubmitting = true;
      this.validationErrors = {};

      const formValue = this.taskForm.value;
      const task: Task = {
        title: formValue.title.trim(),
        description: formValue.description?.trim() || '',
        status: formValue.status,
        dueDate: new Date(formValue.dueDate)
      };

      const operation = this.isEditMode
        ? this.taskService.updateTask(this.data.task!.id!, task)
        : this.taskService.createTask(task);

      operation.subscribe({
        next: (response) => {
          this.isSubmitting = false;
          const message = this.isEditMode ? 'Task updated successfully!' : 'Task created successfully!';
          this.snackBar.open(message, 'Close', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });
          this.dialogRef.close(response);
        },
        error: (error) => {
          this.isSubmitting = false;
          this.handleError(error);
        }
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private handleError(error: any): void {
    if (error.fieldErrors) {
      // Validation errors from server
      this.validationErrors = error.fieldErrors;
      this.snackBar.open('Please correct the validation errors', 'Close', {
        duration: 5000,
        panelClass: ['error-snackbar']
      });
    } else {
      // General error
      const errorMessage = error.message || 'An error occurred while saving the task';
      this.snackBar.open(errorMessage, 'Close', {
        duration: 5000,
        panelClass: ['error-snackbar']
      });
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.taskForm.controls).forEach(key => {
      const control = this.taskForm.get(key);
      control?.markAsTouched();
    });
  }

  getFieldError(fieldName: string): string {
    const control = this.taskForm.get(fieldName);
    
    // Server-side validation errors take precedence
    if (this.validationErrors[fieldName]) {
      return this.validationErrors[fieldName];
    }

    // Client-side validation errors
    if (control?.invalid && control?.touched) {
      if (control.errors?.['required']) {
        return `${this.getFieldDisplayName(fieldName)} is required`;
      }
      if (control.errors?.['maxlength']) {
        const maxLength = control.errors['maxlength'].requiredLength;
        return `${this.getFieldDisplayName(fieldName)} must not exceed ${maxLength} characters`;
      }
      if (control.errors?.['pastDate']) {
        return 'Due date must be in the future';
      }
    }

    return '';
  }

  private getFieldDisplayName(fieldName: string): string {
    const fieldNames: { [key: string]: string } = {
      title: 'Title',
      description: 'Description',
      status: 'Status',
      dueDate: 'Due Date'
    };
    return fieldNames[fieldName] || fieldName;
  }

  hasFieldError(fieldName: string): boolean {
    return this.getFieldError(fieldName) !== '';
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  getCurrentDateTime(): string {
    const now = new Date();
    return now.toISOString().slice(0, 16);
  }
}