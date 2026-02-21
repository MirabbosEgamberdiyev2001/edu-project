export enum SubjectCategory {
  DTM = 'DTM',
  SCHOOL = 'SCHOOL',
  OLYMPIAD = 'OLYMPIAD',
  CERTIFICATE = 'CERTIFICATE',
  ATTESTATSIYA = 'ATTESTATSIYA',
}

export interface SubjectDto {
  id: string;
  userId: string;
  name: string;
  description: string | null;
  nameTranslations: Record<string, string> | null;
  descriptionTranslations: Record<string, string> | null;
  icon: string | null;
  color: string | null;
  category: SubjectCategory | null;
  gradeLevel: number | null;
  isTemplate: boolean;
  templateId: string | null;
  isActive: boolean;
  isArchived: boolean;
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
  category?: SubjectCategory;
  gradeLevel?: number;
}

export interface UpdateSubjectRequest {
  name?: Record<string, string>;
  description?: Record<string, string>;
  icon?: string;
  color?: string;
  category?: SubjectCategory;
  gradeLevel?: number;
  isActive?: boolean;
}

// PagedResponse moved to @/types/api.ts
export type { PagedResponse } from './api';
