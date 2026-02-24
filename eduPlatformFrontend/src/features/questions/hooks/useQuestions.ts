import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { questionApi } from '@/api/questionApi';
import type { QuestionListParams } from '@/types/question';

export function useQuestions(params?: QuestionListParams) {
  return useQuery({
    queryKey: ['questions', params],
    queryFn: async ({ signal }) => {
      const { data } = await questionApi.getQuestions(params, signal);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useQuestion(id: string | undefined) {
  return useQuery({
    queryKey: ['question', id],
    queryFn: async ({ signal }) => {
      const { data } = await questionApi.getQuestion(id!, signal);
      return data.data;
    },
    enabled: !!id,
  });
}

export function useQuestionsByIds(ids: string[]) {
  return useQuery({
    queryKey: ['questions', 'by-ids', ids],
    queryFn: async () => {
      const { data } = await questionApi.getQuestionsByIds(ids);
      return data.data;
    },
    enabled: ids.length > 0,
  });
}

export function useQuestionVersions(id: string | undefined) {
  return useQuery({
    queryKey: ['question-versions', id],
    queryFn: async ({ signal }) => {
      const { data } = await questionApi.getVersionHistory(id!, signal);
      return data.data;
    },
    enabled: !!id,
  });
}
