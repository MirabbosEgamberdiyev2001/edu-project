import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { questionApi } from '@/api/questionApi';
import { useToast } from '@/hooks/useToast';
import type { CreateQuestionRequest, UpdateQuestionRequest } from '@/types/question';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useQuestionMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('common');

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['questions'] });
    queryClient.invalidateQueries({ queryKey: ['question'] });
    queryClient.invalidateQueries({ queryKey: ['question-versions'] });
  };

  const create = useMutation({
    mutationFn: (data: CreateQuestionRequest) => questionApi.createQuestion(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const update = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateQuestionRequest }) =>
      questionApi.updateQuestion(id, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => questionApi.deleteQuestion(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const submitForModeration = useMutation({
    mutationFn: (id: string) => questionApi.submitForModeration(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const rollback = useMutation({
    mutationFn: ({ id, version }: { id: string; version: number }) =>
      questionApi.rollbackToVersion(id, version),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const bulkSubmit = useMutation({
    mutationFn: (questionIds: string[]) => questionApi.bulkSubmitForModeration(questionIds),
    onSuccess: ({ data: resp }) => {
      const result = resp.data;
      if (result && result.failedCount > 0) {
        toast.warning(`${resp.message} (${result.successCount} succeeded, ${result.failedCount} failed)`);
      } else {
        toast.success(resp.message);
      }
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  return { create, update, remove, submitForModeration, rollback, bulkSubmit };
}
