import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type { UserDto } from '@/types/user';
import type { UpdateProfileRequest } from '@/types/auth';

const AUTH = '/auth';

export const userApi = {
  getCurrentUser: () =>
    api.get<ApiResponse<UserDto>>(`${AUTH}/me`),

  updateProfile: (data: UpdateProfileRequest) =>
    api.put<ApiResponse<UserDto>>(`${AUTH}/me`, data),
};
