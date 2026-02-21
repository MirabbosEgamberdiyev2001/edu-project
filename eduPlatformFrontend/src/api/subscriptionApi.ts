import api from '@/lib/axios';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type { UserSubscriptionDto, UsageDto } from '@/types/subscription';

const SUBSCRIPTIONS = '/subscriptions';

export const subscriptionApi = {
  getMySubscription: () =>
    api.get<ApiResponse<UserSubscriptionDto>>(`${SUBSCRIPTIONS}/my`),

  getMySubscriptionHistory: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<UserSubscriptionDto>>>(`${SUBSCRIPTIONS}/my/history`, { params }),

  getMyUsage: () =>
    api.get<ApiResponse<UsageDto[]>>(`${SUBSCRIPTIONS}/my/usage`),

  cancelSubscription: (id: string) =>
    api.post<ApiResponse<UserSubscriptionDto>>(`${SUBSCRIPTIONS}/${id}/cancel`),
};
