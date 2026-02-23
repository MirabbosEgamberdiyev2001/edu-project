import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { AxiosError } from 'axios';
import { useToast } from '@/hooks/useToast';
import { testApi } from '@/api/testApi';
import type { GenerateTestRequest, HeaderConfig } from '@/types/test';
import type { ApiError } from '@/types/api';

export function useTestMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation('common');

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['tests'] });

  const generate = useMutation({
    mutationFn: (data: GenerateTestRequest) => testApi.generate(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const update = useMutation({
    mutationFn: ({ id, data }: { id: string; data: { title?: string; titleTranslations?: Record<string, string>; category?: string; headerConfig?: HeaderConfig } }) =>
      testApi.updateHistory(id, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => testApi.deleteHistory(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  const regenerate = useMutation({
    mutationFn: (id: string) => testApi.regenerate(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || t('error'));
    },
  });

  return { generate, update, remove, regenerate };
}
