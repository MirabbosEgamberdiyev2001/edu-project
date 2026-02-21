import api from '@/lib/axios';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  AttemptDto,
  SubmitAnswerRequest,
  BatchSaveAnswerRequest,
  BatchSaveAnswerResponse,
  StartAttemptRequest,
  AttemptAnswerDto,
  GradeAnswerRequest,
  AttemptResultDto,
} from '@/types/testTaking';
import type { AssignmentDto } from '@/types/assignment';

const TEST_TAKING = '/test-taking';

export const testTakingApi = {
  getAvailableAssignments: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<AssignmentDto>>>(`${TEST_TAKING}/assignments`, { params }),

  startAttempt: (assignmentId: string, data?: StartAttemptRequest) =>
    api.post<ApiResponse<AttemptDto>>(`${TEST_TAKING}/assignments/${assignmentId}/start`, data || {}),

  submitAnswer: (attemptId: string, data: SubmitAnswerRequest) =>
    api.post<ApiResponse<AttemptAnswerDto>>(`${TEST_TAKING}/attempts/${attemptId}/answer`, data),

  batchSaveAnswers: (attemptId: string, data: BatchSaveAnswerRequest) =>
    api.post<ApiResponse<BatchSaveAnswerResponse>>(`${TEST_TAKING}/attempts/${attemptId}/answers/batch`, data),

  submitAttempt: (attemptId: string) =>
    api.post<ApiResponse<AttemptDto>>(`${TEST_TAKING}/attempts/${attemptId}/submit`),

  reportTabSwitch: (attemptId: string) =>
    api.post<ApiResponse<AttemptDto>>(`${TEST_TAKING}/attempts/${attemptId}/tab-switch`),

  getAttempt: (attemptId: string) =>
    api.get<ApiResponse<AttemptDto>>(`${TEST_TAKING}/attempts/${attemptId}`),

  getMyAttempts: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<AttemptDto>>>(`${TEST_TAKING}/my-attempts`, { params }),

  gradeAnswer: (data: GradeAnswerRequest) =>
    api.post<ApiResponse<void>>(`${TEST_TAKING}/grade`, data),
};
