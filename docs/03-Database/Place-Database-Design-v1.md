# Place Database Design v1

## Trạng thái

FEAT-001, FEAT-002 và FEAT-003 đều đạt `done` ngày 2026-07-20. Live database
đang ở Flyway V9; Hibernate validation, PostgreSQL Testcontainers và runtime
smoke test đã thành công.

## Bảng đầu tiên

`places`

## Nguyên tắc

- Flyway migration-first.
- Hibernate `ddl-auto: validate`.
- V1–V9 là lịch sử migration đã áp dụng và bất biến; không sửa hoặc xóa.
- FEAT-002 không tạo lại schema V3–V5 và không sửa migration hiện có.
- V2 là schema `places` canonical.
- Tiền dùng `NUMERIC` trong PostgreSQL và `BigDecimal` trong Java.
- Seed chưa xác minh phải ghi rõ demo.

## Migration files và trạng thái live database

Trạng thái được kiểm tra ngày 2026-07-20:

| Version | File | Chức năng | Live database |
| --- | --- | --- | --- |
| V1 | `V1__enable_pgvector.sql` | Bật extension `vector` nếu chưa có. | Applied, `success = true` |
| V2 | `V2__create_places_table.sql` | Tạo bảng `places`, unique slug, các default và check constraint. | Applied, `success = true` |
| V3 | `V3__create_categories_table.sql` | Tạo bảng `categories` với unique name và slug. | Applied, `success = true` |
| V4 | `V4__create_place_categories_table.sql` | Tạo bảng nối many-to-many cùng foreign key cascade. | Applied, `success = true` |
| V5 | `V5__create_opening_hours_table.sql` | Tạo opening hours, unique place/day và time/day constraints. | Applied, `success = true` |
| V6 | `V6__seed_demo_places.sql` | Seed 5 place active và 1 place inactive có nhãn demo. | Applied, `success = true` |
| V7 | `V7__strengthen_category_and_opening_hour_constraints.sql` | Enforce category slug và trạng thái giờ đóng/mở. | Applied, `success = true` |
| V8 | `V8__seed_demo_place_metadata.sql` | Seed category, place-category và opening-hours demo. | Applied, `success = true` |
| V9 | `V9__enable_unaccent_and_place_search_index.sql` | Bật `unaccent` và tạo reverse category index. | Applied, `success = true` |

Live database xác nhận V1–V9 đều `success=true`, gồm 6 places/5 active,
5 categories, 8 place-category relations và 33 opening-hour rows. Automated
PostgreSQL Testcontainers test xác nhận cùng lịch sử, extensions `vector`/`unaccent`,
Hibernate validate, constraints và migrate lại không seed trùng.

Không sửa hoặc xóa V1–V6. Category và OpeningHour không được phát triển thêm
trong FEAT-001 dù schema tương ứng đã tồn tại.

## FEAT-002 — canonical schema

### `categories` từ V3

- `id BIGSERIAL PRIMARY KEY`.
- `name VARCHAR(100) NOT NULL UNIQUE`.
- `slug VARCHAR(120) NOT NULL UNIQUE`.
- `description TEXT` nullable.
- `created_at` và `updated_at` là `TIMESTAMPTZ NOT NULL` có default
  `CURRENT_TIMESTAMP`.

V7 bổ sung check constraint `chk_categories_slug_format`, enforce regex
`^[a-z0-9]+(-[a-z0-9]+)*$` cho category slug.

### `place_categories` từ V4

- Khóa chính kép `(place_id, category_id)`.
- `place_id` tham chiếu `places(id)` với `ON DELETE CASCADE`.
- `category_id` tham chiếu `categories(id)` với `ON DELETE CASCADE`.

### `opening_hours` từ V5

- `id BIGSERIAL PRIMARY KEY` và `place_id` tham chiếu `places(id)` với
  `ON DELETE CASCADE`.
- `day_of_week SMALLINT NOT NULL` trong `1..7`.
- `open_time` và `close_time` nullable; `closed BOOLEAN NOT NULL DEFAULT FALSE`.
- Unique `(place_id, day_of_week)`, nên schema hiện tại chỉ hỗ trợ tối đa
  một interval mỗi ngày.
- Constraint V5 chấp nhận `closed = TRUE` mà không bắt buộc
  `open_time`/`close_time` phải `NULL`; khi `closed = FALSE`, hai giờ phải non-null
  và `open_time < close_time`.

V7 giữ V5 bất biến và bổ sung `chk_opening_hours_state`:

- `closed=true` yêu cầu cả hai time là `NULL`;
- `closed=false` yêu cầu cả hai time khác `NULL` và `open_time < close_time`.

### Migration FEAT-002 đã áp dụng

```text
V7__strengthen_category_and_opening_hour_constraints.sql
V8__seed_demo_place_metadata.sql
```

V8 seed 5 category đã duyệt (`Văn hóa`, `Lịch sử`, `Nghệ thuật`, `Khoa học`,
`Ngoài trời`), 8 relation bằng slug và 33 opening-hours rows cho 5 active
places. Fixtures gồm open, closed và unknown; comment migration ghi rõ
`DEMO DATA - NOT VERIFIED`.

Chi tiết contract và test evidence nằm trong
`FEAT-002-place-detail-category-opening-hours.md` và development log FEAT-002;
feature hiện `done`.

## Seed dữ liệu FEAT-001

Migration đã áp dụng và xác minh ngày 2026-07-20:

```text
V6__seed_demo_places.sql
```

Quy tắc:

- Tạo 6 địa điểm mô phỏng.
- 5 bản ghi có `active = true`.
- 1 bản ghi có `active = false`.
- Tên bắt đầu bằng `Demo`.
- Mô tả ghi rõ dữ liệu minh họa, chưa được xác minh.
- Dữ liệu không được trình bày như thông tin địa điểm thực đã xác minh.

## Verification FEAT-001 ngày 2026-07-20

- Maven clean/verify: 15 tests, 0 failures, 0 errors, 0 skipped.
- PostgreSQL Testcontainers/pgvector: V1–V6, `vector`, Hibernate validate,
  constraints, 6 rows/5 active và migrate lại không trùng seed đều thành công.
- Runtime smoke test: list mặc định, page vượt giới hạn và invalid pagination
  đều đúng contract.

## Verification FEAT-002 ngày 2026-07-20

- `./mvnw test`: 28 tests, 0 failures, 0 errors, 0 skipped.
- `./mvnw clean verify`: build và Spring Boot repackage thành công với cùng 28 tests.
- PostgreSQL Testcontainers chạy V1–V8 trên database sạch; live database nâng từ
  V6 lên V8 và restart xác nhận schema up to date.
- Constraint tests bao phủ category slug, open/closed state, time ordering và
  uniqueness của place-category/place-day.
- Hibernate statistics xác nhận detail active dùng đúng 3 SQL queries, không
  tăng theo số category hoặc opening-hour rows.

## FEAT-003 — V9 đã áp dụng và xác minh

FEAT-003 không thêm business table, column hoặc Entity field. Migration sau V8 là:

```text
V9__enable_unaccent_and_place_search_index.sql
```

V9 chỉ thực hiện:

- `CREATE EXTENSION IF NOT EXISTS unaccent`;
- tạo reverse index `place_categories(category_id, place_id)` nếu chưa có index
  tương đương.

Preflight read-only trước implementation ngày 2026-07-20 xác nhận:

- PostgreSQL image cung cấp `unaccent` 1.1 nhưng live database chưa install;
- `place_categories` chỉ có primary-key index `(place_id, category_id)`;
- không có place slug `search`, nhưng FEAT-003 không tạo route `/search`.

Không thêm trigram/GIN/full-text, index riêng cho `active`, normalized columns
hoặc live demo seed. Search dùng JPA Specification với PostgreSQL `unaccent`;
test-only fixtures 30–100 rows cung cấp pagination/query-plan evidence.

Kết quả verification ngày 2026-07-20:

- Testcontainers PostgreSQL 16 chạy V1–V9 trên database sạch; migrate lại không
  tạo migration mới và Hibernate `ddl-auto=validate` thành công.
- Live database nâng từ V8 lên V9, restart xác nhận schema up to date; V1–V9
  đều có `success=true`.
- `unaccent` 1.1 đã installed; index
  `idx_place_categories_category_place(category_id, place_id)` tồn tại.
- Query category dùng distinct content/count và bind parameters; automated
  statistics xác nhận tối đa 2 SQL/request, không N+1.
- `EXPLAIN (ANALYZE, BUFFERS)` cho category + district + indoor + budget trên
  live demo dataset báo execution 0.090 ms và 15 shared buffer hits. Dataset
  chỉ có 6 demo rows nên đây là evidence local, không phải production benchmark.
- Test-only fixture 30 matching places xác nhận stable pagination và local
  response dưới 500 ms; không có live seed mới.
