# 2026-07-20 — FEAT-003 Requirements Approval

## Kết quả

Owner phê duyệt locked requirements và file-by-file plan cho FEAT-003 Place
Search, Filter & Pagination MVP. Implementation, verification và documentation
hoàn tất cùng ngày; feature chuyển từ `approved` qua `in-progress` sang `done`.

## Baseline được kiểm tra

- FEAT-001/002 production code hiện diện trên `feat/places`.
- Live database ở Flyway V8 với 6 places/5 active, 5 categories, 8 relations
  và 33 opening-hour rows.
- `unaccent` 1.1 available nhưng chưa installed.
- `place_categories` chỉ có index theo `(place_id, category_id)`.
- Sáu test classes bị mất vì không nằm trong commit đã được tìm thấy nguyên vẹn
  trong Git dangling blobs và phục hồi bằng patch.
- `./mvnw clean test`: 28 tests, 0 failures, 0 errors, 0 skipped.

## Quyết định API

- Mở rộng `GET /api/v1/places`; không tạo `/places/search`.
- Filters: `keyword`, `district`, `category`, `indoor`, `maxCost`.
- Giữ exact 7-field page envelope và 11-field summary.
- Giữ default `page=0`, `size=20`, size range `1..100`.
- Fixed sort `name ASC, id ASC`; không client sort hoặc relevance.
- Invalid request dùng Spring `ProblemDetail`, `code=INVALID_REQUEST` và
  `fieldErrors`.

## Quyết định nghiệp vụ

- Filters kết hợp AND; năm keyword fields kết hợp OR.
- Keyword là literal substring trên name, short/full description, address và district.
- Text dùng NFC, trim, collapse whitespace, lower và PostgreSQL `unaccent`.
- District exact normalized match.
- Category nhận một exact lowercase slug; valid missing slug trả empty page.
- `indoor=false` là filter hợp lệ.
- Budget phù hợp khi `place.minCost <= maxCost`.
- `%`, `_`, `\` là literal, không phải client wildcard.

## Quyết định database và kiến trúc

- V9 bật `unaccent` và thêm reverse index `(category_id, place_id)`.
- Không thêm table/column, trigram, full-text, active index hoặc live demo seed.
- `PlaceRepository` dùng `JpaSpecificationExecutor` và Criteria API.
- Không `RepositoryImpl`, native search query hoặc search engine.
- Test-only fixtures 30–100 places dùng cho pagination/performance evidence.

## Acceptance gates

- Từng filter, full AND combination, normalization và edge cases đúng contract.
- Category join không duplicate và total count đúng.
- Tối đa 2 SQL queries/request; không N+1.
- Mục tiêu dưới 500 ms sau warm-up trên fixture 30–100 places.
- 28 regression tests cùng FEAT-003 tests, clean verify, startup và smoke pass.
- V9 chạy clean/live/restart; Hibernate validate thành công.

## Ngoài phạm vi

Frontend, `/places/search`, multi-category, client sorting, relevance/fuzzy/full-text,
distance/open-now filters và generated recommendation.

## Tài liệu liên quan

- `docs/01-Requirements/Features/FEAT-003-place-search-filter-pagination.md`
- `docs/04-API/Place-API-v1.md`
- `docs/03-Database/Place-Database-Design-v1.md`
- `docs/00-Dashboard/PROJECT_CONTEXT.md`

## Báo cáo khóa luận

- Chương 3: dynamic filtering, pagination contract, JPA Specification và unaccent.
- Chương 4: filter test matrix, distinct count, query count/plan và latency.

## Bước tiếp theo

Review/commit/push có chủ đích toàn bộ workspace FEAT-001–003, sau đó dùng
`$grill-with-docs` để khóa FEAT-004. Không triển khai Trip trong FEAT-003.

## Implementation

- Thêm `PlaceSearchRequest` với Jakarta Validation và defaults 0/20.
- Thêm NFC/whitespace normalizer cùng literal LIKE escaping.
- `PlaceRepository` dùng `JpaSpecificationExecutor`; `PlaceSpecifications`
  tạo active predicate bắt buộc và năm optional predicates.
- Search text dùng PostgreSQL `unaccent` với bind parameters; category join
  distinct content/count.
- Controller bind strict boolean; common error handler trả safe ProblemDetail
  có `INVALID_REQUEST` và `fieldErrors`.
- V9 bật `unaccent` và tạo reverse index `(category_id, place_id)`; không thêm
  business schema hoặc live demo data.

## Verification cuối

- `./mvnw clean test`: 39/39 tests pass.
- `./mvnw clean verify`: 39/39 tests pass, JAR repackage thành công.
- PostgreSQL Testcontainers: V1–V9 clean migration, rerun idempotent, Hibernate
  validate, wildcard/accent/filters/distinct/query-count tests đều pass.
- Fixture 30 matching places: stable pagination, không trùng giữa page, mỗi
  request 1–2 SQL và dưới 500 ms local.
- Live V8→V9/restart: history V1–V9 success, `unaccent` 1.1 và reverse index.
- Query plan representative: execution 0.090 ms, 15 shared buffer hits trên 6
  demo rows; chỉ là local evidence.
- Final JAR smoke: no-filter và full combination 200; invalid request 400;
  `indoor=TRUE` bị từ chối 400; detail/category regression 200. Warm filters
  quan sát khoảng 5–37 ms.
- Review phát hiện Criteria literal ban đầu; đã đổi sang Hibernate Criteria
  `value(...)` và xác nhận SQL sinh `unaccent(?)` trước full verify cuối.

## Ánh xạ khóa luận

- Chương 3 — Thiết kế hệ thống: Place Module package-by-feature, mở rộng catalog
  contract, JPA Specification và phân tách controller/service/repository/DTO.
- Chương 3 — Thiết kế dữ liệu: Flyway V9, PostgreSQL `unaccent`, reverse
  many-to-many index và lý do chưa dùng trigram/full-text ở quy mô MVP.
- Chương 4 — Cài đặt: normalization tiếng Việt, literal wildcard escaping,
  strict validation, bound parameters và distinct category query.
- Chương 4 — Đánh giá: ma trận 39 tests, fixture 30 places, SQL query count,
  `EXPLAIN ANALYZE`, runtime smoke và giới hạn của live demo dataset 6 rows.
