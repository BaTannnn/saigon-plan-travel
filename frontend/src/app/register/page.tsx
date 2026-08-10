import type { Metadata } from "next";
import { AuthCard } from "@/features/auth/components/auth-card";
import { RegisterForm } from "@/features/auth/components/register-form";

export const metadata: Metadata = {
  title: "Đăng ký",
};

export default function RegisterPage() {
  return (
    <AuthCard
      eyebrow="Bắt đầu khám phá"
      title="Tạo tài khoản"
      description="Đăng ký để lưu địa điểm yêu thích và thông tin chuyến đi."
      alternateText="Đã có tài khoản?"
      alternateHref="/login"
      alternateLabel="Đăng nhập"
    >
      <RegisterForm />
    </AuthCard>
  );
}
