import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { moderationApi } from '@/api/moderationApi';
import type { BulkModerationRequest } from '@/types/question';

export function usePendingQuestions(params: { page?: number; size?: number } = {}) {
  return useQuery({
    queryKey: ['moderation', 'pending', params],
    queryFn: async () => {
      const { data } = await moderationApi.getPendingQuestions(params);
      return data.data;
    },
    staleTime: 15_000,
  });
}

export function useApproveQuestion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => moderationApi.approveQuestion(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['moderation'] });
    },
  });
}

export function useRejectQuestion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) =>
      moderationApi.rejectQuestion(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['moderation'] });
    },
  });
}

export function useBulkApprove() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: BulkModerationRequest) => moderationApi.bulkApprove(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['moderation'] });
    },
  });
}

export function useBulkReject() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: BulkModerationRequest) => moderationApi.bulkReject(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['moderation'] });
    },
  });
}
