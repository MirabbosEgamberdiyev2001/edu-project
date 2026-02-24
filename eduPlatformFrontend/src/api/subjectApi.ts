import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type {
  SubjectDto,
  CreateSubjectRequest,
  UpdateSubjectRequest,
  PagedResponse,
} from '@/types/subject';

const SUBJECTS = '/subjects';

export interface SubjectListParams {
  search?: string;
  gradeLevel?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: string;
}

export const subjectApi = {
  getSubjects: (params?: SubjectListParams, signal?: AbortSignal) =>
    api.get<ApiResponse<PagedResponse<SubjectDto>>>(SUBJECTS, { params, signal }),

  getArchivedSubjects: (params?: { page?: number; size?: number }, signal?: AbortSignal) =>
    api.get<ApiResponse<PagedResponse<SubjectDto>>>(`${SUBJECTS}/archived`, { params, signal }),

  getSubject: (id: string, signal?: AbortSignal) =>
    api.get<ApiResponse<SubjectDto>>(`${SUBJECTS}/${id}`, { signal }),

  createSubject: (data: CreateSubjectRequest) =>
    api.post<ApiResponse<SubjectDto>>(SUBJECTS, data),

  updateSubject: (id: string, data: UpdateSubjectRequest) =>
    api.put<ApiResponse<SubjectDto>>(`${SUBJECTS}/${id}`, data),

  deleteSubject: (id: string) =>
    api.delete<ApiResponse<void>>(`${SUBJECTS}/${id}`),

  archiveSubject: (id: string) =>
    api.post<ApiResponse<SubjectDto>>(`${SUBJECTS}/${id}/archive`),

  restoreSubject: (id: string) =>
    api.post<ApiResponse<SubjectDto>>(`${SUBJECTS}/${id}/restore`),

  createBulk: (data: { items: CreateSubjectRequest[]; skipDuplicates?: boolean }) =>
    api.post<ApiResponse<{ created: number; skipped: number; errors: string[] }>>(`${SUBJECTS}/bulk`, data),

  patchSubject: (id: string, data: UpdateSubjectRequest) =>
    api.patch<ApiResponse<SubjectDto>>(`${SUBJECTS}/${id}`, data),
};
