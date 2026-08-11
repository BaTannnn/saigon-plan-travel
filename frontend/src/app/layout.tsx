import type { Metadata } from "next";
import { SiteHeader } from "@/components/layout/site-header";
import { AuthProvider } from "@/features/auth/auth-provider";
import "leaflet/dist/leaflet.css";
import "./globals.css";
import { Geist } from "next/font/google";

const geist = Geist({
  subsets: ["latin"],
  variable: "--font-geist-sans",
});

export const metadata: Metadata = {
  title: {
    default: "SaigonPlanTravel",
    template: "%s | SaigonPlanTravel",
  },
  description:
    "Khám phá địa điểm và lưu sở thích chuyến đi tại Thành phố Hồ Chí Minh.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" className={geist.variable}>
      <body>
        <AuthProvider>
          <SiteHeader />
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
