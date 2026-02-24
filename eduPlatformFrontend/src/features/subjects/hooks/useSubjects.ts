import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { subjectApi, type SubjectListParams } from '@/api/subjectApi';

export function useSubjects(params?: SubjectListParams) {
  return useQuery({
    queryKey: ['subjects', params],
    queryFn: async ({ signal }) => {
      const { data } = await subjectApi.getSubjects(params, signal);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useArchivedSubjects(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['subjects', 'archived', params],
    queryFn: async ({ signal }) => {
      const { data } = await subjectApi.getArchivedSubjects(params, signal);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}
