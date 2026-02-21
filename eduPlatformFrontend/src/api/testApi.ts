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

  deleteHistory: (id: string) =>
    api.delete<ApiResponse<void>>(`${TESTS}/history/${id}`),

  duplicate: (id: string) =>
    api.post<ApiResponse<GenerateTestResponse>>(`${TESTS}/history/${id}/duplicate`),

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
};
