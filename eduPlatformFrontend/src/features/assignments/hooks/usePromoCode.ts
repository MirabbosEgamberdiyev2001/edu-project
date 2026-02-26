import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { assignmentApi } from '@/api/assignmentApi';
import { useToast } from '@/hooks/useToast';
import type { GeneratePromoCodeRequest } from '@/types/assignment';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function usePromoCode(assignmentId: string | undefined) {
  return useQuery({
    queryKey: ['assignments', assignmentId, 'promo-code'],
    queryFn: async ({ signal }) => {
      const { data } = await assignmentApi.getPromoCode(assignmentId!, signal);
      return data.data;
    },
    enabled: !!assignmentId,
  });
}

export function useGeneratePromoCode(assignmentId: string) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('assignment');
  const { t: tc } = useTranslation('common');

  return useMutation({
    mutationFn: (data?: GeneratePromoCodeRequest) =>
      assignmentApi.generatePromoCode(assignmentId, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message || t('success.promoCodeGenerated'));
      queryClient.invalidateQueries({ queryKey: ['assignments', assignmentId, 'promo-code'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || tc('error'));
    },
  });
}

export function useRevokePromoCode(assignmentId: string) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('assignment');
  const { t: tc } = useTranslation('common');

  return useMutation({
    mutationFn: () => assignmentApi.revokePromoCode(assignmentId),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message || t('success.promoCodeRevoked'));
      queryClient.invalidateQueries({ queryKey: ['assignments', assignmentId, 'promo-code'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || tc('error'));
    },
  });
}
