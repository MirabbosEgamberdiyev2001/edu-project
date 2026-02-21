import api from '@/lib/axios';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  SubscriptionPlanDto,
  CreatePlanRequest,
  UpdatePlanRequest,
} from '@/types/subscription';

const PLANS = '/subscription-plans';

export const subscriptionPlanApi = {
  getPlans: () =>
    api.get<ApiResponse<SubscriptionPlanDto[]>>(PLANS),

  getAllPlans: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<SubscriptionPlanDto>>>(`${PLANS}/all`, { params }),

  getPlan: (id: string) =>
    api.get<ApiResponse<SubscriptionPlanDto>>(`${PLANS}/${id}`),

  createPlan: (data: CreatePlanRequest) =>
    api.post<ApiResponse<SubscriptionPlanDto>>(PLANS, data),

  updatePlan: (id: string, data: UpdatePlanRequest) =>
    api.put<ApiResponse<SubscriptionPlanDto>>(`${PLANS}/${id}`, data),

  deletePlan: (id: string) =>
    api.delete<ApiResponse<void>>(`${PLANS}/${id}`),
};
