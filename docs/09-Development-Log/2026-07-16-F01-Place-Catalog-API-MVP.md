# 2026-07-16 — F01 Place Catalog API MVP

## Mục tiêu

Hoàn thành vertical slice đầu tiên đọc place từ PostgreSQL và trả REST API có
pagination.

## Ghi nhận triển khai ngày 2026-07-16

Phần này là lịch sử của lần triển khai ngày 2026-07-16, không phải xác nhận rằng
branch và live database hiện tại đang đáp ứng đầy đủ Definition of Done.

- Giữ nguyên Flyway V1–V5 vì đã được áp dụng.
- Thêm `V6__seed_demo_places.sql` với 5 place active và 1 place inactive.
- Đồng bộ `Place` entity với schema `places`.
- Thêm repository query active có pagination.
- Thêm DTO summary 11 trường và pagination envelope.
- Thêm mapper, read-only service và controller `GET /api/v1/places`.
- Khóa sorting theo `name ASC`, sau đó `id ASC`.
- Thêm validation cho `page` và `size`.
- Thêm HTTP 400/500 bằng Spring `ProblemDetail`.
- Thêm Testcontainers PostgreSQL/pgvector cùng unit, web và integration tests.

## Verification lịch sử ngày 2026-07-16

```bash
cd backend
./mvnw -DskipTests compile
./mvnw test
```

Kết quả:

- Compile thành công.
- 9 tests.
- 0 failures.
- 0 errors.
- 0 skipped.

Tại thời điểm đó, Testcontainers xác nhận Flyway V1–V6 và Hibernate
`spring.jpa.hibernate.ddl-auto=validate` hoạt động trên PostgreSQL/pgvector.

## Phạm vi không thay đổi

Không triển khai Category, OpeningHour, RAG, Recommendation, filter nâng cao
hoặc Place detail API.

## Ghi chú báo cáo

- Chương 3.2: Place module trong modular monolith.
- Chương 3.5: bảng `places`, constraint và seed demo.
- Chương 3.7: API danh sách, pagination và error contract.

## Cập nhật 2026-07-18

Theo quyết định của owner, toàn bộ automated test, cấu hình Mockito và test
dependencies đã được gỡ để viết lại sau. Kết quả 9 test phía trên là lịch sử
của lần triển khai ngày 2026-07-16, không phản ánh test suite hiện có trong
repository.

Source production và migration files được giữ lại. FEAT-001 chưa đáp ứng đầy đủ
Definition of Done cho đến khi automated tests được khôi phục và chạy lại.

## Cập nhật baseline đã duyệt 2026-07-20

### Trạng thái thực tế được audit

- Source production cho list API vẫn có controller, service, repository,
  mapper, DTO summary và `ProblemDetail` handler.
- Baseline được audit chưa tích hợp startup entrypoint từ nhánh
  `chore/project-bootstrap`; việc tích hợp được xử lý trong task riêng và không
  được tạo entrypoint trùng nếu file đã tồn tại.
- Live database có V1–V5 trong `flyway_schema_history` với `success = true`.
- V6 tồn tại trong source nhưng đang pending trên live database; bảng `places`
  có 0 rows. V6 chỉ được chạy sau khi startup integration được khôi phục.
- Repository hiện không có automated tests, Mockito configuration hoặc test
  dependencies. Kết quả 9 tests ở trên chỉ là lịch sử ngày 2026-07-16.
- FEAT-001 giữ trạng thái **in-progress** cho đến khi startup, V6, automated
  tests và runtime smoke test đều thành công.

### Contract và thứ tự feature đã phê duyệt

- Public endpoint là `GET /api/v1/places`.
- Defaults là `page=0`, `size=20`; `page >= 0`, `1 <= size <= 100`.
- Fixed sort là `name ASC`, sau đó `id ASC`; client không điều khiển sort.
- Response giữ envelope 7 field hiện tại và empty response dùng `content: []`.
- Summary giữ 11 field hiện tại, không có `address`; `shortDescription`
  nullable và `indoor` non-null. `address` dành cho detail response.
- Errors dùng Spring `ProblemDetail`; có thể mở rộng sau bằng `code`,
  `fieldErrors`, `requestId`.
- V2 và toàn bộ V1–V5 là migration canonical/bất biến, không sửa hoặc xóa.
- Khôi phục automated test baseline trước khi bắt đầu feature tiếp theo.
- FEAT-002 được thực hiện trước FEAT-003 và dùng detail route theo `slug`.
- FEAT-003 thêm filters trực tiếp vào `GET /api/v1/places`, không dùng
  `/api/v1/places/search`.

### Điều kiện đóng FEAT-001

1. Tích hợp startup entrypoint từ nguồn hiện có và không tạo bản trùng.
2. Khởi động backend, áp dụng V6 đúng một lần và xác minh Hibernate validate.
3. Khôi phục/rerun automated test baseline.
4. Chạy runtime smoke test cho success, empty và invalid pagination contracts.

## Đóng FEAT-001 ngày 2026-07-20

### Automated verification

Owner cung cấp kết quả Maven clean/verify:

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Log xác nhận Flyway validate 6 migrations, schema public ở version 6 và up to
date; JPA `EntityManagerFactory` khởi tạo thành công; Spring Boot JAR được
repackage thành công. Source integration test dùng PostgreSQL/pgvector
Testcontainers và kiểm tra:

- history V1–V6;
- extension `vector`;
- 6 place rows, gồm 5 active;
- migrate lại không seed trùng;
- các constraint canonical của `places`.

### Runtime smoke test

```bash
curl -i "http://localhost:8080/api/v1/places?page=0&size=20"
curl -i "http://localhost:8080/api/v1/places?page=99&size=20"
curl -i "http://localhost:8080/api/v1/places?page=-1&size=20"
```

Kết quả quan sát:

- Default request: HTTP 200, 5 active places, đúng `name ASC, id ASC`, envelope
  7 field và summary 11 field.
- Page 99: HTTP 200, `content: []`, vẫn giữ pagination metadata.
- Page âm: HTTP 400 `application/problem+json` với Spring `ProblemDetail`.
- Health, `size=101` và `page=abc` được owner xác nhận thành công theo contract.

### Kết luận

FEAT-001 đạt Definition of Done và chuyển sang `done` ngày 2026-07-20. V1–V6
là baseline bất biến cho FEAT-002.
