import { useMutation, useQueryClient } from '@tanstack/react-query';
import { groupApi } from '@/api/groupApi';
import { useToast } from '@/hooks/useToast';
import type { CreateGroupRequest, UpdateGroupRequest } from '@/types/group';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useGroupMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['groups'] });

  const create = useMutation({
    mutationFn: (data: CreateGroupRequest) => groupApi.createGroup(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to create group');
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
      toast.error(error.response?.data?.message || 'Failed to update group');
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => groupApi.deleteGroup(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to delete group');
    },
  });

  const archive = useMutation({
    mutationFn: (id: string) => groupApi.archiveGroup(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to archive group');
    },
  });

  return { create, update, remove, archive };
}
