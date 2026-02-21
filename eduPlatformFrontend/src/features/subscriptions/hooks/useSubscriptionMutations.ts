import { useMutation, useQueryClient } from '@tanstack/react-query';
import { subscriptionApi } from '@/api/subscriptionApi';
import { subscriptionPlanApi } from '@/api/subscriptionPlanApi';
import { paymentApi } from '@/api/paymentApi';
import { useToast } from '@/hooks/useToast';
import type { CreatePlanRequest, UpdatePlanRequest, CreatePaymentRequest } from '@/types/subscription';
import type { AxiosError } from 'axios';
import type { ApiError } from '@/types/api';

export function useSubscriptionMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();

  const cancelSubscription = useMutation({
    mutationFn: (id: string) => subscriptionApi.cancelSubscription(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      queryClient.invalidateQueries({ queryKey: ['my-subscription'] });
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to cancel subscription');
    },
  });

  const initiatePayment = useMutation({
    mutationFn: (data: CreatePaymentRequest) => paymentApi.initiatePayment(data),
    onSuccess: ({ data: resp }) => {
      window.location.href = resp.data.redirectUrl;
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to initiate payment');
    },
  });

  return { cancelSubscription, initiatePayment };
}

export function usePlanMutations() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['subscription-plans'] });

  const createPlan = useMutation({
    mutationFn: (data: CreatePlanRequest) => subscriptionPlanApi.createPlan(data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to create plan');
    },
  });

  const updatePlan = useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdatePlanRequest }) =>
      subscriptionPlanApi.updatePlan(id, data),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to update plan');
    },
  });

  const deletePlan = useMutation({
    mutationFn: (id: string) => subscriptionPlanApi.deletePlan(id),
    onSuccess: ({ data: resp }) => {
      toast.success(resp.message);
      invalidate();
    },
    onError: (error: AxiosError<ApiError>) => {
      toast.error(error.response?.data?.message || 'Failed to delete plan');
    },
  });

  return { createPlan, updatePlan, deletePlan };
}
