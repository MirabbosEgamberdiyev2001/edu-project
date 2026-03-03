import api from '@/lib/axios';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type { HomeworkDto, HomeworkSubmissionDto, CreateHomeworkRequest, GradeSubmissionRequest } from '@/types/homework';

const BASE = '/homeworks';

export const homeworkApi = {
  // Teacher
  create: (data: CreateHomeworkRequest) =>
    api.post<ApiResponse<HomeworkDto>>(BASE, data),

  getTeacherHomeworks: (page = 0, size = 20) =>
    api.get<ApiResponse<PagedResponse<HomeworkDto>>>(`${BASE}/teacher`, { params: { page, size } }),

  getSubmissions: (homeworkId: string, page = 0, size = 50) =>
    api.get<ApiResponse<PagedResponse<HomeworkSubmissionDto>>>(
      `${BASE}/${homeworkId}/submissions`, { params: { page, size } }),

  gradeSubmission: (submissionId: string, data: GradeSubmissionRequest) =>
    api.patch<ApiResponse<HomeworkSubmissionDto>>(
      `${BASE}/submissions/${submissionId}/grade`, data),

  exportSubmissions: (homeworkId: string) =>
    api.get(`${BASE}/${homeworkId}/submissions/export`, { responseType: 'blob' }),

  // Student
  getStudentHomeworks: (groupId?: string, page = 0, size = 20) =>
    api.get<ApiResponse<PagedResponse<HomeworkDto>>>(`${BASE}/student`, {
      params: { ...(groupId ? { groupId } : {}), page, size },
    }),

  submitHomework: (homeworkId: string, textAnswer?: string, file?: File) => {
    const form = new FormData();
    if (textAnswer) form.append('textAnswer', textAnswer);
    if (file) form.append('file', file);
    return api.post<ApiResponse<HomeworkSubmissionDto>>(
      `${BASE}/${homeworkId}/submit`, form);
  },
};
