import { useQuery } from '@tanstack/react-query';
import { adminApi } from '@/api/adminApi';

interface UseAuditLogsParams {
  page?: number;
  size?: number;
}

export function useAuditLogs(params: UseAuditLogsParams = {}) {
  return useQuery({
    queryKey: ['admin', 'audit-logs', params],
    queryFn: async () => {
      const { data } = await adminApi.getAuditLogs(params);
      return data.data;
    },
    staleTime: 15_000,
  });
}

export function useAuditLogsByCategory(category: string, params: UseAuditLogsParams = {}) {
  return useQuery({
    queryKey: ['admin', 'audit-logs', 'category', category, params],
    queryFn: async () => {
      const { data } = await adminApi.getAuditLogsByCategory(category, params);
      return data.data;
    },
    enabled: !!category,
    staleTime: 15_000,
  });
}

export function useAuditLogsByUser(userId: string, params: UseAuditLogsParams = {}) {
  return useQuery({
    queryKey: ['admin', 'audit-logs', 'user', userId, params],
    queryFn: async () => {
      const { data } = await adminApi.getAuditLogsByUser(userId, params);
      return data.data;
    },
    enabled: !!userId,
    staleTime: 15_000,
  });
}

export function useAuditLogsByDateRange(from: string, to: string, params: UseAuditLogsParams = {}) {
  return useQuery({
    queryKey: ['admin', 'audit-logs', 'date-range', from, to, params],
    queryFn: async () => {
      const { data } = await adminApi.getAuditLogsByDateRange(from, to, params);
      return data.data;
    },
    enabled: !!from && !!to,
    staleTime: 15_000,
  });
}
