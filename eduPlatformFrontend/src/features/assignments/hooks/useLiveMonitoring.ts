import { useQuery } from '@tanstack/react-query';
import { assignmentApi } from '@/api/assignmentApi';

export function useLiveMonitoring(assignmentId: string, enabled = true) {
  return useQuery({
    queryKey: ['assignments', assignmentId, 'live'],
    queryFn: async () => {
      const { data } = await assignmentApi.getLiveMonitoring(assignmentId);
      return data.data;
    },
    enabled: !!assignmentId && enabled,
    refetchInterval: 5000,
  });
}
