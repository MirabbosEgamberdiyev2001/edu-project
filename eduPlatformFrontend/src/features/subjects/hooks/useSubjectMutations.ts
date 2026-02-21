import { useMutation, useQueryClient } from '@tanstack/react-query';
import { subjectApi } from '@/api/subjectApi';
import { useToast } from '@/hooks/useToast';
import type { CreateSubjectRequest, UpdateSubjectRequest } from '@/types/subject';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useSubjectMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['subjects'] });

  const create = useMutation({
    mutationFn: (data: CreateSubjectRequest) => subjectApi.createSubject(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to create subject');
    },
  });

  const update = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateSubjectRequest }) =>
      subjectApi.updateSubject(id, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to update subject');
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => subjectApi.deleteSubject(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to delete subject');
    },
  });

  const archive = useMutation({
    mutationFn: (id: string) => subjectApi.archiveSubject(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to archive subject');
    },
  });

  const restore = useMutation({
    mutationFn: (id: string) => subjectApi.restoreSubject(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to restore subject');
    },
  });

  const fork = useMutation({
    mutationFn: (id: string) => subjectApi.forkTemplate(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to fork template');
    },
  });

  return { create, update, remove, archive, restore, fork };
}
