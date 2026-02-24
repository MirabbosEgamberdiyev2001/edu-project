import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { testTakingApi } from '@/api/testTakingApi';

export function useAvailableAssignments(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['test-taking', 'available', params],
    queryFn: async ({ signal }) => {
      const { data } = await testTakingApi.getAvailableAssignments(params, signal);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}
