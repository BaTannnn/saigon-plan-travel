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
const ACCESS_TOKEN_EXPIRES_AT_KEY =
  "saigonplantravel.accessTokenExpiresAt";

type StoredAuthSession = {
  accessToken: string;
  expiresAt: number;
};

type AuthStatus = "loading" | "authenticated" | "guest";

type AuthState = {
  status: AuthStatus;
  user: AuthUser | null;
  accessToken: string | null;
  expiresAt: number | null;
};

export type RunAuthenticated = <T>(
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

function removeAuthSession() {
  try {
    window.sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    window.sessionStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
  } catch {
    // In-memory state is still cleared when browser storage is unavailable.
  }
}

function storeAuthSession(session: StoredAuthSession) {
  try {
    window.sessionStorage.setItem(ACCESS_TOKEN_KEY, session.accessToken);
    window.sessionStorage.setItem(
      ACCESS_TOKEN_EXPIRES_AT_KEY,
      String(session.expiresAt),
    );
  } catch (error) {
    removeAuthSession();
    throw error;
  }
}

function readAuthSession(): StoredAuthSession | null {
  try {
    const accessToken = window.sessionStorage.getItem(ACCESS_TOKEN_KEY);
    const storedExpiresAt = window.sessionStorage.getItem(
      ACCESS_TOKEN_EXPIRES_AT_KEY,
    );
    const expiresAt = storedExpiresAt === null ? NaN : Number(storedExpiresAt);

    if (!accessToken || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      removeAuthSession();
      return null;
    }

    return { accessToken, expiresAt };
  } catch {
    removeAuthSession();
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>(initialState);

  const logout = useCallback(() => {
    removeAuthSession();
    setState({
      status: "guest",
      user: null,
      accessToken: null,
      expiresAt: null,
    });
  }, []);

  useEffect(() => {
    let cancelled = false;
    const storedSession = readAuthSession();

    if (!storedSession) {
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

    getCurrentUser(storedSession.accessToken)
      .then((user) => {
        if (cancelled) return;

        setState({
          status: "authenticated",
          user,
          accessToken: storedSession.accessToken,
          expiresAt: storedSession.expiresAt,
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
    const expiresAt = Date.now() + response.expiresInSeconds * 1000;

    storeAuthSession({
      accessToken: response.accessToken,
      expiresAt,
    });

    setState({
      status: "authenticated",
      user: {
        publicId: response.publicId,
        email: response.email,
        displayName: response.displayName,
        role: response.role,
      },
      accessToken: response.accessToken,
      expiresAt,
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
