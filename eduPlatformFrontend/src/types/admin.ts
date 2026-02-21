import type { Role, UserStatus } from './user';

// ===== Dashboard Stats =====

export interface UsersByRole {
  superAdmins: number;
  admins: number;
  moderators: number;
  teachers: number;
  parents: number;
  students: number;
}

export interface UsersByStatus {
  active: number;
  inactive: number;
  blocked: number;
  pendingVerification: number;
}

export interface QuestionsByDifficulty {
  easy: number;
  medium: number;
  hard: number;
}

export interface QuestionsByType {
  mcqSingle: number;
  mcqMulti: number;
  trueFalse: number;
  fillBlank: number;
  matching: number;
  ordering: number;
  shortAnswer: number;
  essay: number;
}

export interface NotificationStats {
  totalSent: number;
  totalFailed: number;
  totalPending: number;
  totalRetrying: number;
  bySms: number;
  byEmail: number;
}

export interface RecentActivity {
  id: string;
  userId: string;
  action: string;
  actionCategory: string;
  entityType: string;
  createdAt: string;
}

export interface DashboardStats {
  totalUsers: number;
  activeUsers: number;
  newUsersThisWeek: number;
  totalSubjects: number;
  totalTopics: number;
  totalQuestions: number;
  pendingQuestions: number;
  activeQuestions: number;
  totalTests: number;
  testsThisWeek: number;
  activeSessionsToday: number;
  totalDownloads: number;
  usersByRole: UsersByRole;
  usersByStatus: UsersByStatus;
  questionsByDifficulty: QuestionsByDifficulty;
  questionsByType: QuestionsByType;
  notificationStats: NotificationStats;
  recentActivity: RecentActivity[];
}

// ===== Trend Data =====

export interface TrendPoint {
  date: string;
  count: number;
}

export interface TrendData {
  weeklyRegistrations: TrendPoint[];
  weeklyTestCreations: TrendPoint[];
  dailyActiveUsers: TrendPoint[];
}

// ===== Content Stats =====

export interface SubjectStats {
  subjectId: string;
  subjectName: string;
  topicCount: number;
  questionCount: number;
  testCount: number;
}

export interface TopTeacher {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  questionCount: number;
  subjectCount: number;
}

export interface ContentStats {
  subjectStats: SubjectStats[];
  topTeachers: TopTeacher[];
}

// ===== System Info =====

export interface JvmInfo {
  maxMemoryMb: number;
  totalMemoryMb: number;
  freeMemoryMb: number;
  usedMemoryMb: number;
  availableProcessors: number;
  javaVersion: string;
}

export interface DatabaseInfo {
  activeConnections: number;
  idleConnections: number;
  totalConnections: number;
  maxPoolSize: number;
}

export interface SystemInfo {
  jvm: JvmInfo;
  database: DatabaseInfo;
  uptime: string;
  serverTime: string;
}

// ===== Admin User =====

export interface AdminUserDto {
  id: string;
  email: string | null;
  phone: string | null;
  emailVerified: boolean;
  phoneVerified: boolean;
  firstName: string;
  lastName: string;
  avatarUrl: string | null;
  role: Role;
  status: UserStatus;
  statusReason: string | null;
  failedLoginAttempts: number;
  lockedUntil: string | null;
  lastLoginAt: string | null;
  lastLoginIp: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChangeRoleRequest {
  role: Role;
}

export interface ChangeStatusRequest {
  status: UserStatus;
  reason?: string;
}

// ===== Audit Log =====

export interface AuditLogDto {
  id: string;
  userId: string;
  userRole: string;
  action: string;
  actionCategory: string;
  entityType: string;
  entityId: string;
  oldValues: Record<string, unknown> | null;
  newValues: Record<string, unknown> | null;
  ipAddress: string;
  userAgent: string;
  createdAt: string;
}