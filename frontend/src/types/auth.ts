export type UserRole = "ADMIN" | "USER";

export type AuthUser = {
  publicId: string;
  email: string;
  displayName: string;
  role: UserRole;
};

export type RegisterRequest = {
  email: string;
  password: string;
  displayName: string;
};

export type RegisterResponse = AuthUser;

export type LoginRequest = {
  email: string;
  password: string;
};

export type LoginResponse = AuthUser & {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
};

export type CurrentUserResponse = AuthUser;
