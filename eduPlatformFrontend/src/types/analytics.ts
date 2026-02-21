export interface TeacherDashboardDto {
  totalStudents: number;
  totalGroups: number;
  totalAssignments: number;
  totalTests: number;
  averageScore: number;
  completionRate: number;
  testCreationTrend: TrendPointDto[];
  topStudents: TopStudentDto[];
  atRiskStudents: AtRiskStudentDto[];
  recentActivity: TeacherActivityDto[];
}

export interface TrendPointDto {
  date: string;
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
  strongAreas: StrongAreaDto[];
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

export interface StrongAreaDto {
  topicId: string;
  topicName: string;
  subjectName: string;
  averageScore: number;
  attemptCount: number;
}

export interface WeeklyActivityDto {
  date: string;
  attemptCount: number;
}

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
