import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { parentApi } from '@/api/parentApi';
import { useToast } from '@/hooks/useToast';
import type { PairWithCodeRequest } from '@/types/parent';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useParentMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('common');

  const invalidateChildren = () =>
    queryClient.invalidateQueries({ queryKey: ['parent', 'children'] });

  const invalidateParents = () =>
    queryClient.invalidateQueries({ queryKey: ['parent', 'parents'] });

  const generatePairingCode = useMutation({
    mutationFn: () => parentApi.generatePairingCode(),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const pairWithCode = useMutation({
    mutationFn: (data: PairWithCodeRequest) => parentApi.pairWithCode(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidateChildren();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const revokePairing = useMutation({
    mutationFn: (id: string) => parentApi.revokePairing(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidateChildren();
      invalidateParents();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  return { generatePairingCode, pairWithCode, revokePairing };
}
