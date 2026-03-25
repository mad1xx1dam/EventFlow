import { createContext } from "react";
import type { CurrentUserResponse, LoginRequest } from "../types/auth";

export interface AuthContextValue {
  user: CurrentUserResponse | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (payload: LoginRequest) => Promise<void>;
  logout: () => void;
  refreshCurrentUser: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);