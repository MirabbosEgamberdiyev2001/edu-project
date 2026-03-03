import api from '@/lib/axios';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type { InAppNotificationDto } from '@/types/notification';

const BASE = '/notifications';

export const notificationApi = {
  getAll: (page = 0, size = 20) =>
    api.get<ApiResponse<PagedResponse<InAppNotificationDto>>>(BASE, { params: { page, size } }),

  getUnreadCount: () =>
    api.get<ApiResponse<{ count: number }>>(`${BASE}/unread-count`),

  markAllRead: () =>
    api.patch<ApiResponse<void>>(`${BASE}/mark-all-read`),

  markRead: (id: string) =>
    api.patch<ApiResponse<void>>(`${BASE}/${id}/mark-read`),
};
