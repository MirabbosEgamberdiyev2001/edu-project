import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type { PagedResponse } from '@/types/subject';
import type {
  QuestionDto,
  QuestionVersionDto,
  CreateQuestionRequest,
  UpdateQuestionRequest,
  QuestionListParams,
  BulkModerationResponse,
} from '@/types/question';

const QUESTIONS = '/questions';

export const questionApi = {
  getQuestions: (params?: QuestionListParams) =>
    api.get<ApiResponse<PagedResponse<QuestionDto>>>(QUESTIONS, { params }),

  getQuestion: (id: string) =>
    api.get<ApiResponse<QuestionDto>>(`${QUESTIONS}/${id}`),

  createQuestion: (data: CreateQuestionRequest) =>
    api.post<ApiResponse<QuestionDto>>(QUESTIONS, data),

  updateQuestion: (id: string, data: UpdateQuestionRequest) =>
    api.put<ApiResponse<QuestionDto>>(`${QUESTIONS}/${id}`, data),

  deleteQuestion: (id: string) =>
    api.delete<ApiResponse<void>>(`${QUESTIONS}/${id}`),

  submitForModeration: (id: string) =>
    api.post<ApiResponse<QuestionDto>>(`${QUESTIONS}/${id}/submit`),

  getQuestionsByTopic: (topicId: string, params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<QuestionDto>>>(`/topics/${topicId}/questions`, { params }),

  getVersionHistory: (id: string) =>
    api.get<ApiResponse<QuestionVersionDto[]>>(`${QUESTIONS}/${id}/versions`),

  rollbackToVersion: (id: string, version: number) =>
    api.post<ApiResponse<QuestionDto>>(`${QUESTIONS}/${id}/rollback/${version}`),

  createBulk: (data: { items: CreateQuestionRequest[]; skipDuplicates?: boolean }) =>
    api.post<ApiResponse<{ created: number; skipped: number; errors: string[] }>>(`${QUESTIONS}/bulk`, data),

  patchQuestion: (id: string, data: UpdateQuestionRequest) =>
    api.patch<ApiResponse<QuestionDto>>(`${QUESTIONS}/${id}`, data),

  getQuestionsByIds: (ids: string[]) =>
    api.post<ApiResponse<QuestionDto[]>>(`${QUESTIONS}/by-ids`, { questionIds: ids }),

  bulkSubmitForModeration: (questionIds: string[]) =>
    api.post<ApiResponse<BulkModerationResponse>>(`${QUESTIONS}/bulk-submit`, { questionIds }),

  // Import / Export
  importQuestions: (formData: FormData) =>
    api.post<ApiResponse<{ successCount: number; errorCount: number; errors: string[] }>>(
      `${QUESTIONS}/import`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    ),

  getImportTemplate: () =>
    api.get(`${QUESTIONS}/export-template`, { responseType: 'blob' }),
};
