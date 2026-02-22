import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { authApi } from '@/api/authApi';
import { useToast } from '@/hooks/useToast';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useSessions() {
  return useQuery({
    queryKey: ['sessions'],
    queryFn: async () => {
      const { data } = await authApi.getSessions();
      return data.data;
    },
  });
}

export function useSessionMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('common');
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['sessions'] });

  const revokeSession = useMutation({
    mutationFn: (sessionId: string) => authApi.revokeSession(sessionId),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const revokeAllSessions = useMutation({
    mutationFn: () => authApi.revokeAllSessions(),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  return { revokeSession, revokeAllSessions };
}
