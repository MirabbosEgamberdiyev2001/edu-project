import { useMutation } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { authApi } from '@/api/authApi';
import { useToast } from '@/hooks/useToast';
import type { ChangePasswordRequest } from '@/types/auth';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useChangePassword() {
  const toast = useToast();
  const { t } = useTranslation('common');

  return useMutation({
    mutationFn: (data: ChangePasswordRequest) => authApi.changePassword(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message || 'Password changed successfully');
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });
}
