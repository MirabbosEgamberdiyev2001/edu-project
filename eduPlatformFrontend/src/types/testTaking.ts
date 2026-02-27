export enum AttemptStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  SUBMITTED = 'SUBMITTED',
  AUTO_GRADED = 'AUTO_GRADED',
  NEEDS_REVIEW = 'NEEDS_REVIEW',
  GRADED = 'GRADED',
  EXPIRED = 'EXPIRED',
}

export interface AttemptDto {
  id: string;
  assignmentId: string;
  assignmentTitle: string | null;

  // Frontend-expected aliases (populated by backend alongside the originals)
  testTitle: string | null;        // = assignmentTitle
  tabSwitches: number;             // = tabSwitchCount
  score: number | null;            // = rawScore as double
  maxScore: number | null;         // = maxScore
  timeRemaining: number | null;    // = remainingSeconds
  durationMinutes: number | null;

  studentId: string;
  status: AttemptStatus;
  startedAt: string;
  submittedAt: string | null;
  percentage: number | null;
  totalQuestions: number;
  answeredQuestions: number;

  // Exam questions — populated for IN_PROGRESS attempts
  questions: AttemptQuestionDto[];

  // Answers map (keyed by questionId) — populated for completed attempts
  answers: Record<string, AttemptAnswerDto> | null;
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
  optionsOrder: string[] | null;
  // Populated for submitted/graded attempts when assignment allows it
  correctAnswer: unknown | null;
  proof: string | null;
}

export interface AttemptAnswerDto {
  id: string;
  questionId: string;
  selectedAnswer: unknown;
  response: unknown;          // alias for selectedAnswer
  isCorrect: boolean | null;
  isPartial: boolean | null;
  earnedPoints: number | null;
  score: number | null;       // alias for earnedPoints
  maxPoints: number | null;
  bookmarked: boolean | null;
  timeSpentSeconds: number | null;
}

export interface SubmitAnswerRequest {
  questionId: string;
  questionIndex?: number;
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
