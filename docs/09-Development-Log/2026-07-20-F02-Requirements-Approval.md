# 2026-07-20 — FEAT-002 Requirements Approval

## Mục tiêu

Khóa phạm vi và contract cho Place Detail, Category và Opening Hours trước khi
viết production code.

## Feature liên quan

- `FEAT-001 — Place Catalog API MVP`: `done`.
- `FEAT-002 — Place Detail, Category & Opening Hours MVP`: được `approved`
  trong phiên này; sau implementation cùng ngày, trạng thái hiện tại là `done`.

## Việc đã làm

- Đọc root/backend `AGENTS.md`, `PROJECT_CONTEXT.md`, source backend, migrations,
  tests, database note, API note và feature specs liên quan.
- Xác nhận V3–V5 đã sở hữu schema Category/OpeningHour và không được sửa.
- Khóa hai public endpoints:
  - `GET /api/v1/places/{slug}`;
  - `GET /api/v1/categories`.
- Khóa detail payload 15 field, nested `[]`, ordering và category catalog không
  pagination.
- Khóa ISO day, `HH:mm` và open/closed/unknown semantics.
- Khóa Spring `ProblemDetail` với `code=PLACE_NOT_FOUND` cho missing, malformed
  và inactive slug.
- Khóa forward constraints, demo fixtures, automated test matrix, tối đa 3
  queries/detail và mục tiêu latency dưới 500 ms sau warm-up.

## Quyết định database

- V1–V6 là baseline bất biến.
- Candidate đầu tiên là V7 nhưng phải recheck trước implementation.
- Forward migration phải bổ sung:
  - category slug regex `^[a-z0-9]+(?:-[a-z0-9]+)*$`;
  - `closed=true ⇒ open_time IS NULL AND close_time IS NULL`.
- Seed metadata dùng migration riêng sau constraint migration, tham chiếu place
  bằng slug và ghi rõ `DEMO DATA — NOT VERIFIED`.

## Dữ liệu demo đã duyệt

- 5 category: `Văn hóa`, `Lịch sử`, `Nghệ thuật`, `Khoa học`, `Ngoài trời`.
- Mỗi place demo có 1–2 category.
- 5 active places có lịch tuần; fixture phải chứa đủ open, closed và unknown.
- Có ít nhất một place đủ 7 ngày; ngày thiếu row biểu diễn unknown.

## Kiểm thử bắt buộc

- Mapper/service unit tests.
- MockMvc contract/error tests.
- PostgreSQL/pgvector Testcontainers migration và constraint tests.
- Automated query-count test: tối đa 3 queries, không N+1.
- FEAT-001 regression tests.
- `./mvnw test`, normal startup và runtime smoke tests.

## Files thay đổi

- `docs/00-Dashboard/PROJECT_CONTEXT.md`
- `docs/01-Requirements/Domain-Glossary.md`
- `docs/01-Requirements/Features/FEAT-001-place-catalog-api-mvp.md`
- `docs/01-Requirements/Features/FEAT-002-place-detail-category-opening-hours.md`
- `docs/03-Database/Place-Database-Design-v1.md`
- `docs/04-API/Place-API-v1.md`
- `docs/09-Development-Log/2026-07-16-F01-Place-Catalog-API-MVP.md`
- File development log này.

## Production changes

Không có production code hoặc migration nào được tạo/sửa trong phiên approval.

## Việc tiếp theo

File-by-file implementation plan đã được owner phê duyệt ngày 2026-07-20.

1. Bắt đầu phiên implementation riêng và chuyển FEAT-002 sang `in-progress`.
2. Recheck migration version; V7/V8 chỉ là candidate cho đến khi preflight xong.
3. Triển khai theo các bước reviewable và verification gates đã duyệt.

Phiên requirements kết thúc mà không tạo/sửa production code hoặc migration.

## Bắt đầu implementation

Owner yêu cầu tiếp tục sau khi duyệt plan. Preflight xác nhận PostgreSQL healthy,
Flyway V1–V6 đều `success=true`, 6 place rows/5 active, pgvector 0.8.5 và V7/V8
chưa có trong source. FEAT-002 chuyển sang `in-progress` ngày 2026-07-20.

## Báo cáo khóa luận

- Chương 3: mô hình Place–Category–OpeningHour, API contract và business rules.
- Chương 3: Flyway forward migration và Entity–DTO boundary.
- Chương 4: PostgreSQL constraints, query count, latency và regression evidence.

## Hoàn tất implementation

FEAT-002 đạt `done` ngày 2026-07-20 sau Flyway V7–V8, 28 automated tests,
`clean verify`, live migration và runtime smoke test. Chi tiết kỹ thuật và bằng
chứng nằm tại `2026-07-20-F02-Place-Detail-Implementation.md`.
