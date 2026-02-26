import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation('group');
  const { t: tCommon } = useTranslation('common');

  return useMutation({
    mutationFn: (data: AddMembersRequest) => groupApi.addMembers(groupId, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message || t('success.membersAdded'));
      queryClient.invalidateQueries({ queryKey: ['groups', groupId, 'members'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || tCommon('error'));
    },
  });
}

export function useRemoveMember(groupId: string) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('group');
  const { t: tCommon } = useTranslation('common');

  return useMutation({
    mutationFn: (studentId: string) => groupApi.removeMember(groupId, studentId),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message || t('success.memberRemoved'));
      queryClient.invalidateQueries({ queryKey: ['groups', groupId, 'members'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || tCommon('error'));
    },
  });
}

export function useRemoveMembersBatch(groupId: string) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('group');
  const { t: tCommon } = useTranslation('common');

  return useMutation({
    mutationFn: (studentIds: string[]) => groupApi.removeMembersBatch(groupId, { studentIds }),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message || t('success.membersBatchRemoved'));
      queryClient.invalidateQueries({ queryKey: ['groups', groupId, 'members'] });
      queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || tCommon('error'));
    },
  });
}
