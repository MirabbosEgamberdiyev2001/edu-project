import { useQuery } from '@tanstack/react-query';
import { subscriptionApi } from '@/api/subscriptionApi';

export function useMySubscription() {
  return useQuery({
    queryKey: ['my-subscription'],
    queryFn: async () => {
      const { data } = await subscriptionApi.getMySubscription();
      return data.data;
    },
  });
}

export function useMyUsage() {
  return useQuery({
    queryKey: ['my-subscription', 'usage'],
    queryFn: async () => {
      const { data } = await subscriptionApi.getMyUsage();
      return data.data;
    },
  });
}
