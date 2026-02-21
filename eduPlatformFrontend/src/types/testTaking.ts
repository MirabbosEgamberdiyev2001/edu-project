export enum AttemptStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  SUBMITTED = 'SUBMITTED',
  GRADED = 'GRADED',
  EXPIRED = 'EXPIRED',
}

export interface AttemptDto {
  id: string;
  assignmentId: string;
  studentId: string;
  testTitle: string;
  status: AttemptStatus;
  startedAt: string;
  submittedAt: string | null;
  score: number | null;
  maxScore: number;
  percentage: number | null;
  totalQuestions: number;
  answeredQuestions: number;
  tabSwitches: number;
  timeRemaining: number | null;
  durationMinutes: number | null;
  questions: AttemptQuestionDto[];
  answers: Record<string, AttemptAnswerDto>;
}

export interface AttemptQuestionDto {
  id: string;
  questionText: string;
  questionType: string;
  difficulty: string;
  points: number;
  timeLimitSeconds: number | null;
  media: Record<string, unknown> | null;
  options: unknown;
}

export interface AttemptAnswerDto {
  questionId: string;
  response: unknown;
  savedAt: string;
  isCorrect: boolean | null;
  score: number | null;
}

export interface SubmitAnswerRequest {
  questionId: string;
  response: unknown;
}

export interface BatchSaveAnswerRequest {
  answers: SubmitAnswerRequest[];
}

export interface BatchSaveAnswerResponse {
  saved: number;
  failed: number;
}

export interface StartAttemptRequest {
  deviceInfo?: string;
}

export interface GradeAnswerRequest {
  answerId: string;
  score: number;
  feedback?: string;
}

export interface AttemptResultDto {
  id: string;
  testTitle: string;
  score: number;
  maxScore: number;
  percentage: number;
  totalQuestions: number;
  correctAnswers: number;
  wrongAnswers: number;
  unanswered: number;
  tabSwitches: number;
  startedAt: string;
  submittedAt: string;
  durationSeconds: number;
  answers: DetailedAnswerDto[];
}

export interface DetailedAnswerDto {
  questionId: string;
  questionText: string;
  questionType: string;
  options: unknown;
  correctAnswer: unknown;
  studentAnswer: unknown;
  isCorrect: boolean;
  score: number;
  maxScore: number;
  proof: string | null;
}
