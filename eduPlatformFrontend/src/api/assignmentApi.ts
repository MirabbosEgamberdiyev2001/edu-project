import api from '@/lib/axios';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  AssignmentDto,
  CreateAssignmentRequest,
  UpdateAssignmentRequest,
  AssignmentResultDto,
  LiveMonitoringDto,
  AssignmentStatus,
} from '@/types/assignment';

const ASSIGNMENTS = '/assignments';

export interface AssignmentListParams {
  status?: AssignmentStatus;
  search?: string;
  page?: number;
  size?: number;
}

export const assignmentApi = {
  createAssignment: (data: CreateAssignmentRequest) =>
    api.post<ApiResponse<AssignmentDto>>(ASSIGNMENTS, data),

  getAssignments: (params?: AssignmentListParams) =>
    api.get<ApiResponse<PagedResponse<AssignmentDto>>>(ASSIGNMENTS, { params }),

  getAssignment: (id: string) =>
    api.get<ApiResponse<AssignmentDto>>(`${ASSIGNMENTS}/${id}`),

  updateAssignment: (id: string, data: UpdateAssignmentRequest) =>
    api.put<ApiResponse<AssignmentDto>>(`${ASSIGNMENTS}/${id}`, data),

  activateAssignment: (id: string) =>
    api.post<ApiResponse<AssignmentDto>>(`${ASSIGNMENTS}/${id}/activate`),

  cancelAssignment: (id: string) =>
    api.post<ApiResponse<AssignmentDto>>(`${ASSIGNMENTS}/${id}/cancel`),

  deleteAssignment: (id: string) =>
    api.delete<ApiResponse<void>>(`${ASSIGNMENTS}/${id}`),

  getLiveMonitoring: (id: string) =>
    api.get<ApiResponse<LiveMonitoringDto>>(`${ASSIGNMENTS}/${id}/live`),

  getResults: (id: string) =>
    api.get<ApiResponse<AssignmentResultDto>>(`${ASSIGNMENTS}/${id}/results`),

  exportResults: (id: string, format: 'CSV' | 'EXCEL') =>
    api.get(`${ASSIGNMENTS}/${id}/results/export`, {
      params: { format },
      responseType: 'blob',
    }),
};
