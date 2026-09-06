import { requestJson } from "@/lib/api/api-client";
import type {
  CurrentUserResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from "@/types/auth";

function getAuthPath(path: string) {
  return `/api/v1/auth${path}`;
}

export function register(request: RegisterRequest) {
  return requestJson<RegisterResponse>(getAuthPath("/register"), {
    method: "POST",
    body: request,
    cache: "no-store",
  });
}

export function login(request: LoginRequest) {
  return requestJson<LoginResponse>(getAuthPath("/login"), {
    method: "POST",
    body: request,
    cache: "no-store",
  });
}

export function getCurrentUser(token: string) {
  return requestJson<CurrentUserResponse>(getAuthPath("/me"), {
    method: "GET",
    token,
    cache: "no-store",
  });
}
