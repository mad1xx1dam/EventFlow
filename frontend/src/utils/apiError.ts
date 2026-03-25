import type { AxiosError } from "axios";

export interface BackendErrorResponse {
  message?: string;
  errors?: Record<string, string>;
}

export const getApiErrorData = (
  error: unknown
): BackendErrorResponse | null => {
  const axiosError = error as AxiosError<BackendErrorResponse>;
  return axiosError.response?.data ?? null;
};

export const getApiErrorMessage = (
  error: unknown,
  fallbackMessage: string
): string => {
  const data = getApiErrorData(error);

  if (data?.message && typeof data.message === "string" && data.message.trim()) {
    return data.message;
  }

  return fallbackMessage;
};

export const getApiErrorStatus = (error: unknown): number | null => {
  const axiosError = error as AxiosError<BackendErrorResponse>;
  return typeof axiosError.response?.status === "number"
    ? axiosError.response.status
    : null;
};