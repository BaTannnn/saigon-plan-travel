# 2026-07-21 — Place Explorer Frontend

## Phạm vi

Triển khai vertical slice frontend đầu tiên tiêu thụ FEAT-001–003:

- `/places`: catalog, search, filter, pagination, list/map selection;
- `/places/[slug]`: detail, categories, opening hours và vị trí;
- responsive desktop/mobile theo hướng Mint Map;
- không triển khai FEAT-004–009.

## Quyết định

- Dùng contract source thực tế: FEAT-003 mở rộng `GET /api/v1/places`, không có
  `GET /api/v1/places/search`.
- Fetch API trong Next Server Components vì backend chưa cấu hình CORS.
- URL search parameters là nguồn trạng thái filter.
- Quận được suy ra từ active catalog gọi với `size=100`; không tạo metadata API giả.
- Leaflet được dynamic import với `ssr: false` trong Client Component.
- Không có `imageUrl`, vì vậy detail dùng category gradient illustration.

## Verification

- `npm run lint`: pass, 0 error/warning.
- `npm run build`: pass; `/places` và `/places/[slug]` là dynamic routes.
- Runtime với Spring Boot/PostgreSQL thật:
  - `GET http://localhost:3000/places`: HTTP 200, đủ 5 demo active places;
  - `GET http://localhost:3000/places/demo-art-space`: HTTP 200, có detail và giờ mở cửa.
- Repository chưa có frontend test runner/script; không cài thêm framework.
- Environment không có browser automation/screenshot tool, nên chưa tạo
  screenshot đúng 1440×900 và 390×844; responsive đã được kiểm tra qua CSS
  breakpoint và production type/build checks.

## Dependency

Thêm `leaflet`, `react-leaflet` và `@types/leaflet`. `npm install` báo 2 moderate
vulnerabilities trong dependency tree; không chạy `npm audit fix --force` vì có
thể tạo breaking changes ngoài phạm vi.

## Refactor shadcn/ui — 2026-07-21

- Khởi tạo shadcn/ui 4.13.1 trong app hiện tại với Radix/Nova, Tailwind v4,
  CSS variables và alias `@/*`; không scaffold app mới và không nâng Next,
  React hay Tailwind.
- Thêm đúng các primitive đang dùng: Button, Input, Label, Select, Sheet, Alert,
  Skeleton, Avatar, Card, Badge và Pagination.
- Ánh xạ toàn bộ semantic theme về Mint Map; không thêm dark mode hoặc font
  ngoài. Thêm Button `accent` cho CTA nhưng giữ action ở trạng thái disabled.
- Thay mobile filter dialog tự viết bằng Sheet; Select giữ GET form và URL là
  source of truth. Category chip và pagination vẫn dùng Next Link.
- Giữ nguyên API client, DTO, Server Component fetching, Leaflet dynamic import,
  marker/card selection và responsive list/map layout.
- Loại bỏ CSS không còn tham chiếu của primary/secondary button, native select,
  close icon, custom dialog backdrop/panel và generic skeleton class.

### Verification sau refactor

- `npm run lint`: pass.
- `npx tsc --noEmit`: pass.
- `npm run build`: pass; `/places` và `/places/[slug]` vẫn dynamic.
- Production runtime với PostgreSQL/Spring Boot thật trả HTTP 200 cho catalog,
  detail, empty result và URL kết hợp filter.
- Khi backend dừng, `/places` hiển thị shadcn Alert và khẳng định không dùng dữ
  liệu giả.
- Không có formatter/test script trong `package.json`; không cài framework mới.
- Môi trường không có browser automation/Chromium, nên chưa có screenshot hoặc
  xác minh tương tác thật ở 1440×900 và 390×844.
