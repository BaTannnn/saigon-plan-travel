import type { Metadata } from "next";
import { SiteHeader } from "@/components/layout/site-header";
import { AuthProvider } from "@/features/auth/auth-provider";
import "leaflet/dist/leaflet.css";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "SaigonPlanTravel",
    template: "%s | SaigonPlanTravel",
  },
  description:
    "Khám phá địa điểm và lưu sở thích chuyến đi tại Thành phố Hồ Chí Minh.",
  icons: {
    icon: [
      {
        url: "/saigonplantravel-logo-icon.svg",
        type: "image/svg+xml",
      },
    ],
    shortcut: "/saigonplantravel-logo-icon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi">
      <body>
        <AuthProvider>
          <SiteHeader />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
