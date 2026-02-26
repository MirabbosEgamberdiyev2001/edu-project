// ── Teacher Dashboard ──────────────────────────────

export interface TeacherDashboardDto {
  totalStudents: number;
  totalGroups: number;
  totalAssignments: number;
  activeAssignments: number;
  totalTests: number;
  averageScore: number;
  completionRate: number;
  testCreationTrend: TrendPointDto[];
  recentActivity: TeacherActivityDto[];
  topStudents: TopStudentDto[];
  atRiskStudents: AtRiskStudentDto[];
}

export interface TrendPointDto {
  date: string;   // e.g. "2024-01" or ISO date
  value: number;
}

export interface TopStudentDto {
  studentId: string;
  firstName: string;
  lastName: string;
  averageScore: number;
  totalAttempts: number;
  completionRate: number;
}

export interface AtRiskStudentDto {
  studentId: string;
  firstName: string;
  lastName: string;
  averageScore: number;
  missedAssignments: number;
  lastActivityAt: string | null;
}

export interface TeacherActivityDto {
  type: string;
  description: string;
  createdAt: string;
}

// ── Student Analytics ──────────────────────────────

export interface StudentAnalyticsDto {
  studentId: string;
  firstName: string;
  lastName: string;
  overallAverage: number;
  totalAttempts: number;
  totalAssignments: number;
  completionRate: number;
  scoreTrend: TrendPointDto[];
  subjectBreakdown: SubjectBreakdownDto[];
  weakAreas: WeakAreaDto[];
  strongAreas: WeakAreaDto[];
  weeklyActivity: WeeklyActivityDto[];
}

export interface SubjectBreakdownDto {
  subjectId: string;
  subjectName: string;
  averageScore: number;
  totalAttempts: number;
}

export interface WeakAreaDto {
  topicId: string;
  topicName: string;
  subjectName: string;
  averageScore: number;
  attemptCount: number;
}

export type StrongAreaDto = WeakAreaDto;

export interface WeeklyActivityDto {
  date: string;
  attemptCount: number;
}

// ── Group Statistics ──────────────────────────────

export interface GroupStatisticsDto {
  groupId: string;
  groupName: string;
  memberCount: number;
  averageScore: number;
  completionRate: number;
  totalAssignments: number;
  completedAssignments: number;
  scoreDistribution: ScoreDistributionDto[];
  studentRankings: StudentRankingDto[];
  subjectPerformance: SubjectPerformanceDto[];
}

export interface ScoreDistributionDto {
  range: string;
  count: number;
}

export interface StudentRankingDto {
  rank: number;
  studentId: string;
  firstName: string;
  lastName: string;
  averageScore: number;
  attemptCount: number;
}

export interface SubjectPerformanceDto {
  subjectId: string;
  subjectName: string;
  averageScore: number;
  assignmentCount: number;
}
