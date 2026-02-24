import { useQuery } from '@tanstack/react-query';
import { subjectApi } from '@/api/subjectApi';

export function useSubject(id: string | undefined) {
  return useQuery({
    queryKey: ['subject', id],
    queryFn: async ({ signal }) => {
      const { data } = await subjectApi.getSubject(id!, signal);
      return data.data;
    },
    enabled: !!id,
  });
}
