import type { Metadata } from "next";
import { AuthCard } from "@/features/auth/components/auth-card";
import { LoginForm } from "@/features/auth/components/login-form";

export const metadata: Metadata = {
  title: "Đăng nhập",
};

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ registered?: string | string[] }>;
}) {
  const registered = (await searchParams).registered === "1";

  return (
    <AuthCard
      title="Đăng nhập"
      alternateText="Chưa có tài khoản?"
      alternateHref="/register"
      alternateLabel="Đăng ký"
    >
      <LoginForm registered={registered} />
    </AuthCard>
  );
}
