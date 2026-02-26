import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { testTakingApi } from '@/api/testTakingApi';
import { useToast } from '@/hooks/useToast';
import type { RedeemPromoCodeRequest } from '@/types/assignment';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useRedeemPromoCode() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('testTaking');
  const { t: tc } = useTranslation('common');

  return useMutation({
    mutationFn: (data: RedeemPromoCodeRequest) => testTakingApi.redeemPromoCode(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message || t('promoCode.redeemed'));
      queryClient.invalidateQueries({ queryKey: ['available-assignments'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || tc('error'));
    },
  });
}
