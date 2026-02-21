import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type {
  TeacherDashboardDto,
  StudentAnalyticsDto,
  GroupStatisticsDto,
} from '@/types/analytics';

const ANALYTICS = '/analytics';

export const analyticsApi = {
  getTeacherDashboard: () =>
    api.get<ApiResponse<TeacherDashboardDto>>(`${ANALYTICS}/teacher/dashboard`),

  exportTeacherDashboard: () =>
    api.get(`${ANALYTICS}/teacher/dashboard/export`, { responseType: 'blob' }),

  getMyAnalytics: () =>
    api.get<ApiResponse<StudentAnalyticsDto>>(`${ANALYTICS}/student/me`),

  getStudentAnalytics: (studentId: string) =>
    api.get<ApiResponse<StudentAnalyticsDto>>(`${ANALYTICS}/student/${studentId}`),

  getGroupStatistics: (groupId: string) =>
    api.get<ApiResponse<GroupStatisticsDto>>(`${ANALYTICS}/group/${groupId}`),

  exportGroupStatistics: (groupId: string) =>
    api.get(`${ANALYTICS}/group/${groupId}/export`, { responseType: 'blob' }),
};
