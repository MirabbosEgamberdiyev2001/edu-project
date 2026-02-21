import { useQuery } from '@tanstack/react-query';
import { adminApi } from '@/api/adminApi';

export function useDashboardStats() {
  return useQuery({
    queryKey: ['admin', 'dashboard', 'stats'],
    queryFn: async () => {
      const { data } = await adminApi.getDashboardStats();
      return data.data;
    },
    staleTime: 60_000,
  });
}

export function useTrendData() {
  return useQuery({
    queryKey: ['admin', 'dashboard', 'trends'],
    queryFn: async () => {
      const { data } = await adminApi.getTrendData();
      return data.data;
    },
    staleTime: 60_000,
  });
}

export function useContentStats() {
  return useQuery({
    queryKey: ['admin', 'dashboard', 'content-stats'],
    queryFn: async () => {
      const { data } = await adminApi.getContentStats();
      return data.data;
    },
    staleTime: 60_000,
  });
}

export function useSystemInfo() {
  return useQuery({
    queryKey: ['admin', 'dashboard', 'system-info'],
    queryFn: async () => {
      const { data } = await adminApi.getSystemInfo();
      return data.data;
    },
    staleTime: 30_000,
  });
}
