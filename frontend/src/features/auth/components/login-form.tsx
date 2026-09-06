"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/features/auth/auth-provider";
import { PasswordInput } from "@/features/auth/components/password-input";
import { ApiError } from "@/lib/api/api-client";

function loginErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 0) {
      return "Không thể kết nối đến máy chủ. Hãy kiểm tra backend và thử lại.";
    }
    if (error.status === 401) {
      return "Email hoặc mật khẩu không đúng.";
    }
    return error.problem?.detail ?? "Không thể đăng nhập. Hãy thử lại.";
  }

  return "Không thể đăng nhập. Hãy thử lại.";
}

export function LoginForm({ registered = false }: { registered?: boolean }) {
  const router = useRouter();
  const { login } = useAuth();
  const [pending, setPending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setErrorMessage(null);

    const formData = new FormData(event.currentTarget);

    try {
      await login({
        email: String(formData.get("email") ?? ""),
        password: String(formData.get("password") ?? ""),
      });
      router.replace("/");
      router.refresh();
    } catch (error) {
      setErrorMessage(loginErrorMessage(error));
    } finally {
      setPending(false);
    }
  }

  return (
    <form className="grid gap-5" onSubmit={handleSubmit}>
      {registered ? (
        <Alert className="border-primary/30 bg-primary-soft text-primary-strong">
          <AlertTitle>Đăng ký thành công</AlertTitle>
          <AlertDescription>
            Tài khoản đã được tạo. Hãy đăng nhập để bắt đầu phiên mới.
          </AlertDescription>
        </Alert>
      ) : null}

      {errorMessage ? (
        <Alert variant="destructive" role="alert">
          <AlertTitle>Đăng nhập chưa thành công</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      <div className="grid gap-2">
        <Label htmlFor="login-email">Email</Label>
        <Input
          id="login-email"
          name="email"
          type="email"
          autoComplete="email"
          maxLength={255}
          required
          disabled={pending}
          className="h-11"
        />
      </div>

      <div className="grid gap-2">
        <Label htmlFor="login-password">Mật khẩu</Label>
        <PasswordInput
          id="login-password"
          name="password"
          autoComplete="current-password"
          maxLength={64}
          required
          disabled={pending}
        />
      </div>

      <Button className="mt-1 w-full" type="submit" disabled={pending}>
        {pending ? "Đang đăng nhập…" : "Đăng nhập"}
      </Button>
    </form>
  );
}
