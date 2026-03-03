import axios from 'axios';
import type { ApiResponse } from '@/types/api';

export interface PublicPromoInfoDto {
  assignmentId: string;
  assignmentTitle: string;
  testTitle: string;
  questionCount: number | null;
  durationMinutes: number;
  teacherFullName: string | null;
  startsAt: string | null;
  endsAt: string | null;
  maxAttempts: number;
  showResults: boolean;
}

export interface PublicPromoEnrollRequest {
  firstName: string;
  lastName: string;
  phone: string;
}

export interface PromoEnrollResultDto {
  accessToken: string;
  assignmentId: string;
  studentId: string;
}

// Use raw axios (no auth token) for public endpoints
const publicAxios = axios.create({ baseURL: '/api/v1' });

export const publicPromoApi = {
  verifyCode: (code: string) =>
    publicAxios.get<ApiResponse<PublicPromoInfoDto>>(`/public/promo/${code}/verify`),

  enroll: (code: string, data: PublicPromoEnrollRequest) =>
    publicAxios.post<ApiResponse<PromoEnrollResultDto>>(`/public/promo/${code}/enroll`, data),
};
