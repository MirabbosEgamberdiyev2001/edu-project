import { type Role, type UserDto } from './user';

export interface LoginRequest {
  email?: string;
  phone?: string;
  password: string;
}

export interface RegisterRequest {
  email?: string;
  phone?: string;
  password: string;
  firstName: string;
  lastName: string;
  role?: Role;
}

export interface OtpVerifyRequest {
  email?: string;
  phone?: string;
  code: string;
}

export interface ForgotPasswordRequest {
  email?: string;
  phone?: string;
}

export interface PasswordResetRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  avatarUrl?: string;
  locale?: string;
  timezone?: string;
  bio?: string;
  workplace?: string;
  subjectId?: string;
}

// Responses

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserDto;
}

export interface OtpResponse {
  message: string;
}

export interface OtpVerifyResponse {
  verified: boolean;
  resetToken?: string;
  registeredUser?: UserDto;
  accessToken?: string;
  refreshToken?: string;
  tokenType?: string;
  expiresIn?: number;
}

// OAuth

export interface GoogleAuthRequest {
  idToken?: string;
  accessToken?: string;
  /** STUDENT | TEACHER | PARENT — used only for brand-new users */
  role?: string;
}

export interface TelegramAuthRequest {
  id: number;
  firstName: string;
  lastName?: string;
  username?: string;
  photoUrl?: string;
  authDate: number;
  hash: string;
}

// Sessions

export interface SessionDto {
  id: string;
  jti: string;
  ipAddress: string;
  deviceInfo: string | null;
  createdAt: string;
  lastActivityAt: string | null;
  isCurrent: boolean;
}
