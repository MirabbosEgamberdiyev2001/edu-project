import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type {
  TopicDto,
  TopicTreeDto,
  CreateTopicRequest,
  UpdateTopicRequest,
  MoveTopicRequest,
  ReorderTopicsRequest,
} from '@/types/topic';

export const topicApi = {
  getTopicTree: (subjectId: string, gradeLevel: number, signal?: AbortSignal) =>
    api.get<ApiResponse<TopicTreeDto[]>>(`/subjects/${subjectId}/topics`, { params: { gradeLevel }, signal }),

  createTopic: (subjectId: string, data: CreateTopicRequest) =>
    api.post<ApiResponse<TopicDto>>(`/subjects/${subjectId}/topics`, data),

  updateTopic: (id: string, data: UpdateTopicRequest) =>
    api.put<ApiResponse<TopicDto>>(`/topics/${id}`, data),

  deleteTopic: (id: string) =>
    api.delete<ApiResponse<void>>(`/topics/${id}`),

  reorderTopics: (data: ReorderTopicsRequest) =>
    api.put<ApiResponse<void>>('/topics/reorder', data),

  moveTopic: (id: string, data: MoveTopicRequest) =>
    api.post<ApiResponse<TopicDto>>(`/topics/${id}/move`, data),

  createBulk: (subjectId: string, data: { items: CreateTopicRequest[]; skipDuplicates?: boolean }) =>
    api.post<ApiResponse<{ created: number; skipped: number; errors: string[] }>>(`/subjects/${subjectId}/topics/bulk`, data),

  patchTopic: (id: string, data: UpdateTopicRequest) =>
    api.patch<ApiResponse<TopicDto>>(`/topics/${id}`, data),
};
