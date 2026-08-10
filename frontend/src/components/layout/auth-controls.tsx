"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/features/auth/auth-provider";

export function AuthControls() {
  const router = useRouter();
  const { status, user, logout } = useAuth();

  if (status === "loading") {
    return (
      <div
        className="flex items-center justify-self-end gap-2"
        aria-label="Đang kiểm tra phiên đăng nhập"
        role="status"
      >
        <Skeleton className="size-[42px] rounded-full max-md:size-[38px]" />
        <Skeleton className="h-4 w-20 max-md:hidden" />
      </div>
    );
  }

  if (status === "guest" || !user) {
    return (
      <div className="flex items-center justify-self-end gap-2">
        <Button asChild className="max-md:px-2" variant="ghost" size="sm">
          <Link href="/login">Đăng nhập</Link>
        </Button>
        <Button asChild className="max-md:hidden" size="sm">
          <Link href="/register">Đăng ký</Link>
        </Button>
      </div>
    );
  }

  const initial = user.displayName.trim().charAt(0).toLocaleUpperCase("vi") || "U";

  function handleLogout() {
    logout();
    router.push("/places");
    router.refresh();
  }

  return (
    <div
      className="flex items-center justify-self-end gap-2 font-bold"
      aria-label={`Người dùng ${user.displayName}`}
    >
      <Avatar
        className="size-[42px] border border-border bg-[linear-gradient(145deg,var(--primary-soft),var(--surface))] text-primary-strong max-md:size-[38px]"
        size="lg"
        aria-hidden="true"
      >
        <AvatarFallback className="bg-transparent font-extrabold text-primary-strong">
          {initial}
        </AvatarFallback>
      </Avatar>
      <span className="max-w-32 truncate max-md:hidden">{user.displayName}</span>
      <Button
        className="px-2.5 text-xs max-md:px-2"
        type="button"
        variant="ghost"
        size="sm"
        onClick={handleLogout}
      >
        <span className="max-md:hidden">Đăng xuất</span>
        <span className="hidden max-md:inline">Thoát</span>
      </Button>
    </div>
  );
}
