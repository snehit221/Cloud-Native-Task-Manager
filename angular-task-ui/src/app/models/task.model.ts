export interface Task {
  id?: number;
  title: string;
  description?: string;
  status: TaskStatus;
  dueDate: Date;
  createdAt?: string;
  updatedAt?: string;
}

export enum TaskStatus {
  TODO = 'TODO',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED'
}

export interface TaskPage {
  content: Task[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface TaskFilter {
  status?: TaskStatus;
  title?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface ApiResponse<T> {
  data?: T;
  message?: string;
  success: boolean;
}

export interface ValidationError {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  fieldErrors?: { [key: string]: string };
}