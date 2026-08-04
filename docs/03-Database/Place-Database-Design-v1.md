# Place Database Design v1

## Trạng thái

Place Module dùng PostgreSQL, Flyway migration-first và Hibernate
`ddl-auto=validate`. Migration hiện có từ V1 đến V10.

Theo quyết định owner ngày 2026-07-27, database development sẽ được drop và
tạo lại. Vì vậy administrative-unit refactor được viết trực tiếp vào V2 và V6,
không tạo V11.

> Database đã áp dụng V2 hoặc V6 phiên bản cũ không được chạy tiếp bằng lịch sử
> mới vì checksum sẽ khác. Phải recreate database trước khi khởi động backend.

## Bảng `places`

V2 tạo các cột:

| Cột | Kiểu | Null | Ghi chú |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | No | Primary key |
| `name` | `VARCHAR(150)` | No | Tên hiển thị |
| `slug` | `VARCHAR(180)` | No | Unique |
| `short_description` | `VARCHAR(500)` | Yes | Mô tả card |
| `full_description` | `TEXT` | Yes | Mô tả detail |
| `address` | `VARCHAR(255)` | No | Địa chỉ dạng văn bản |
| `administrative_unit_name` | `VARCHAR(100)` | Yes | Tên đơn vị hành chính đã xác minh |
| `administrative_unit_type` | `VARCHAR(20)` | Yes | `WARD`, `COMMUNE`, `SPECIAL_ZONE` |
| `latitude` | `NUMERIC(10,7)` | No | `-90..90` |
| `longitude` | `NUMERIC(10,7)` | No | `-180..180` |
| `estimated_visit_minutes` | `INTEGER` | No | Lớn hơn 0 |
| `min_cost` | `NUMERIC(12,2)` | No | Không âm |
| `max_cost` | `NUMERIC(12,2)` | No | Không nhỏ hơn min cost |
| `indoor` | `BOOLEAN` | No | Môi trường địa điểm |
| `active` | `BOOLEAN` | No | Trạng thái public catalog |
| `created_at` | `TIMESTAMPTZ` | No | Default current timestamp |
| `updated_at` | `TIMESTAMPTZ` | No | Default current timestamp |

## Administrative-unit invariants

Database enforce:

1. Name và type cùng `NULL`, hoặc cùng có giá trị.
2. Name có giá trị không được blank sau `TRIM`.
3. Type có giá trị chỉ được là `WARD`, `COMMUNE`, `SPECIAL_ZONE`.

Nullable là chủ ý: dữ liệu chưa được xác minh phải giữ trạng thái unknown,
không được suy diễn từ địa chỉ, tọa độ hay thông tin hành chính cũ.

## Seed V6

V6 vẫn seed sáu place demo, gồm năm active và một inactive. Tất cả đều có:

```text
administrative_unit_name = NULL
administrative_unit_type = NULL
```

Các row cần mapping thủ công:

- `demo-art-space`
- `demo-city-garden`
- `demo-history-hall`
- `demo-riverside-walk`
- `demo-science-center`
- `demo-temporarily-hidden-place`

Không sửa seed để gán tên hoặc type trước khi có dữ liệu được owner xác minh.

## Search và index

- Keyword dùng `lower(unaccent(...))` trên name, descriptions, address và
  `administrative_unit_name`.
- Filter administrative-unit name là exact normalized match.
- V9 tiếp tục sở hữu extension `unaccent` và reverse category index.
- Chưa thêm index cho administrative-unit name ở quy mô 30–100 place.
- Chỉ thêm index mới sau khi có `EXPLAIN (ANALYZE, BUFFERS)` chứng minh nhu cầu.

## Migration inventory

| Version | Vai trò |
| --- | --- |
| V1 | Enable pgvector |
| V2 | Create canonical places table |
| V3 | Create categories |
| V4 | Create place-category join table |
| V5 | Create opening hours |
| V6 | Seed six demo places |
| V7 | Strengthen category/opening-hour constraints |
| V8 | Seed category and opening-hour demo metadata |
| V9 | Enable unaccent and reverse category index |
| V10 | Create authentication users table |
