export interface InAppNotificationDto {
  id: string;
  type: string;
  title: string;
  body: string | null;
  referenceId: string | null;
  referenceType: string | null;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationListResponse {
  content: InAppNotificationDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
