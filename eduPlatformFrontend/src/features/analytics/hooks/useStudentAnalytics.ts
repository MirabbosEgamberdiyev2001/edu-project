import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '@/api/analyticsApi';

export function useMyAnalytics() {
  return useQuery({
    queryKey: ['analytics', 'student', 'me'],
    queryFn: async () => {
      const { data } = await analyticsApi.getMyAnalytics();
      return data.data;
    },
  });
}

export function useStudentAnalytics(studentId: string) {
  return useQuery({
    queryKey: ['analytics', 'student', studentId],
    queryFn: async () => {
      const { data } = await analyticsApi.getStudentAnalytics(studentId);
      return data.data;
    },
    enabled: !!studentId,
  });
}
