import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type {
  SubjectDto,
  CreateSubjectRequest,
  UpdateSubjectRequest,
  PagedResponse,
  SubjectCategory,
} from '@/types/subject';

const SUBJECTS = '/subjects';

export interface SubjectListParams {
  category?: SubjectCategory;
  search?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: string;
}

export const subjectApi = {
  getSubjects: (params?: SubjectListParams) =>
    api.get<ApiResponse<PagedResponse<SubjectDto>>>(SUBJECTS, { params }),

  getArchivedSubjects: (params?: { page?: number; size?: number }) =>
    api.get<ApiResponse<PagedResponse<SubjectDto>>>(`${SUBJECTS}/archived`, { params }),

  getSubject: (id: string) =>
    api.get<ApiResponse<SubjectDto>>(`${SUBJECTS}/${id}`),

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

  forkTemplate: (id: string) =>
    api.post<ApiResponse<SubjectDto>>(`${SUBJECTS}/${id}/fork`),

  createBulk: (data: { items: CreateSubjectRequest[]; skipDuplicates?: boolean }) =>
    api.post<ApiResponse<{ created: number; skipped: number; errors: string[] }>>(`${SUBJECTS}/bulk`, data),

  patchSubject: (id: string, data: UpdateSubjectRequest) =>
    api.patch<ApiResponse<SubjectDto>>(`${SUBJECTS}/${id}`, data),
};
