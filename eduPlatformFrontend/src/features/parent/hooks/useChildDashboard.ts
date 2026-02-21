import { useQuery } from '@tanstack/react-query';
import { parentApi } from '@/api/parentApi';

export function useChildDashboard(childId: string) {
  return useQuery({
    queryKey: ['parent', 'children', childId, 'dashboard'],
    queryFn: async () => {
      const { data } = await parentApi.getChildDashboard(childId);
      return data.data;
    },
    enabled: !!childId,
  });
}
