import { useQuery } from '@tanstack/react-query';
import { subjectApi } from '@/api/subjectApi';

export function useSubject(id: string | undefined) {
  return useQuery({
    queryKey: ['subject', id],
    queryFn: async () => {
      const { data } = await subjectApi.getSubject(id!);
      return data.data;
    },
    enabled: !!id,
  });
}
