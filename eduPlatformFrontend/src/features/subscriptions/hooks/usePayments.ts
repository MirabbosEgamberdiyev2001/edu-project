import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { paymentApi } from '@/api/paymentApi';

export function useMyPayments(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['my-payments', params],
    queryFn: async () => {
      const { data } = await paymentApi.getMyPayments(params);
      return data.data;
    },
    placeholderData: keepPreviousData,
  });
}
