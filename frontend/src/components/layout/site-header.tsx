import Link from "next/link";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { PinIcon } from "@/components/ui/icons";

export function SiteHeader() {
  return (
    <header className="site-header">
      <Link className="brand" href="/places" aria-label="SaigonPlanTravel - Khám phá">
        <span className="brand-mark">
          <PinIcon />
        </span>
        <span>SaigonPlanTravel</span>
      </Link>

      <nav className="desktop-nav" aria-label="Điều hướng chính">
        <Link className="nav-link nav-link-active" href="/places">
          Khám phá
        </Link>
        <span className="nav-link nav-link-disabled" aria-disabled="true">
          Lịch trình
          <Badge className="coming-soon-pill" variant="secondary">
            Sắp ra mắt
          </Badge>
        </span>
      </nav>

      <div className="user-chip" aria-label="Người dùng Tân">
        <Avatar className="user-avatar" size="lg" aria-hidden="true">
          <AvatarFallback className="user-avatar-fallback">T</AvatarFallback>
        </Avatar>
        <span className="user-name">Tân</span>
      </div>
    </header>
  );
}
