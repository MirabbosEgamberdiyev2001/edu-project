import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { subscriptionPlanApi } from '@/api/subscriptionPlanApi';

export function usePlans() {
  return useQuery({
    queryKey: ['subscription-plans'],
    queryFn: async () => {
      const { data } = await subscriptionPlanApi.getPlans();
      return data.data;
    },
  });
}

export function useAllPlans(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['subscription-plans', 'all', params],
    queryFn: async () => {
      const { data } = await subscriptionPlanApi.getAllPlans(params);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}
