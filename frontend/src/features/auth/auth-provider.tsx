"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { ApiError } from "@/lib/api/api-client";
import { getCurrentUser, login as requestLogin } from "@/lib/api/auth-api";
import type { AuthUser, LoginRequest } from "@/types/auth";

const ACCESS_TOKEN_KEY = "saigonplantravel.accessToken";

type AuthStatus = "loading" | "authenticated" | "guest";

type AuthState = {
  status: AuthStatus;
  user: AuthUser | null;
  accessToken: string | null;
  expiresAt: number | null;
};

type RunAuthenticated = <T>(
  request: (accessToken: string) => Promise<T>,
) => Promise<T>;

type AuthContextValue = AuthState & {
  login: (request: LoginRequest) => Promise<void>;
  logout: () => void;
  runAuthenticated: RunAuthenticated;
};

const initialState: AuthState = {
  status: "loading",
  user: null,
  accessToken: null,
  expiresAt: null,
};

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredToken() {
  try {
    return window.sessionStorage.getItem(ACCESS_TOKEN_KEY);
  } catch {
    return null;
  }
}

function storeToken(token: string) {
  window.sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
}

function removeStoredToken() {
  try {
    window.sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  } catch {
    // In-memory state is still cleared when browser storage is unavailable.
  }
}

function readTokenExpiration(token: string) {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;

    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(
      normalized.length + ((4 - (normalized.length % 4)) % 4),
      "=",
    );
    const claims = JSON.parse(window.atob(padded)) as { exp?: unknown };

    return typeof claims.exp === "number" ? claims.exp * 1000 : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>(initialState);

  const logout = useCallback(() => {
    removeStoredToken();
    setState({
      status: "guest",
      user: null,
      accessToken: null,
      expiresAt: null,
    });
  }, []);

  useEffect(() => {
    let cancelled = false;
    const storedToken = readStoredToken();

    if (!storedToken) {
      window.queueMicrotask(() => {
        if (cancelled) return;

        setState({
          status: "guest",
          user: null,
          accessToken: null,
          expiresAt: null,
        });
      });

      return () => {
        cancelled = true;
      };
    }

    getCurrentUser(storedToken)
      .then((user) => {
        if (cancelled) return;

        setState({
          status: "authenticated",
          user,
          accessToken: storedToken,
          expiresAt: readTokenExpiration(storedToken),
        });
      })
      .catch(() => {
        if (cancelled) return;
        logout();
      });

    return () => {
      cancelled = true;
    };
  }, [logout]);

  useEffect(() => {
    if (state.status !== "authenticated" || state.expiresAt === null) {
      return;
    }

    const timeout = window.setTimeout(
      logout,
      Math.max(0, state.expiresAt - Date.now()),
    );

    return () => window.clearTimeout(timeout);
  }, [logout, state.expiresAt, state.status]);

  const login = useCallback(async (request: LoginRequest) => {
    const response = await requestLogin(request);
    storeToken(response.accessToken);

    setState({
      status: "authenticated",
      user: {
        publicId: response.publicId,
        email: response.email,
        displayName: response.displayName,
        role: response.role,
      },
      accessToken: response.accessToken,
      expiresAt: Date.now() + response.expiresInSeconds * 1000,
    });
  }, []);

  const runAuthenticated = useCallback<RunAuthenticated>(
    async (request) => {
      if (!state.accessToken) {
        throw new Error("Authentication is required");
      }

      try {
        return await request(state.accessToken);
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          logout();
        }
        throw error;
      }
    },
    [logout, state.accessToken],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      login,
      logout,
      runAuthenticated,
    }),
    [login, logout, runAuthenticated, state],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
