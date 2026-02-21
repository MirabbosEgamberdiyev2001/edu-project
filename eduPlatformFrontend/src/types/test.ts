export type TestStatus = 'CREATED' | 'GENERATING' | 'READY' | 'DOWNLOADED' | 'DELETED';

export type ExportFormat = 'PDF' | 'DOCX';

export interface DifficultyDistribution {
  easy: number;
  medium: number;
  hard: number;
}

export interface HeaderConfig {
  schoolName?: string;
  className?: string;
  teacherName?: string;
  logoUrl?: string;
  date?: string;
}

export type TestGenerationMode = 'auto' | 'manual';

export interface GenerateTestRequest {
  title: string;
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
  status: TestStatus;
  createdAt: string;
}

export interface AvailableQuestionsResponse {
  totalAvailable: number;
  easyCount: number;
  mediumCount: number;
  hardCount: number;
  maxPossibleQuestions: number;
}
