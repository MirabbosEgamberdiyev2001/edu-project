import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/api/authApi';
import { useAuthStore } from '@/stores/authStore';
import { storage } from '@/lib/storage';
import { useToast } from '@/hooks/useToast';
import type { TelegramAuthRequest } from '@/types/auth';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useTelegramAuth() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const toast = useToast();

  return useMutation({
    mutationFn: (data: TelegramAuthRequest) => authApi.telegramAuth(data),
    onSuccess: ({ data: resp }) => {
      const { accessToken, refreshToken, user } = resp.data;
      storage.setTokens(accessToken, refreshToken);
      setAuth(user);
      navigate('/dashboard', { replace: true });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Telegram authentication failed');
    },
  });
}
