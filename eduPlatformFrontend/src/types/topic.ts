export interface TopicDto {
  id: string;
  subjectId: string;
  parentId: string | null;
  userId: string;
  gradeLevel: number;
  name: string;
  description: string | null;
  nameTranslations: Record<string, string> | null;
  descriptionTranslations: Record<string, string> | null;
  level: number;
  path: string;
  isActive: boolean;
  questionCount: number;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface TopicTreeDto {
  id: string;
  subjectId: string;
  parentId: string | null;
  userId: string;
  gradeLevel: number;
  name: string;
  description: string | null;
  nameTranslations: Record<string, string> | null;
  descriptionTranslations: Record<string, string> | null;
  level: number;
  questionCount: number;
  sortOrder: number;
  isActive: boolean;
  children: TopicTreeDto[];
}

export interface CreateTopicRequest {
  name: Record<string, string>;
  description?: Record<string, string>;
  parentId?: string;
  gradeLevel: number;
}

export interface UpdateTopicRequest {
  name?: Record<string, string>;
  description?: Record<string, string>;
  isActive?: boolean;
}

export interface MoveTopicRequest {
  newParentId?: string | null;
  newSubjectId?: string;
}

export interface ReorderTopicsRequest {
  items: { id: string; sortOrder: number }[];
}
