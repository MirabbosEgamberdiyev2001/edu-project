import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { assignmentApi } from '@/api/assignmentApi';
import { useToast } from '@/hooks/useToast';
import type { CreateAssignmentRequest, UpdateAssignmentRequest } from '@/types/assignment';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useAssignmentMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('common');

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['assignments'] });

  const create = useMutation({
    mutationFn: (data: CreateAssignmentRequest) => assignmentApi.createAssignment(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const update = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateAssignmentRequest }) =>
      assignmentApi.updateAssignment(id, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const activate = useMutation({
    mutationFn: (id: string) => assignmentApi.activateAssignment(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const cancel = useMutation({
    mutationFn: (id: string) => assignmentApi.cancelAssignment(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => assignmentApi.deleteAssignment(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  return { create, update, activate, cancel, remove };
}
