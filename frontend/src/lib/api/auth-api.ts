import { requestJson } from "@/lib/api/api-client";
import type {
  CurrentUserResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from "@/types/auth";

const DEFAULT_BROWSER_BACKEND_URL = "http://localhost:8080";

function getBrowserBackendBaseUrl() {
  return (
    process.env.NEXT_PUBLIC_BACKEND_API_BASE_URL ??
    DEFAULT_BROWSER_BACKEND_URL
  ).replace(/\/$/, "");
}

function getAuthUrl(path: string) {
  return `${getBrowserBackendBaseUrl()}/api/v1/auth${path}`;
}

export function register(request: RegisterRequest) {
  return requestJson<RegisterResponse>(getAuthUrl("/register"), {
    method: "POST",
    body: request,
    cache: "no-store",
  });
}

export function login(request: LoginRequest) {
  return requestJson<LoginResponse>(getAuthUrl("/login"), {
    method: "POST",
    body: request,
    cache: "no-store",
  });
}

export function getCurrentUser(token: string) {
  return requestJson<CurrentUserResponse>(getAuthUrl("/me"), {
    token,
    cache: "no-store",
  });
}
