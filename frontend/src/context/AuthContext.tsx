import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from "react";
import { authApi } from "../api/authApi";
import { AuthContext, type AuthContextValue } from "./auth-context";
import type { CurrentUserResponse, LoginRequest } from "../types/auth";

const ACCESS_TOKEN_KEY = "accessToken";

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [user, setUser] = useState<CurrentUserResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const logout = useCallback(() => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    setUser(null);
  }, []);

  const refreshCurrentUser = useCallback(async () => {
    const currentUser = await authApi.getCurrentUser();
    setUser(currentUser);
  }, []);

  const login = useCallback(async (payload: LoginRequest) => {
    const response = await authApi.login(payload);
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);

    const currentUser = await authApi.getCurrentUser();
    setUser(currentUser);
  }, []);

  useEffect(() => {
    const initializeAuth = async () => {
      const token = localStorage.getItem(ACCESS_TOKEN_KEY);

      if (!token) {
        setIsLoading(false);
        return;
      }

      try {
        const currentUser = await authApi.getCurrentUser();
        setUser(currentUser);
      } catch {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };

    void initializeAuth();
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      isAuthenticated: user !== null,
      login,
      logout,
      refreshCurrentUser,
    }),
    [user, isLoading, login, logout, refreshCurrentUser]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};