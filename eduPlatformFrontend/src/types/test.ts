export type TestStatus = 'CREATED' | 'GENERATING' | 'READY' | 'DOWNLOADED' | 'DELETED';

export type GlobalStatus = 'NONE' | 'PENDING_MODERATION' | 'APPROVED' | 'REJECTED';

export type ExportFormat = 'PDF' | 'DOCX';

export enum TestCategory {
  DTM = 'DTM',
  SCHOOL = 'SCHOOL',
  OLYMPIAD = 'OLYMPIAD',
  CERTIFICATE = 'CERTIFICATE',
  ATTESTATSIYA = 'ATTESTATSIYA',
}

export interface DifficultyDistribution {
  easy: number;
  medium: number;
  hard: number;
}

export interface HeaderConfig {
  schoolName?: string;
  schoolNameTranslations?: Record<string, string>;
  className?: string;
  teacherName?: string;
  teacherNameTranslations?: Record<string, string>;
  logoUrl?: string;
  date?: string;
}

export type TestGenerationMode = 'auto' | 'manual';

export interface GenerateTestRequest {
  title: string;
  titleTranslations?: Record<string, string>;
  category?: TestCategory;
  subjectId: string;
  topicIds: string[];
  questionCount?: number;
  questionIds?: string[];
  variantCount?: number;
  difficultyDistribution?: DifficultyDistribution;
  shuffleQuestions?: boolean;
  shuffleOptions?: boolean;
  randomSeed?: number;
  headerConfig?: HeaderConfig;
}

export interface VariantDto {
  code: string;
  questionIds: string[];
  answerKey: Record<string, unknown>[];
  optionsOrder: string[][];
}

export interface GenerateTestResponse {
  testId: string;
  title: string;
  titleTranslations?: Record<string, string>;
  questionCount: number;
  variantCount: number;
  difficultyDistribution: Record<string, number>;
  randomSeed: number;
  variants: VariantDto[];
  createdAt: string;
}

export interface TestHistoryDto {
  id: string;
  userId: string;
  title: string;
  titleTranslations?: Record<string, string>;
  category: TestCategory | null;
  subjectId: string;
  subjectName: string;
  topicIds: string[];
  questionCount: number;
  variantCount: number;
  difficultyDistribution: Record<string, number>;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  randomSeed: number;
  headerConfig: Record<string, unknown>;
  variants: VariantDto[];
  testPdfUrl: string | null;
  answerKeyPdfUrl: string | null;
  combinedPdfUrl: string | null;
  proofsPdfUrl: string | null;
  downloadCount: number;
  lastDownloadedAt: string | null;
  isPublic: boolean;
  publicSlug: string | null;
  publicDurationMinutes: number | null;
  status: TestStatus;
  createdAt: string;
  globalStatus: GlobalStatus;
  globalRejectionReason: string | null;
  globalSubmittedAt: string | null;
  globalReviewedAt: string | null;
  gradeLevel: number | null;
  teacherName?: string;
}

export interface GlobalTestStartResponse {
  attemptId: string;
  assignmentId: string;
  testTitle: string;
  durationMinutes: number;
  questionCount: number;
}

export interface AvailableQuestionsResponse {
  totalAvailable: number;
  easyCount: number;
  mediumCount: number;
  hardCount: number;
  maxPossibleQuestions: number;
}
