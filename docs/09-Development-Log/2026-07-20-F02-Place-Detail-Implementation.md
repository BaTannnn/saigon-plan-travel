# 2026-07-20 — FEAT-002 Place Detail Implementation

## Kết quả

FEAT-002 đạt `done`. Place Module cung cấp hai public endpoints:

```http
GET /api/v1/places/{slug}
GET /api/v1/categories
```

Feature giữ nguyên contract catalog FEAT-001 và không mở rộng sang search,
filter hoặc generated recommendation.

## Thay đổi kỹ thuật

- V7 thêm check constraint cho category slug và trạng thái giờ đóng/mở.
- V8 seed 5 categories, 8 place-category relations và 33 opening-hour rows.
- Thêm `Category` và `OpeningHour` entities; mở rộng `Place` bằng LAZY relations.
- Thêm active exact-slug lookup và category ordering repository query.
- Thêm record DTO cho detail, category và opening-hour responses.
- Mở rộng mapper với nested sorting; time serialize chính xác `HH:mm`.
- Thêm read-only services, `CategoryController` và place detail route.
- Mở rộng common Spring `ProblemDetail` handler bằng `PLACE_NOT_FOUND`.

## Database evidence

Live PostgreSQL sau smoke run:

| Dữ liệu | Số lượng |
| --- | ---: |
| Places | 6 |
| Active places | 5 |
| Categories | 5 |
| Place-category relations | 8 |
| Opening-hour rows | 33 |

`flyway_schema_history` xác nhận V1–V8 đều `success=true`. Startup kế tiếp báo
schema version 8 và không cần migration thêm. Hibernate `ddl-auto=validate`
khởi tạo EntityManagerFactory thành công.

## Query behavior

PostgreSQL integration test bật Hibernate statistics và xác nhận detail của
place có 7 opening-hour rows dùng đúng 3 SQL queries:

1. active place theo exact slug;
2. categories của place;
3. opening hours của place.

Số query không tăng theo số phần tử nested, vì vậy không có N+1 trong use case
detail MVP.

## Automated verification

Các lệnh đã chạy từ `backend/`:

```bash
./mvnw -DskipTests compile
./mvnw -DskipTests test-compile
./mvnw test
./mvnw clean verify
```

Kết quả cuối: 28 tests, 0 failures, 0 errors, 0 skipped; Spring Boot JAR được
repackage thành công. Test suite bao phủ mapper, services, MockMvc contracts,
PostgreSQL migrations/constraints, exact active-slug lookup, ordering, query
count và regression FEAT-001.

## Runtime smoke test

Backend được chạy tạm trên cổng 8081 vì cổng 8080 đã có tiến trình khác. Không
dừng hoặc thay đổi tiến trình trên 8080. Kết quả:

| Request | Kết quả |
| --- | --- |
| `GET /actuator/health` | 200 |
| `GET /api/v1/places/demo-art-space` | 200, đúng 15 fields, `HH:mm`, sorted nested arrays |
| inactive place slug | 404 `PLACE_NOT_FOUND` |
| missing slug | 404 `PLACE_NOT_FOUND` |
| case-mismatch slug | 404 `PLACE_NOT_FOUND` |
| `GET /api/v1/categories` | 200, root array, `name ASC, id ASC` |
| `GET /api/v1/places?page=0&size=20` | 200, 5 active places, contract FEAT-001 không đổi |

Detail active mất khoảng 31 ms trong smoke run sau startup. Đây chỉ là bằng
chứng cục bộ dưới mục tiêu 500 ms, không phải benchmark production.

## Vấn đề gặp và xử lý

- Sandbox ban đầu không truy cập được Docker socket; repository integration test
  được chạy lại với quyền đã phê duyệt và thành công.
- Runtime đầu tiên dùng cổng 8080 thất bại do port đang được sử dụng. Smoke test
  được chuyển sang 8081 và tiến trình tạm được graceful shutdown sau khi xong.
- Một câu truy vấn kiểm đếm thủ công lỗi do quoting nhãn SQL; truy vấn read-only
  được chạy lại không có nhãn và trả đúng `6/5/5/8/33`.

## Liên kết tài liệu

- Spec: `docs/01-Requirements/Features/FEAT-002-place-detail-category-opening-hours.md`
- Database: `docs/03-Database/Place-Database-Design-v1.md`
- API: `docs/04-API/Place-API-v1.md`
- Context: `docs/00-Dashboard/PROJECT_CONTEXT.md`
- Requirements approval: `docs/09-Development-Log/2026-07-20-F02-Requirements-Approval.md`

## Báo cáo khóa luận

- Chương 3: mô hình Place–Category–OpeningHour, Flyway V7–V8, package-by-feature,
  Entity–DTO boundary và API contracts.
- Chương 4: 28-test evidence, PostgreSQL constraint tests, đúng 3 detail queries,
  runtime response time và regression FEAT-001.
- Hạn chế MVP: dữ liệu là demo chưa xác minh; chưa có lịch ngoại lệ, nhiều
  interval/ngày, search/filter hoặc generated recommendation.

## Bước tiếp theo

Khởi động requirements session riêng cho FEAT-003 Place Search, Filter &
Pagination MVP; chưa viết code trước khi spec và file-by-file plan được duyệt.
