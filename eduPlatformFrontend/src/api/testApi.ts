import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type { PagedResponse } from '@/types/subject';
import type { QuestionDto } from '@/types/question';
import type {
  GenerateTestRequest,
  GenerateTestResponse,
  TestHistoryDto,
  AvailableQuestionsResponse,
  ExportFormat,
} from '@/types/test';

const TESTS = '/tests';

export const testApi = {
  generate: (data: GenerateTestRequest) =>
    api.post<ApiResponse<GenerateTestResponse>>(`${TESTS}/generate`, data),

  preview: (data: GenerateTestRequest) =>
    api.post<ApiResponse<GenerateTestResponse>>(`${TESTS}/generate/preview`, data),

  validate: (data: GenerateTestRequest) =>
    api.post<ApiResponse<void>>(`${TESTS}/generate/validate`, data),

  getAvailableQuestions: (topicIds: string[]) =>
    api.get<ApiResponse<AvailableQuestionsResponse>>(`${TESTS}/generate/available`, {
      params: { topicIds: topicIds.join(',') },
    }),

  getQuestionsForSelection: (params: {
    topicIds: string[];
    difficulty?: string;
    status?: string;
    page?: number;
    size?: number;
  }) =>
    api.get<ApiResponse<PagedResponse<QuestionDto>>>(`${TESTS}/generate/questions`, {
      params: {
        topicIds: params.topicIds.join(','),
        difficulty: params.difficulty || undefined,
        status: params.status || undefined,
        page: params.page ?? 0,
        size: params.size ?? 50,
      },
    }),

  getHistory: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<TestHistoryDto>>>(`${TESTS}/history`, { params }),

  getHistoryById: (id: string) =>
    api.get<ApiResponse<TestHistoryDto>>(`${TESTS}/history/${id}`),

  updateHistory: (id: string, data: Record<string, unknown>) =>
    api.put<ApiResponse<TestHistoryDto>>(`${TESTS}/history/${id}`, data),

  deleteHistory: (id: string) =>
    api.delete<ApiResponse<void>>(`${TESTS}/history/${id}`),

  regenerate: (id: string) =>
    api.post<ApiResponse<GenerateTestResponse>>(`${TESTS}/history/${id}/regenerate`),

  exportTest: (id: string, format: ExportFormat = 'PDF') =>
    api.get<Blob>(`${TESTS}/history/${id}/export/test`, {
      params: { format },
      responseType: 'blob',
    }),

  exportAnswerKey: (id: string, format: ExportFormat = 'PDF') =>
    api.get<Blob>(`${TESTS}/history/${id}/export/answer-key`, {
      params: { format },
      responseType: 'blob',
    }),

  exportCombined: (id: string, format: ExportFormat = 'PDF') =>
    api.get<Blob>(`${TESTS}/history/${id}/export/combined`, {
      params: { format },
      responseType: 'blob',
    }),

  exportProofs: (id: string, format: ExportFormat = 'PDF') =>
    api.get<Blob>(`${TESTS}/history/${id}/export/proofs`, {
      params: { format },
      responseType: 'blob',
    }),

  publishTest: (id: string, durationMinutes?: number) =>
    api.post<ApiResponse<TestHistoryDto>>(`${TESTS}/history/${id}/publish`, null, {
      params: durationMinutes ? { durationMinutes } : undefined,
    }),

  unpublishTest: (id: string) =>
    api.delete<ApiResponse<TestHistoryDto>>(`${TESTS}/history/${id}/publish`),

  getPublicTest: (slug: string) =>
    api.get<ApiResponse<TestHistoryDto>>(`/public-tests/${slug}`),
};
