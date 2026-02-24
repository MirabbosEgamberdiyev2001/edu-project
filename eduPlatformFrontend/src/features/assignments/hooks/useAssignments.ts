import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { assignmentApi, type AssignmentListParams } from '@/api/assignmentApi';

export function useAssignments(params?: AssignmentListParams) {
  return useQuery({
    queryKey: ['assignments', params],
    queryFn: async ({ signal }) => {
      const { data } = await assignmentApi.getAssignments(params, signal);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useAssignment(id: string) {
  return useQuery({
    queryKey: ['assignments', id],
    queryFn: async ({ signal }) => {
      const { data } = await assignmentApi.getAssignment(id, signal);
      return data.data;
    },
    enabled: !!id,
  });
}
