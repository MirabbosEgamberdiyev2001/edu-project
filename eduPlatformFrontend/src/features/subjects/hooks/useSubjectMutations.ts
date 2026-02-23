import { useMutation, useQueryClient } from '@tanstack/react-query';
import { subjectApi } from '@/api/subjectApi';
import { useToast } from '@/hooks/useToast';
import { useTranslation } from 'react-i18next';
import type { CreateSubjectRequest, UpdateSubjectRequest } from '@/types/subject';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useSubjectMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('common');
  const { t: tSubject } = useTranslation('subject');

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['subjects'] });

  const handleError = (error: AxiosError<ApiError>) => {
    if (error.response?.status === 409) {
      toast.error(tSubject('duplicateError'));
    } else {
      toast.error(error.response?.data?.message || t('error'));
    }
  };

  const create = useMutation({
    mutationFn: (data: CreateSubjectRequest) => subjectApi.createSubject(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: handleError,
  });

  const update = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateSubjectRequest }) =>
      subjectApi.updateSubject(id, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: handleError,
  });

  const remove = useMutation({
    mutationFn: (id: string) => subjectApi.deleteSubject(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const archive = useMutation({
    mutationFn: (id: string) => subjectApi.archiveSubject(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const restore = useMutation({
    mutationFn: (id: string) => subjectApi.restoreSubject(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  return { create, update, remove, archive, restore };
}
