import { useQuery } from '@tanstack/react-query';
import { parentApi } from '@/api/parentApi';

export function useChildren() {
  return useQuery({
    queryKey: ['parent', 'children'],
    queryFn: async () => {
      const { data } = await parentApi.getChildren();
      return data.data;
    },
  });
}

export function useParents() {
  return useQuery({
    queryKey: ['parent', 'parents'],
    queryFn: async () => {
      const { data } = await parentApi.getParents();
      return data.data;
    },
  });
}
