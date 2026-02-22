import { useMutation, useQueryClient } from '@tanstack/react-query';
import { topicApi } from '@/api/topicApi';
import { useToast } from '@/hooks/useToast';
import { useTranslation } from 'react-i18next';
import type { CreateTopicRequest, UpdateTopicRequest, MoveTopicRequest, ReorderTopicsRequest } from '@/types/topic';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useTopicMutations(subjectId: string | undefined) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('common');

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['topics', subjectId] });
    queryClient.invalidateQueries({ queryKey: ['subjects'] });
    queryClient.invalidateQueries({ queryKey: ['subject', subjectId] });
  };

  const create = useMutation({
    mutationFn: (data: CreateTopicRequest) => topicApi.createTopic(subjectId!, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const update = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateTopicRequest }) =>
      topicApi.updateTopic(id, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => topicApi.deleteTopic(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const move = useMutation({
    mutationFn: ({ id, data }: { id: string; data: MoveTopicRequest }) =>
      topicApi.moveTopic(id, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const reorder = useMutation({
    mutationFn: (data: ReorderTopicsRequest) => topicApi.reorderTopics(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  return { create, update, remove, move, reorder };
}
