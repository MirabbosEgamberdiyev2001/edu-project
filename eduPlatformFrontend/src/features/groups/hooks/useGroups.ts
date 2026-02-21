import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { groupApi, type GroupListParams, type MyGroupsParams } from '@/api/groupApi';

export function useGroups(params?: GroupListParams) {
  return useQuery({
    queryKey: ['groups', params],
    queryFn: async () => {
      const { data } = await groupApi.getGroups(params);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useMyGroups(params?: MyGroupsParams) {
  return useQuery({
    queryKey: ['groups', 'my', params],
    queryFn: async () => {
      const { data } = await groupApi.getMyGroups(params);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}

export function useGroup(id: string | undefined) {
  return useQuery({
    queryKey: ['groups', id],
    queryFn: async () => {
      const { data } = await groupApi.getGroup(id!);
      return data.data;
    },
    enabled: !!id,
  });
}
