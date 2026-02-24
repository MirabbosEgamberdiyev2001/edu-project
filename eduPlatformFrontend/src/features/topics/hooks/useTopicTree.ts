import { useQuery } from '@tanstack/react-query';
import { topicApi } from '@/api/topicApi';

export function useTopicTree(subjectId: string | undefined, gradeLevel: number | null) {
  return useQuery({
    queryKey: ['topics', subjectId, gradeLevel],
    queryFn: async ({ signal }) => {
      const { data } = await topicApi.getTopicTree(subjectId!, gradeLevel!, signal);
      return data.data;
    },
    enabled: !!subjectId && gradeLevel !== null,
  });
}
