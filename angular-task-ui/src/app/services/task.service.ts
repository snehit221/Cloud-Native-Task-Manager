import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { Task, TaskPage, TaskFilter, TaskStatus } from '../models/task.model';
import { environment } from '../../environments/environment'; // Add this import

@Injectable({
  providedIn: 'root'
})
export class TaskService {
  private readonly apiUrl = `${environment.apiUrl}`;
  
  // Subject to notify components about task changes
  private taskUpdatedSubject = new BehaviorSubject<boolean>(false);
  public taskUpdated$ = this.taskUpdatedSubject.asObservable();

  constructor(private http: HttpClient) { }

  /**
   * Get all tasks with pagination and filtering
   */
  getTasks(filter: TaskFilter = {}): Observable<TaskPage> {
    let params = new HttpParams();
    
    if (filter.page !== undefined) {
      params = params.set('page', filter.page.toString());
    }
    if (filter.size !== undefined) {
      params = params.set('size', filter.size.toString());
    }
    if (filter.sortBy) {
      params = params.set('sortBy', filter.sortBy);
    }
    if (filter.sortDir) {
      params = params.set('sortDir', filter.sortDir);
    }
    if (filter.status) {
      params = params.set('status', filter.status);
    }
    if (filter.title) {
      params = params.set('title', filter.title);
    }

    return this.http.get<TaskPage>(this.apiUrl, { params })
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Get task by ID
   */
  getTaskById(id: number): Observable<Task> {
    return this.http.get<Task>(`${this.apiUrl}/${id}`)
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Create new task
   */
  createTask(task: Task): Observable<Task> {
    const payload = {
    ...task,
    dueDate: task.dueDate ? new Date(task.dueDate).toISOString().slice(0,19) : null
  };
    return this.http.post<Task>(this.apiUrl, payload)
      .pipe(
        tap(() => this.taskUpdatedSubject.next(true)),
        catchError(this.handleError)
      );
  }

  /**
   * Update existing task.
   * 
   * Payload structure:
   *   - All fields from the Task object.
   *   - dueDate is sent as an ISO string (YYYY-MM-DDTHH:mm:ss) or null.
   * 
   * Possible error responses:
   *   - 400: Validation errors (returns fieldErrors in response).
   *   - 404: Task not found.
   *   - 500: Server error.
   */
  updateTask(id: number, task: Task): Observable<Task> {
   const payload = {
    ...task,
    dueDate: task.dueDate ? new Date(task.dueDate).toISOString().slice(0,19) : null
  };
    return this.http.put<Task>(`${this.apiUrl}/${id}`, payload)
      .pipe(
        tap(() => this.taskUpdatedSubject.next(true)),
        catchError(this.handleError)
      );
  }

  /**
   * Delete task
   */
  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`)
      .pipe(
        tap(() => this.taskUpdatedSubject.next(true)),
        catchError(this.handleError)
      );
  }

  /**
   * Get tasks by status
   */
  getTasksByStatus(status: TaskStatus, page: number = 0, size: number = 10): Observable<TaskPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<TaskPage>(`${this.apiUrl}/status/${status}`, { params })
      .pipe(
        catchError(this.handleError)
      );
  }

  /**
   * Get available task statuses
   */
  getTaskStatuses(): TaskStatus[] {
    return Object.values(TaskStatus);
  }

  /**
   * Get status display name
   */
  getStatusDisplayName(status: TaskStatus): string {
    switch (status) {
      case TaskStatus.TODO:
        return 'To Do';
      case TaskStatus.IN_PROGRESS:
        return 'In Progress';
      case TaskStatus.COMPLETED:
        return 'Completed';
      default:
        return status;
    }
  }

  /**
   * Handle HTTP errors
   */
  private handleError(error: HttpErrorResponse) {
    console.error('API Error:', error);
    
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      console.error('Client Error:', error.error.message);
      return throwError(() => new Error('A client-side error occurred. Please try again.'));
    } else {
      // Server-side error
      console.error('Server Error:', error.status, error.error);
      
      if (error.status === 400 && error.error?.fieldErrors) {
        // Validation errors
        return throwError(() => error.error);
      } else if (error.status === 404) {
        return throwError(() => new Error('Task not found.'));
      } else if (error.status === 500) {
        return throwError(() => new Error('Server error. Please try again later.'));
      } else {
        return throwError(() => new Error(`Server returned code ${error.status}.`));
      }
    }
  }
}