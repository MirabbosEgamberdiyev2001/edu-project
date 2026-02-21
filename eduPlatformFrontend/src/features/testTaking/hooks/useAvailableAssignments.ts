import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { testTakingApi } from '@/api/testTakingApi';

export function useAvailableAssignments(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['test-taking', 'available', params],
    queryFn: async () => {
      const { data } = await testTakingApi.getAvailableAssignments(params);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}
