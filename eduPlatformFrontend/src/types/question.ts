export enum QuestionType {
  MCQ_SINGLE = 'MCQ_SINGLE',
  MCQ_MULTI = 'MCQ_MULTI',
  TRUE_FALSE = 'TRUE_FALSE',
  SHORT_ANSWER = 'SHORT_ANSWER',
  ESSAY = 'ESSAY',
  FILL_BLANK = 'FILL_BLANK',
  MATCHING = 'MATCHING',
  ORDERING = 'ORDERING',
}

export enum Difficulty {
  EASY = 'EASY',
  MEDIUM = 'MEDIUM',
  HARD = 'HARD',
}

export enum QuestionStatus {
  DRAFT = 'DRAFT',
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  REJECTED = 'REJECTED',
  ARCHIVED = 'ARCHIVED',
  DEPRECATED = 'DEPRECATED',
}

export interface QuestionVersionDto {
  id: string;
  questionId: string;
  version: number;
  questionText: string;
  questionTextTranslations: Record<string, string> | null;
  options: unknown;
  correctAnswer: unknown;
  proof: string | null;
  proofTranslations: Record<string, string> | null;
  changedBy: string;
  changeReason: string | null;
  createdAt: string;
}

export interface QuestionDto {
  id: string;
  topicId: string;
  topicName: string;
  topicNameTranslations: Record<string, string> | null;
  subjectId: string;
  subjectName: string;
  subjectNameTranslations: Record<string, string> | null;
  userId: string;
  userName: string;
  questionText: string;
  questionTextTranslations: Record<string, string> | null;
  questionType: QuestionType;
  difficulty: Difficulty;
  points: number;
  timeLimitSeconds: number | null;
  media: Record<string, unknown> | null;
  options: unknown;
  correctAnswer: unknown;
  proof: string | null;
  proofTranslations: Record<string, string> | null;
  proofRequired: boolean;
  status: QuestionStatus;
  rejectionReason: string | null;
  moderatedBy: string | null;
  moderatedAt: string | null;
  timesUsed: number;
  correctRate: number | null;
  avgTimeSeconds: number | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
}

export interface CreateQuestionRequest {
  topicId: string;
  questionText: Record<string, string>;
  questionType: QuestionType;
  difficulty?: Difficulty;
  points?: number;
  timeLimitSeconds?: number;
  media?: Record<string, unknown>;
  options?: unknown;
  correctAnswer?: unknown;
  proof?: Record<string, string>;
}

export interface UpdateQuestionRequest {
  questionText?: Record<string, string>;
  questionType?: QuestionType;
  difficulty?: Difficulty;
  points?: number;
  timeLimitSeconds?: number;
  media?: Record<string, unknown>;
  options?: unknown;
  correctAnswer?: unknown;
  proof?: Record<string, string>;
  changeReason?: string;
}

export interface BulkModerationRequest {
  questionIds: string[];
  reason?: string;
}

export interface BulkModerationResponse {
  totalRequested: number;
  successCount: number;
  failedCount: number;
  failedIds: string[];
  errors: string[];
}

export interface QuestionListParams {
  subjectId?: string;
  topicId?: string;
  questionType?: QuestionType;
  difficulty?: Difficulty;
  status?: QuestionStatus;
  search?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: string;
}
