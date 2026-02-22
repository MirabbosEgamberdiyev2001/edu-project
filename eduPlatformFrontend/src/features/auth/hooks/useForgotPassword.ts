import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/api/authApi';
import { useToast } from '@/hooks/useToast';
import type { ForgotPasswordRequest } from '@/types/auth';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useForgotPassword() {
  const navigate = useNavigate();
  const toast = useToast();

  return useMutation({
    mutationFn: (data: ForgotPasswordRequest) => authApi.forgotPassword(data),
    onSuccess: (_resp, variables) => {
      const identifier = variables.email || variables.phone || '';
      sessionStorage.setItem(
        'otpState',
        JSON.stringify({ identifier, purpose: 'PASSWORD_RESET' }),
      );
      navigate('/auth/otp-verify', {
        state: {
          identifier,
          purpose: 'PASSWORD_RESET',
          forgotPasswordData: variables,
        },
        replace: true,
      });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to send reset code');
    },
  });
}
