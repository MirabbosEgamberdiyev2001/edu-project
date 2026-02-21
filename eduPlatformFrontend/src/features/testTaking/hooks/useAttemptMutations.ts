import { useMutation, useQueryClient } from '@tanstack/react-query';
import { testTakingApi } from '@/api/testTakingApi';
import { useToast } from '@/hooks/useToast';
import type { SubmitAnswerRequest, BatchSaveAnswerRequest } from '@/types/testTaking';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useAttemptMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();

  const startAttempt = useMutation({
    mutationFn: (assignmentId: string) => testTakingApi.startAttempt(assignmentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['test-taking'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to start test');
    },
  });

  const submitAnswer = useMutation({
    mutationFn: ({ attemptId, data }: { attemptId: string; data: SubmitAnswerRequest }) =>
      testTakingApi.submitAnswer(attemptId, data),
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to save answer');
    },
  });

  const batchSaveAnswers = useMutation({
    mutationFn: ({ attemptId, data }: { attemptId: string; data: BatchSaveAnswerRequest }) =>
      testTakingApi.batchSaveAnswers(attemptId, data),
  });

  const submitAttempt = useMutation({
    mutationFn: (attemptId: string) => testTakingApi.submitAttempt(attemptId),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      queryClient.invalidateQueries({ queryKey: ['test-taking'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to submit test');
    },
  });

  const reportTabSwitch = useMutation({
    mutationFn: (attemptId: string) => testTakingApi.reportTabSwitch(attemptId),
  });

  return { startAttempt, submitAnswer, batchSaveAnswers, submitAttempt, reportTabSwitch };
}
