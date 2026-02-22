import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { testApi } from '@/api/testApi';

export function useTests(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['tests', 'list', params],
    queryFn: async () => {
      const { data } = await testApi.getHistory(params);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useTestHistory(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['tests', 'history', params],
    queryFn: async () => {
      const { data } = await testApi.getHistory(params);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useTestDetail(id: string) {
  return useQuery({
    queryKey: ['tests', 'detail', id],
    queryFn: async () => {
      const { data } = await testApi.getHistoryById(id);
      return data.data;
    },
    enabled: !!id,
  });
}

export function useAvailableQuestions(topicIds: string[]) {
  return useQuery({
    queryKey: ['tests', 'available', topicIds],
    queryFn: async () => {
      const { data } = await testApi.getAvailableQuestions(topicIds);
      return data.data;
    },
    enabled: topicIds.length > 0,
  });
}

export function useQuestionsForSelection(params: {
  topicIds: string[];
  difficulty?: string;
  status?: string;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: ['tests', 'questions-selection', params],
    queryFn: async () => {
      const { data } = await testApi.getQuestionsForSelection(params);
      return data.data;
    },
    enabled: params.topicIds.length > 0,
    placeholderData: keepPreviousData,
  });
}
