import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type { PagedResponse } from '@/types/subject';
import type {
  DashboardStats,
  TrendData,
  ContentStats,
  SystemInfo,
  AdminUserDto,
  AuditLogDto,
  ChangeRoleRequest,
  ChangeStatusRequest,
} from '@/types/admin';
import type { Role, UserStatus } from '@/types/user';

const ADMIN = '/admin';

export const adminApi = {
  // Dashboard
  getDashboardStats: () =>
    api.get<ApiResponse<DashboardStats>>(`${ADMIN}/dashboard/stats`),

  getTrendData: () =>
    api.get<ApiResponse<TrendData>>(`${ADMIN}/dashboard/trends`),

  getContentStats: () =>
    api.get<ApiResponse<ContentStats>>(`${ADMIN}/dashboard/content-stats`),

  getSystemInfo: () =>
    api.get<ApiResponse<SystemInfo>>(`${ADMIN}/dashboard/system-info`),

  // User Management
  getUsers: (params?: {
    search?: string;
    role?: Role;
    status?: UserStatus;
    page?: number;
    size?: number;
  }) =>
    api.get<ApiResponse<PagedResponse<AdminUserDto>>>(`${ADMIN}/users`, { params }),

  getUserById: (id: string) =>
    api.get<ApiResponse<AdminUserDto>>(`${ADMIN}/users/${id}`),

  changeRole: (id: string, data: ChangeRoleRequest) =>
    api.put<ApiResponse<AdminUserDto>>(`${ADMIN}/users/${id}/role`, data),

  changeStatus: (id: string, data: ChangeStatusRequest) =>
    api.put<ApiResponse<AdminUserDto>>(`${ADMIN}/users/${id}/status`, data),

  unlockUser: (id: string) =>
    api.post<ApiResponse<AdminUserDto>>(`${ADMIN}/users/${id}/unlock`),

  deleteUser: (id: string) =>
    api.delete<ApiResponse<void>>(`${ADMIN}/users/${id}`),

  // Audit Logs
  getAuditLogs: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<AuditLogDto>>>(`${ADMIN}/audit-logs`, { params }),

  getAuditLogsByUser: (userId: string, params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<AuditLogDto>>>(`${ADMIN}/audit-logs/user/${userId}`, { params }),

  getAuditLogsByCategory: (category: string, params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<AuditLogDto>>>(`${ADMIN}/audit-logs/category/${category}`, { params }),

  getAuditLogsByDateRange: (from: string, to: string, params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<AuditLogDto>>>(`${ADMIN}/audit-logs/date-range`, {
      params: { from, to, ...params },
    }),

  getAuditLogsByEntity: (entityType: string, entityId: string, params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<AuditLogDto>>>(`${ADMIN}/audit-logs/entity/${entityType}/${entityId}`, { params }),
};