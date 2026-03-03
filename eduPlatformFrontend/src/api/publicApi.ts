import axios from 'axios';
import type { ApiResponse } from '@/types/api';

// Raw axios — no auth token for public endpoints
const publicAxios = axios.create({ baseURL: '/api/v1' });

export interface PublicStats {
  totalUsers: number;
  totalAttempts: number;
  totalSubjects: number;
}

export interface PublicSubjectDto {
  name: string;
  description: string | null;
  testCount: number | null;
  icon: string | null;
  color: string | null;
}

export const publicApi = {
  getStats: () =>
    publicAxios.get<ApiResponse<PublicStats>>('/public/stats'),

  getSubjects: (lang: string) =>
    publicAxios.get<ApiResponse<PublicSubjectDto[]>>(`/public/subjects?lang=${lang}`),
};
