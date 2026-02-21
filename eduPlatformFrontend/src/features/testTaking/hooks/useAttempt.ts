import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { testTakingApi } from '@/api/testTakingApi';

export function useAttempt(attemptId: string, enabled = true) {
  return useQuery({
    queryKey: ['test-taking', 'attempt', attemptId],
    queryFn: async () => {
      const { data } = await testTakingApi.getAttempt(attemptId);
      return data.data;
    },
    enabled: !!attemptId && enabled,
  });
}

export function useMyAttempts(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['test-taking', 'my-attempts', params],
    queryFn: async () => {
      const { data } = await testTakingApi.getMyAttempts(params);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}
