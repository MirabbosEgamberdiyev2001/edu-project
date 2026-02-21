import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/api/authApi';
import { useToast } from '@/hooks/useToast';
import type { PasswordResetRequest } from '@/types/auth';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useResetPassword() {
  const navigate = useNavigate();
  const toast = useToast();

  return useMutation({
    mutationFn: (data: PasswordResetRequest) => authApi.resetPassword(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message || 'Password reset successfully');
      navigate('/auth/login', { replace: true });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to reset password');
    },
  });
}
