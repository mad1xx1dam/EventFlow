import axiosInstance from "./axios";
import type {
  AuthResponse,
  CurrentUserResponse,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  ResendVerificationEmailRequest,
  VerifyEmailResponse,
} from "../types/auth";

export const authApi = {
  async register(payload: RegisterRequest): Promise<RegisterResponse> {
    const { data } = await axiosInstance.post<RegisterResponse>("/auth/register", payload);
    return data;
  },

  async verifyEmail(token: string): Promise<VerifyEmailResponse> {
    const { data } = await axiosInstance.get<VerifyEmailResponse>("/auth/verify-email", {
      params: { token },
    });
    return data;
  },

  async login(payload: LoginRequest): Promise<AuthResponse> {
    const { data } = await axiosInstance.post<AuthResponse>("/auth/login", payload);
    return data;
  },

  async getCurrentUser(): Promise<CurrentUserResponse> {
    const { data } = await axiosInstance.get<CurrentUserResponse>("/auth/me");
    return data;
  },

  async resendVerificationEmail(
    payload: ResendVerificationEmailRequest
  ): Promise<RegisterResponse> {
    const { data } = await axiosInstance.post<RegisterResponse>(
      "/auth/resend-verification",
      payload
    );
    return data;
  },
};

export default authApi;