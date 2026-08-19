"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PasswordInput } from "@/features/auth/components/password-input";
import { ApiError } from "@/lib/api/api-client";
import { register } from "@/lib/api/auth-api";

function registerErrorMessage(error: unknown) {
  if (error instanceof ApiError) {
    if (error.status === 0) {
      return "Không thể kết nối đến máy chủ. Hãy kiểm tra backend và thử lại.";
    }
    if (error.status === 409) {
      return "Email này đã được sử dụng.";
    }
    return error.problem?.detail ?? "Không thể tạo tài khoản. Hãy thử lại.";
  }

  return "Không thể tạo tài khoản. Hãy thử lại.";
}

export function RegisterForm() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [confirmPasswordError, setConfirmPasswordError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);

    const formData = new FormData(event.currentTarget);
    const password = String(formData.get("password") ?? "");
    const confirmPassword = String(formData.get("confirmPassword") ?? "");

    if (password !== confirmPassword) {
      setConfirmPasswordError("Mật khẩu xác nhận chưa khớp.");
      return;
    }

    setConfirmPasswordError(null);
    setPending(true);

    try {
      await register({
        displayName: String(formData.get("displayName") ?? ""),
        email: String(formData.get("email") ?? ""),
        password,
      });
      router.push("/login?registered=1");
    } catch (error) {
      setErrorMessage(registerErrorMessage(error));
    } finally {
      setPending(false);
    }
  }

  return (
    <form className="grid gap-5" onSubmit={handleSubmit}>
      {errorMessage ? (
        <Alert variant="destructive" role="alert">
          <AlertTitle>Đăng ký chưa thành công</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      ) : null}

      <div className="grid gap-2">
        <Label htmlFor="register-display-name">Tên hiển thị</Label>
        <Input
          id="register-display-name"
          name="displayName"
          type="text"
          autoComplete="name"
          maxLength={100}
          required
          disabled={pending}
          className="h-11"
        />
      </div>

      <div className="grid gap-2">
        <Label htmlFor="register-email">Email</Label>
        <Input
          id="register-email"
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
        <Label htmlFor="register-password">Mật khẩu</Label>
        <PasswordInput
          id="register-password"
          name="password"
          autoComplete="new-password"
          minLength={8}
          maxLength={64}
          required
          aria-describedby="register-password-hint"
          disabled={pending}
        />
        <p id="register-password-hint" className="m-0 text-xs text-text-secondary">
          Sử dụng từ 8 đến 64 ký tự.
        </p>
      </div>

      <div className="grid gap-2">
        <Label htmlFor="register-confirm-password">Xác nhận mật khẩu</Label>
        <PasswordInput
          id="register-confirm-password"
          name="confirmPassword"
          autoComplete="new-password"
          minLength={8}
          maxLength={64}
          required
          aria-describedby={
            confirmPasswordError ? "register-confirm-password-error" : undefined
          }
          aria-invalid={Boolean(confirmPasswordError)}
          disabled={pending}
          onChange={() => {
            if (confirmPasswordError) setConfirmPasswordError(null);
          }}
        />
        {confirmPasswordError ? (
          <p
            id="register-confirm-password-error"
            className="m-0 text-xs text-destructive"
          >
            {confirmPasswordError}
          </p>
        ) : null}
      </div>

      <Button className="mt-1 w-full" type="submit" disabled={pending}>
        {pending ? "Đang tạo tài khoản…" : "Tạo tài khoản"}
      </Button>
    </form>
  );
}
