export enum AssignmentStatus {
  DRAFT = 'DRAFT',
  SCHEDULED = 'SCHEDULED',
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

export interface AssignmentDto {
  id: string;
  teacherId: string;
  groupId: string;
  groupName: string;
  testHistoryId: string;
  testTitle: string;
  title: string;
  titleTranslations: Record<string, string> | null;
  description: string | null;
  descriptionTranslations: Record<string, string> | null;
  status: AssignmentStatus;
  startDate: string | null;
  endDate: string | null;
  durationMinutes: number | null;
  maxAttempts: number;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  showResults: boolean;
  preventTabSwitch: boolean;
  preventCopyPaste: boolean;
  totalStudents: number;
  completedStudents: number;
  activeStudents: number;
  averageScore: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAssignmentRequest {
  groupId?: string;
  testHistoryId: string;
  title?: string;
  titleTranslations?: Record<string, string>;
  description?: string;
  descriptionTranslations?: Record<string, string>;
  startDate?: string;
  endDate?: string;
  durationMinutes?: number;
  maxAttempts?: number;
  shuffleQuestions?: boolean;
  shuffleOptions?: boolean;
  showResults?: boolean;
  preventTabSwitch?: boolean;
  preventCopyPaste?: boolean;
}

export interface UpdateAssignmentRequest {
  title?: string;
  titleTranslations?: Record<string, string>;
  description?: string;
  descriptionTranslations?: Record<string, string>;
  startDate?: string;
  endDate?: string;
  durationMinutes?: number;
  maxAttempts?: number;
  shuffleQuestions?: boolean;
  shuffleOptions?: boolean;
  showResults?: boolean;
  preventTabSwitch?: boolean;
  preventCopyPaste?: boolean;
}

export interface AssignmentResultDto {
  assignmentId: string;
  groupId: string;
  groupName: string;
  totalStudents: number;
  completedStudents: number;
  averageScore: number;
  highestScore: number;
  lowestScore: number;
  students: StudentResultDto[];
}

export interface StudentResultDto {
  studentId: string;
  firstName: string;
  lastName: string;
  score: number | null;
  maxScore: number;
  percentage: number | null;
  submittedAt: string | null;
  attemptCount: number;
  tabSwitches: number;
  status: string;
}

export interface LiveMonitoringDto {
  assignmentId: string;
  totalStudents: number;
  activeStudents: number;
  completedStudents: number;
  notStartedStudents: number;
  students: LiveStudentDto[];
}

export interface LiveStudentDto {
  studentId: string;
  firstName: string;
  lastName: string;
  status: string;
  currentQuestion: number | null;
  totalQuestions: number;
  answeredQuestions: number;
  tabSwitches: number;
  startedAt: string | null;
  timeRemaining: number | null;
}

export interface PromoCodeDto {
  id: string;
  assignmentId: string;
  code: string;
  maxUses: number | null;
  currentUses: number;
  expiresAt: string | null;
  isActive: boolean;
  createdAt: string;
}

export interface GeneratePromoCodeRequest {
  maxUses?: number;
  expiresAt?: string;
}

export interface RedeemPromoCodeRequest {
  code: string;
}
