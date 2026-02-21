import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import { useToast } from '@/hooks/useToast';
import { testApi } from '@/api/testApi';
import type { GenerateTestRequest } from '@/types/test';
import type { ApiError } from '@/types/api';

export function useTestMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['tests'] });

  const generate = useMutation({
    mutationFn: (data: GenerateTestRequest) => testApi.generate(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to generate test');
    },
  });

  const remove = useMutation({
    mutationFn: (id: string) => testApi.deleteHistory(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to delete test');
    },
  });

  const duplicate = useMutation({
    mutationFn: (id: string) => testApi.duplicate(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to duplicate test');
    },
  });

  const regenerate = useMutation({
    mutationFn: (id: string) => testApi.regenerate(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to regenerate test');
    },
  });

  return { generate, remove, duplicate, regenerate };
}
