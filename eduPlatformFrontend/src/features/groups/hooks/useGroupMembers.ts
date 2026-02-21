import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { groupApi } from '@/api/groupApi';
import { useToast } from '@/hooks/useToast';
import type { AddMembersRequest } from '@/types/group';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useGroupMembers(groupId: string | undefined) {
  return useQuery({
    queryKey: ['groups', groupId, 'members'],
    queryFn: async () => {
      const { data } = await groupApi.getMembers(groupId!);
      return data.data;
    },
    enabled: !!groupId,
  });
}

export function useAddMembers(groupId: string) {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useMutation({
    mutationFn: (data: AddMembersRequest) => groupApi.addMembers(groupId, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      queryClient.invalidateQueries({ queryKey: ['groups', groupId, 'members'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to add members');
    },
  });
}

export function useRemoveMember(groupId: string) {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useMutation({
    mutationFn: (studentId: string) => groupApi.removeMember(groupId, studentId),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      queryClient.invalidateQueries({ queryKey: ['groups', groupId, 'members'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to remove member');
    },
  });
}
