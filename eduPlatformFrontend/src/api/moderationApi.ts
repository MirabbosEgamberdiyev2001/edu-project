import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type { PagedResponse } from '@/types/subject';
import type {
  QuestionDto,
  BulkModerationRequest,
  BulkModerationResponse,
} from '@/types/question';

const MODERATION = '/moderation';

export const moderationApi = {
  getPendingQuestions: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<QuestionDto>>>(`${MODERATION}/questions`, { params }),

  approveQuestion: (id: string) =>
    api.post<ApiResponse<QuestionDto>>(`${MODERATION}/questions/${id}/approve`),

  rejectQuestion: (id: string, reason: string) =>
    api.post<ApiResponse<QuestionDto>>(`${MODERATION}/questions/${id}/reject`, { reason }),

  bulkApprove: (data: BulkModerationRequest) =>
    api.post<ApiResponse<BulkModerationResponse>>(`${MODERATION}/questions/bulk-approve`, data),

  bulkReject: (data: BulkModerationRequest) =>
    api.post<ApiResponse<BulkModerationResponse>>(`${MODERATION}/questions/bulk-reject`, data),
};
