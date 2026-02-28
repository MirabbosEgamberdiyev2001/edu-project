export enum PairingStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  REVOKED = 'REVOKED',
  EXPIRED = 'EXPIRED',
}

export interface ParentChildDto {
  id: string;
  parentId: string;
  parentName: string;
  childId: string;
  childName: string;
  childEmail: string | null;
  status: PairingStatus;
  pairedAt: string | null;
  revokedAt: string | null;
}

export interface GeneratePairingCodeResponse {
  code: string;
  expiresIn: number;
  qrCodeDataUri: string;
}

export interface PairWithCodeRequest {
  code: string;
}

// Matches backend ChildDashboardDto.RecentAttemptDto
export interface RecentAttemptDto {
  attemptId: string;
  assignmentTitle: string;
  percentage: number | null;
  status: string;
  submittedAt: string | null;
}

// Matches backend ChildDashboardDto.SubjectScoreDto
export interface SubjectScoreDto {
  subjectName: string;
  averageScore: number;
  attemptCount: number;
  level: 'EXCELLENT' | 'GOOD' | 'ATTENTION' | 'CRITICAL';
}

// Matches backend ChildDashboardDto.WeeklyActivityDto
export interface WeeklyActivityDto {
  testsCompletedToday: number;
  testsCompletedThisWeek: number;
  testsCompletedThisMonth: number;
  averageScoreThisWeek: number;
  totalTimeSpentMinutesToday: number;
}

// Matches backend ChildDashboardDto
export interface ChildDashboardDto {
  childId: string;
  childName: string;
  totalAssignments: number;
  completedAssignments: number;
  pendingAssignments: number;
  averageScore: number;
  scoreTrend: 'UP' | 'DOWN' | 'STABLE';
  recentAttempts: RecentAttemptDto[];
  subjectBreakdown: SubjectScoreDto[];
  weeklyActivity: WeeklyActivityDto;
}
