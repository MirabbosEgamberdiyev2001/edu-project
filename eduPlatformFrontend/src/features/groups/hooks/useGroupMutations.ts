import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { groupApi } from '@/api/groupApi';
import { useToast } from '@/hooks/useToast';
import type { CreateGroupRequest, UpdateGroupRequest } from '@/types/group';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useGroupMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('common');

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['groups'] });

  const create = useMutation({
    mutationFn: (data: CreateGroupRequest) => groupApi.createGroup(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const update = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateGroupRequest }) =>
      groupApi.updateGroup(id, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => groupApi.deleteGroup(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const archive = useMutation({
    mutationFn: (id: string) => groupApi.archiveGroup(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  return { create, update, remove, archive };
}
