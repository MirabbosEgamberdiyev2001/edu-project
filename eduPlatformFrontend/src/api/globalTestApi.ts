import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type { PagedResponse } from '@/types/subject';
import type { TestHistoryDto, GlobalTestStartResponse, TestCategory } from '@/types/test';

const BASE = '/global-tests';

export const globalTestApi = {
  getAll: (params?: {
    category?: TestCategory | '';
    subjectId?: string;
    gradeLevel?: number | '';
    page?: number;
    size?: number;
  }, signal?: AbortSignal) => {
    const cleanParams: Record<string, unknown> = {};
    if (params?.category) cleanParams.category = params.category;
    if (params?.subjectId) cleanParams.subjectId = params.subjectId;
    if (params?.gradeLevel) cleanParams.gradeLevel = params.gradeLevel;
    if (params?.page !== undefined) cleanParams.page = params.page;
    if (params?.size !== undefined) cleanParams.size = params.size;
    return api.get<ApiResponse<PagedResponse<TestHistoryDto>>>(BASE, { params: cleanParams, signal });
  },

  getById: (id: string) =>
    api.get<ApiResponse<TestHistoryDto>>(`${BASE}/${id}`),

  start: (id: string) =>
    api.post<ApiResponse<GlobalTestStartResponse>>(`${BASE}/${id}/start`),
};
