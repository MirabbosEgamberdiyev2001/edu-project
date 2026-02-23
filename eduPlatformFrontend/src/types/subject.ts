export interface SubjectDto {
  id: string;
  userId: string;
  name: string;
  description: string | null;
  nameTranslations: Record<string, string> | null;
  descriptionTranslations: Record<string, string> | null;
  icon: string | null;
  color: string | null;
  isTemplate: boolean;
  templateId: string | null;
  isActive: boolean;
  isArchived: boolean;
  gradeLevel: number | null;
  topicCount: number;
  questionCount: number;
  testCount: number;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSubjectRequest {
  name: Record<string, string>;
  description?: Record<string, string>;
  icon?: string;
  color?: string;
  gradeLevel?: number;
}

export interface UpdateSubjectRequest {
  name?: Record<string, string>;
  description?: Record<string, string>;
  icon?: string;
  color?: string;
  isActive?: boolean;
  gradeLevel?: number;
}

// PagedResponse moved to @/types/api.ts
export type { PagedResponse } from './api';
