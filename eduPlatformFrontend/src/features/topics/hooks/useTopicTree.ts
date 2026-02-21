import { useQuery } from '@tanstack/react-query';
import { topicApi } from '@/api/topicApi';

export function useTopicTree(subjectId: string | undefined) {
  return useQuery({
    queryKey: ['topics', subjectId],
    queryFn: async () => {
      const { data } = await topicApi.getTopicTree(subjectId!);
      return data.data;
    },
    enabled: !!subjectId,
  });
}
