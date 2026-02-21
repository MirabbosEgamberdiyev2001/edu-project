import api from '@/lib/axios';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  CreatePaymentRequest,
  PaymentInitiationResponse,
  PaymentDto,
} from '@/types/subscription';

const PAYMENTS = '/payments';

export const paymentApi = {
  initiatePayment: (data: CreatePaymentRequest) =>
    api.post<ApiResponse<PaymentInitiationResponse>>(`${PAYMENTS}/initiate`, data),

  getMyPayments: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<PaymentDto>>>(`${PAYMENTS}/my`, { params }),

  getPayment: (id: string) =>
    api.get<ApiResponse<PaymentDto>>(`${PAYMENTS}/${id}`),
};
