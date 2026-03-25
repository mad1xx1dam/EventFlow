export type UserRole = "ADMIN" | "USER";

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface ResendVerificationEmailRequest {
  email: string;
}

export interface RegisterResponse {
  message: string;
}

export interface VerifyEmailResponse {
  message: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  userId: number;
  email: string;
  role: UserRole;
}

export interface CurrentUserResponse {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  emailVerified: boolean;
  enabled: boolean;
}