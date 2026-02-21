import api from '@/lib/axios';
import type { ApiResponse } from '@/types/api';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  OtpVerifyRequest,
  OtpResponse,
  OtpVerifyResponse,
  RefreshTokenRequest,
  ForgotPasswordRequest,
  PasswordResetRequest,
  ChangePasswordRequest,
  GoogleAuthRequest,
  TelegramAuthRequest,
  SessionDto,
} from '@/types/auth';

const AUTH = '/auth';

export const authApi = {
  register: (data: RegisterRequest) =>
    api.post<ApiResponse<OtpResponse>>(`${AUTH}/register`, data),

  login: (data: LoginRequest) =>
    api.post<ApiResponse<LoginResponse>>(`${AUTH}/login`, data),

  verifyOtp: (data: OtpVerifyRequest) =>
    api.post<ApiResponse<OtpVerifyResponse>>(`${AUTH}/otp/verify`, data),

  refresh: (data: RefreshTokenRequest) =>
    api.post<ApiResponse<LoginResponse>>(`${AUTH}/refresh`, data),

  logout: (data?: RefreshTokenRequest) =>
    api.post<ApiResponse<void>>(`${AUTH}/logout`, data || {}),

  forgotPassword: (data: ForgotPasswordRequest) =>
    api.post<ApiResponse<OtpResponse>>(`${AUTH}/password/forgot`, data),

  resetPassword: (data: PasswordResetRequest) =>
    api.post<ApiResponse<void>>(`${AUTH}/password/reset`, data),

  changePassword: (data: ChangePasswordRequest) =>
    api.post<ApiResponse<void>>(`${AUTH}/password/change`, data),

  // OAuth
  googleAuth: (data: GoogleAuthRequest) =>
    api.post<ApiResponse<LoginResponse>>(`${AUTH}/google`, data),

  telegramAuth: (data: TelegramAuthRequest) =>
    api.post<ApiResponse<LoginResponse>>(`${AUTH}/telegram`, data),

  // Sessions
  getSessions: () =>
    api.get<ApiResponse<SessionDto[]>>(`${AUTH}/sessions`),

  revokeSession: (sessionId: string) =>
    api.delete<ApiResponse<void>>(`${AUTH}/sessions/${sessionId}`),

  revokeAllSessions: () =>
    api.delete<ApiResponse<void>>(`${AUTH}/sessions`),
};
