import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/api/authApi';
import { useAuthStore } from '@/stores/authStore';
import { storage } from '@/lib/storage';
import { useToast } from '@/hooks/useToast';
import type { OtpVerifyRequest } from '@/types/auth';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useOtpVerify() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const toast = useToast();

  return useMutation({
    mutationFn: (data: OtpVerifyRequest) => authApi.verifyOtp(data),
    onSuccess: ({ data: resp }) => {
      sessionStorage.removeItem('otpState');
      const result = resp.data;

      // Registration flow: auto-login
      if (result.accessToken && result.refreshToken && result.registeredUser) {
        storage.setTokens(result.accessToken, result.refreshToken);
        setAuth(result.registeredUser);
        toast.success(resp.message);
        // Small delay to ensure store is flushed before navigation
        setTimeout(() => navigate('/dashboard', { replace: true }), 50);
        return;
      }

      // Password reset flow: navigate to reset page with token
      if (result.resetToken) {
        toast.success(resp.message);
        navigate('/auth/reset-password', {
          state: { resetToken: result.resetToken },
          replace: true,
        });
        return;
      }

      // Verified but no specific action (fallback)
      toast.success(resp.message);
      navigate('/auth/login', { replace: true });
    },
    onError: (error: AxiosError<ApiError>) => {
      // Error is displayed in OtpVerifyPage via verifyOtp.isError
      toast.error(error.response?.data?.message || 'Verification failed');
    },
  });
}
