"use client";

import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

type PasswordInputProps = Omit<React.ComponentProps<typeof Input>, "type">;

export function PasswordInput({ className, ...props }: PasswordInputProps) {
  const [visible, setVisible] = useState(false);
  const label = visible ? "Ẩn mật khẩu" : "Hiển thị mật khẩu";

  return (
    <div className="relative">
      <Input
        {...props}
        className={cn("h-11 pr-11", className)}
        type={visible ? "text" : "password"}
      />
      <Button
        aria-label={label}
        aria-pressed={visible}
        className="absolute top-1/2 right-1 -translate-y-1/2 text-text-secondary hover:text-primary"
        disabled={props.disabled}
        size="icon-xs"
        type="button"
        variant="ghost"
        onClick={() => setVisible((current) => !current)}
      >
        {visible ? <EyeOff /> : <Eye />}
      </Button>
    </div>
  );
}
