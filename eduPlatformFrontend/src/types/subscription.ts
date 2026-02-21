export enum PlanType {
  BASIC = 'BASIC',
  PREMIUM = 'PREMIUM',
  PRO = 'PRO',
  TRIAL = 'TRIAL',
}

export enum SubscriptionStatus {
  ACTIVE = 'ACTIVE',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED',
  SUSPENDED = 'SUSPENDED',
  PENDING_PAYMENT = 'PENDING_PAYMENT',
}

export enum PaymentProvider {
  PAYME = 'PAYME',
  CLICK = 'CLICK',
  UZUM = 'UZUM',
  MANUAL = 'MANUAL',
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  REFUNDED = 'REFUNDED',
}

export interface SubscriptionPlanDto {
  id: string;
  name: string;
  type: PlanType;
  monthlyPrice: number;
  durationDays: number;
  maxTests: number;
  maxExports: number;
  maxQuestionsPerTest: number;
  features: string[];
  isActive: boolean;
  createdAt: string;
}

export interface CreatePlanRequest {
  name: string;
  type: PlanType;
  monthlyPrice: number;
  durationDays: number;
  maxTests: number;
  maxExports: number;
  maxQuestionsPerTest: number;
  features: string[];
}

export interface UpdatePlanRequest {
  name?: string;
  monthlyPrice?: number;
  maxTests?: number;
  maxExports?: number;
  maxQuestionsPerTest?: number;
  features?: string[];
  isActive?: boolean;
}

export interface UserSubscriptionDto {
  id: string;
  userId: string;
  planId: string;
  planName: string;
  planType: PlanType;
  status: SubscriptionStatus;
  startDate: string;
  endDate: string;
  renewalDate: string | null;
  cancelledAt: string | null;
  remainingDays: number;
  createdAt: string;
}

export interface UsageDto {
  usageType: string;
  count: number;
  limit: number;
  percentageUsed: number;
  resetDate: string;
}

export interface CreatePaymentRequest {
  planId: string;
  paymentProvider: PaymentProvider;
}

export interface PaymentInitiationResponse {
  paymentId: string;
  redirectUrl: string;
}

export interface PaymentDto {
  id: string;
  userId: string;
  amount: number;
  provider: PaymentProvider;
  status: PaymentStatus;
  externalTransactionId: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface AssignSubscriptionRequest {
  userId: string;
  planId: string;
  durationDays?: number;
}
