import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/api/authApi';
import { useToast } from '@/hooks/useToast';
import type { RegisterRequest } from '@/types/auth';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useRegister() {
  const navigate = useNavigate();
  const toast = useToast();

  return useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
    onSuccess: (_resp, variables) => {
      const identifier = variables.email || variables.phone || '';
      sessionStorage.setItem(
        'otpState',
        JSON.stringify({ identifier, purpose: 'REGISTER' }),
      );
      navigate('/auth/otp-verify', {
        state: {
          identifier,
          purpose: 'REGISTER',
          registerData: variables,
        },
        replace: true,
      });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Registration failed');
    },
  });
}
