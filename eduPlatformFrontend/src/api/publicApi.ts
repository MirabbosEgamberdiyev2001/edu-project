import axios from 'axios';
import type { ApiResponse } from '@/types/api';

// Raw axios — no auth token for public endpoints
const publicAxios = axios.create({ baseURL: '/api/v1' });

export interface PublicStats {
  totalUsers: number;
  totalAttempts: number;
  totalSubjects: number;
}

export const publicApi = {
  getStats: () =>
    publicAxios.get<ApiResponse<PublicStats>>('/public/stats'),

  getSubjects: () =>
    publicAxios.get<ApiResponse<string[]>>('/public/subjects'),
};
