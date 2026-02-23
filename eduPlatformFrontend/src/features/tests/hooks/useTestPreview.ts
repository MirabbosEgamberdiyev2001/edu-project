import { useMutation } from '@tanstack/react-query';
import { testApi } from '@/api/testApi';
import type { GenerateTestRequest } from '@/types/test';

export function useTestPreview() {
  return useMutation({
    mutationFn: (data: GenerateTestRequest) =>
      testApi.preview(data).then((res) => res.data.data),
  });
}
