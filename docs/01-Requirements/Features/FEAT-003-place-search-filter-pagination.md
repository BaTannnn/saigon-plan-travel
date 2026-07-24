---
id: FEAT-003
title: Place Search, Filter & Pagination MVP
aliases:
  - Tìm kiếm, lọc và phân trang địa điểm
  - API khám phá địa điểm MVP
status: done
priority: P0
project: SaigonPlanTravel
owner: Nguyễn Bá Tân
created: 2026-07-17
updated: 2026-07-20
target_milestone: MVP 2026-08-30
canonical_path: docs/01-Requirements/Features/FEAT-003-place-search-filter-pagination.md
tags:
  - saigon-plan-travel
  - feature-spec
  - place
  - search
  - filter
  - pagination
  - backend
  - mvp
related:
  - "[[00-Dashboard/PROJECT_CONTEXT]]"
  - "[[01-Requirements/Features/FEAT-001-place-catalog-api-mvp]]"
  - "[[01-Requirements/Features/FEAT-002-place-detail-category-opening-hours]]"
  - "[[03-Database/Place-Database-Design-v1]]"
  - "[[04-API/Place-API-v1]]"
---

# FEAT-003 — Place Search, Filter & Pagination MVP

> [!note] Frontend consumer (2026-07-21)
> `/places` dùng URL search parameters cho các filter và gọi route thực tế
> `GET /api/v1/places`. Filter change reset page về 0; không gọi `/search`.

> [!summary]
> Mở rộng public `GET /api/v1/places` hiện có bằng năm filter tùy chọn để frontend khám phá active places theo từ khóa, quận/huyện, category, không gian trong nhà và ngân sách. Feature giữ nguyên pagination envelope, summary payload, defaults và fixed ordering của FEAT-001; giữ nguyên detail/category contracts của FEAT-002.

## 1. Trạng thái và phê duyệt

| Thuộc tính | Giá trị |
| --- | --- |
| Trạng thái | `done` |
| Ngày phê duyệt requirements | 2026-07-20 |
| Độ ưu tiên | `P0 — khám phá địa điểm MVP` |
| Feature phụ thuộc | FEAT-001 và FEAT-002 |
| Module sở hữu | `com.saigonplantravel.backend.place` |
| API được mở rộng | `GET /api/v1/places` |
| Migration | V9 — `unaccent` và reverse category index, đã áp dụng/xác minh |
| Implementation | Hoàn tất và đạt full verification ngày 2026-07-20 |

Requirements được owner phê duyệt sau phỏng vấn `$grill-with-docs`. Mọi thay đổi
contract sau mốc này phải được cập nhật vào spec và duyệt trước khi sửa code.

## 2. Bối cảnh

FEAT-001 đã cung cấp catalog public có pagination, fixed ordering và
`PlaceSummaryResponse`. FEAT-002 đã thêm category metadata. Frontend chưa thể
thu hẹp catalog theo từ khóa, khu vực, sở thích, indoor/outdoor hoặc ngân sách.

Spec cũ đề xuất `/api/v1/places/search`, size tối đa 50 và response riêng. Cách
đó mâu thuẫn với source cùng PROJECT_CONTEXT hiện tại. Quyết định đã duyệt là
mở rộng endpoint catalog hiện có và giữ backward compatibility:

- không tạo `/places/search`;
- request không có filter vẫn trả cùng dữ liệu/shape như FEAT-001;
- không thêm `address` hoặc `numberOfElements` vào list response;
- size vẫn tối đa 100.

## 3. Mục tiêu

1. Cho phép tìm active places bằng năm filter tùy chọn.
2. Hỗ trợ tiếng Việt không phân biệt hoa/thường và dấu.
3. Giữ API contract hiện tại ổn định cho frontend.
4. Có validation/error contract rõ ràng và testable.
5. Chứng minh query đúng, không duplicate, không N+1 và phù hợp quy mô 30–100 places.

## 4. Phạm vi

### 4.1. Trong phạm vi

- Mở rộng `GET /api/v1/places` bằng `keyword`, `district`, `category`,
  `indoor`, `maxCost`.
- Mọi filter có giá trị kết hợp bằng AND.
- Keyword tìm trên năm text fields bằng OR.
- Zero-based pagination hiện có: default 0/20, size `1..100`.
- Fixed sort `name ASC, id ASC`.
- Chỉ trả `active=true`.
- Unicode/text normalization và literal LIKE escaping.
- PostgreSQL `unaccent` qua Flyway V9.
- JPA Specification, bound parameters và distinct category query/count.
- Spring `ProblemDetail` với `INVALID_REQUEST` và field errors.
- Unit, web, PostgreSQL integration, regression, query-count và smoke tests.
- Cập nhật spec, DB/API notes, development log và PROJECT_CONTEXT.

### 4.2. Ngoài phạm vi

- Endpoint `/api/v1/places/search`.
- Thay đổi 7-field page envelope hoặc 11-field summary.
- Client-defined sorting hoặc relevance score.
- Fuzzy search, typo tolerance, synonym, autocomplete, highlight.
- Full-text search, trigram, Elasticsearch/OpenSearch.
- Nhiều category trong một request.
- Price range; chỉ có một budget ceiling.
- Filter theo giờ mở cửa, ngày/giờ, khoảng cách, bounding box hoặc route time.
- Cursor pagination, caching, analytics hoặc personalized ranking.
- Frontend, admin API, authentication/authorization.
- Recommendation, scheduling, RAG hoặc AI reranking.
- Seed thêm hàng chục place giả vào live database.

## 5. API contract

### 5.1. Endpoint

```http
GET /api/v1/places?keyword=&district=&category=&indoor=&maxCost=&page=&size=
Accept: application/json
```

Không request body và không authentication trong MVP. Tất cả query parameters
đều optional.

### 5.2. Query parameters

| Parameter | Kiểu | Default | Validation | Semantics |
| --- | --- | --- | --- | --- |
| `keyword` | string | absent | tối đa 100 ký tự sau chuẩn hóa | literal substring trên 5 fields |
| `district` | string | absent | tối đa 100 ký tự sau chuẩn hóa | exact normalized match |
| `category` | string | absent | tối đa 120, lowercase slug regex | exact category slug |
| `indoor` | boolean | absent | chỉ `true`/`false` | exact boolean match |
| `maxCost` | decimal | absent | `0..100000000` | `place.minCost <= maxCost` |
| `page` | integer | `0` | `>= 0` | zero-based page |
| `size` | integer | `20` | `1..100` | page size |

Category regex:

```regex
^[a-z0-9]+(?:-[a-z0-9]+)*$
```

### 5.3. Response

Response tái sử dụng chính xác `PlacePageResponse` FEAT-001:

```json
{
  "content": [
    {
      "id": 1,
      "name": "Demo Art Space",
      "slug": "demo-art-space",
      "shortDescription": "Dữ liệu minh họa, chưa được xác minh",
      "district": "Quận 1",
      "latitude": 10.7750000,
      "longitude": 106.7000000,
      "estimatedVisitMinutes": 90,
      "minCost": 50000.00,
      "maxCost": 150000.00,
      "indoor": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

- Envelope có đúng 7 fields.
- Mỗi content item có đúng 11 fields.
- Không thêm `address`, categories, opening hours hoặc `numberOfElements`.
- Không trả Entity hoặc Spring `Page` trực tiếp.

### 5.4. Empty và page vượt phạm vi

Filter hợp lệ không có kết quả hoặc page hợp lệ vượt trang cuối trả `200 OK`:

```json
{
  "content": [],
  "page": 99,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1,
  "first": false,
  "last": true
}
```

Không tự chuyển về page cuối, không trả 404.

### 5.5. Validation error

Lỗi parameter trả `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/places",
  "code": "INVALID_REQUEST",
  "fieldErrors": [
    {
      "field": "size",
      "message": "must be less than or equal to 100"
    }
  ]
}
```

Pagination error message/title hiện có có thể được giữ để tương thích, nhưng
mọi invalid FEAT-003 request phải có `status=400`, `code=INVALID_REQUEST` và
field error an toàn. Không trả SQL, stack trace hoặc connection data.

## 6. Quy tắc nghiệp vụ

| ID | Quy tắc |
| --- | --- |
| BR-001 | Public catalog luôn bắt buộc `active=true`. |
| BR-002 | Các filter khác loại kết hợp AND; năm keyword fields kết hợp OR. |
| BR-003 | Text được NFC normalize, trim và collapse whitespace. |
| BR-004 | Keyword/district dùng `lower(unaccent(...))` cho cột và input. |
| BR-005 | Keyword là literal substring; `%`, `_`, `\` phải được escape. |
| BR-006 | District là exact normalized match, không phải substring. |
| BR-007 | Một request chỉ nhận một category lowercase slug. |
| BR-008 | Category hợp lệ nhưng không tồn tại trả empty page. |
| BR-009 | `indoor=false` là filter có mặt, không được coi là absent. |
| BR-010 | Budget phù hợp khi `min_cost <= maxCost`, inclusive. |
| BR-011 | Sorting luôn là `name ASC, id ASC`; không relevance/client sort. |
| BR-012 | Blank normalized keyword/district được coi là absent. |
| BR-013 | Category join không được duplicate place hoặc làm sai total count. |
| BR-014 | Dữ liệu demo không được trình bày như dữ liệu đã xác minh. |

## 7. Chuẩn hóa và query semantics

### 7.1. Keyword

Keyword tìm literal substring trên:

1. `name`;
2. `short_description`;
3. `full_description`;
4. `address`;
5. `district`.

Text field `NULL` không làm query lỗi. Không có ranking hoặc highlight.

### 7.2. District

Ví dụ `quan 1` khớp `Quận 1`; `1` không khớp. Exact match được áp dụng sau
NFC/trim/collapse/lower/unaccent.

### 7.3. Category

Join category chỉ được tạo khi `category` có giá trị. Match theo exact slug.
Query phải distinct và count distinct đúng khi place có nhiều categories.

### 7.4. Cost và indoor

- `maxCost=100000` giữ place có `minCost <= 100000`.
- Không yêu cầu toàn bộ khoảng `maxCost` của place nằm dưới budget.
- Indoor filter dùng trực tiếp `places.indoor`.

## 8. Database và migration

FEAT-003 không thêm business table hoặc Entity field. Sau V8, tạo:

```text
V9__enable_unaccent_and_place_search_index.sql
```

V9 chỉ:

1. `CREATE EXTENSION IF NOT EXISTS unaccent`;
2. tạo index `place_categories(category_id, place_id)` nếu chưa có index tương đương.

Không thêm:

- trigram/GIN/full-text;
- B-tree cho `%keyword%`;
- index riêng cho `active`;
- normalized search columns hoặc materialized view.

Preflight ngày 2026-07-20 xác nhận `unaccent` version 1.1 có trong PostgreSQL
image nhưng chưa installed; `place_categories` hiện chỉ có PK `(place_id,
category_id)`. V9 phải chạy trên database sạch và database đã có V1–V8.

## 9. Kiến trúc triển khai đã duyệt

- `PlaceRepository` mở rộng `JpaSpecificationExecutor<Place>`.
- Dynamic predicates dùng JPA Specification/Criteria API.
- PostgreSQL `unaccent` được gọi qua Criteria function.
- Mọi input dùng bound parameters; không nối SQL/JPQL từ client.
- Không tạo `PlaceRepositoryImpl`, native query hoặc search module riêng.
- Service đọc dùng `@Transactional(readOnly = true)`.
- Controller chỉ bind/validate và gọi service.
- Mapper/DTO FEAT-001 được tái sử dụng.

## 10. Yêu cầu chức năng

| ID | Yêu cầu |
| --- | --- |
| FR-001 | Mở rộng public `GET /api/v1/places`, không thêm `/search`. |
| FR-002 | Năm filters đều optional và kết hợp theo BR-002. |
| FR-003 | Không filter phải giữ behavior FEAT-001. |
| FR-004 | Keyword/district không phân biệt hoa thường và dấu. |
| FR-005 | Category/indoor/cost phải theo BR-007–BR-010. |
| FR-006 | Pagination/default/limits/response giữ contract mục 5. |
| FR-007 | Chỉ active places được trả và tính total. |
| FR-008 | Fixed ordering phải ổn định qua các page. |
| FR-009 | Invalid input trả common ProblemDetail theo mục 5.5. |
| FR-010 | Query category phải distinct content/count. |
| FR-011 | Không trả Entity, Spring Page hoặc lazy collection. |
| FR-012 | Detail và category endpoints FEAT-002 không đổi. |

## 11. Trường hợp biên

- Keyword/district `null`, empty hoặc whitespace-only.
- Keyword chứa `%`, `_`, `\` hoặc Unicode có dấu.
- Một hoặc nhiều DB text fields là `NULL`.
- `indoor=false`.
- `maxCost=0` và `maxCost=100000000`.
- Category đúng format nhưng không tồn tại; category sai format/case.
- Place có nhiều category.
- Nhiều places trùng name.
- Page hợp lệ vượt phạm vi.
- `page=-1`, `size=0/101`, decimal/boolean/integer sai kiểu.
- Filter combination không có kết quả.
- Inactive place thỏa mọi filter.

Strict rejection cho unknown hoặc repeated query parameter chưa phải contract
MVP; implementation không được gây 500 hoặc thay đổi semantics của parameter đã khóa.

## 12. Yêu cầu phi chức năng

| ID | Yêu cầu |
| --- | --- |
| NFR-001 | Với fixture 30–100 places, request sau warm-up mục tiêu dưới 500 ms. |
| NFR-002 | Tối đa 2 SQL queries thông thường: content và count; không N+1. |
| NFR-003 | Category distinct/count phải đúng trên PostgreSQL thật. |
| NFR-004 | Input dùng bound parameters và wildcard escaping. |
| NFR-005 | Hibernate `ddl-auto=validate` và Flyway migration-first giữ nguyên. |
| NFR-006 | Không thêm dependency/search engine/custom repository implementation. |
| NFR-007 | API response độc lập Spring Page serialization. |

## 13. Tiêu chí chấp nhận

### AC-001 — Không filter

Request không filter trả active places theo default 0/20, size max 100, fixed
sort và đúng contract FEAT-001.

### AC-002 — Keyword không dấu

`BAO TANG` khớp `Bảo tàng` trong bất kỳ field nào thuộc danh sách mục 7.1.

### AC-003 — Literal wildcard

`%`, `_`, `\` trong keyword được match như ký tự literal, không mở rộng pattern.

### AC-004 — District exact normalized

`quan 1` khớp `Quận 1`; `1` không khớp.

### AC-005 — Category

Exact existing slug lọc đúng; valid missing slug trả `200` empty page; invalid
slug trả `400 INVALID_REQUEST`.

### AC-006 — Indoor

`indoor=false` chỉ trả outdoor active places.

### AC-007 — Budget

`maxCost=100000` giữ place có `minCost <= 100000` và loại place cao hơn.

### AC-008 — AND combination

Khi gửi cả năm filters, chỉ place thỏa toàn bộ được trả.

### AC-009 — Active invariant

Inactive place không xuất hiện và không được tính vào `totalElements`.

### AC-010 — Pagination

Size 1/100 hợp lệ; 0/101 invalid; negative page invalid; out-of-range page trả
200 empty content cùng total metadata thực.

### AC-011 — Stable ordering

Name trùng nhau không gây mất/trùng record giữa page nhờ `id ASC` tie-breaker.

### AC-012 — Distinct category count

Place nhiều category chỉ xuất hiện một lần và total phản ánh distinct places.

### AC-013 — Errors

Invalid format/type/limit trả safe ProblemDetail với `INVALID_REQUEST` và
fieldErrors; repository không được gọi khi request validation thất bại.

### AC-014 — Migration

V9 cài `unaccent`, tạo reverse category index, chạy đúng một lần trên database
sạch/live V8 và không sửa V1–V8.

### AC-015 — Query/performance

Automated query count không vượt 2, không N+1; local warm response trên fixture
30–100 places dưới 500 ms và có query-plan evidence.

### AC-016 — Regression

28 tests FEAT-001/002 cùng test FEAT-003 mới đều pass; list/detail/categories
giữ status, shape, ordering và error behavior đã khóa.

## 14. File-by-file implementation plan đề xuất

### Phase A — Baseline và migration preflight

- [x] Khôi phục 6 test classes FEAT-001/002 từ Git dangling blobs.
- [x] `./mvnw clean test`: 28 tests pass ngày 2026-07-20.
- [x] Xác nhận source/live database đang ở V8.
- [x] Xác nhận `unaccent` available nhưng chưa installed và reverse index chưa có.
- [x] Bảo đảm các test phục hồi, V7/V8 và docs vẫn hiện diện trong workspace.

### Phase B — Database

- [x] Tạo `backend/src/main/resources/db/migration/V9__enable_unaccent_and_place_search_index.sql`.
- [x] Bật `unaccent` idempotently.
- [x] Tạo reverse index `(category_id, place_id)`.
- [x] Test clean migration, V8→V9, restart và Hibernate validate.

### Phase C — Request validation và normalization

- [x] Tạo `backend/src/main/java/com/saigonplantravel/backend/place/dto/PlaceSearchRequest.java`.
- [x] Thêm Jakarta validation theo mục 5.2.
- [x] Tạo `backend/src/main/java/com/saigonplantravel/backend/place/search/PlaceSearchNormalizer.java`.
- [x] Test NFC, trim, whitespace collapse, blank-as-absent và LIKE escaping.

### Phase D — Dynamic repository query

- [x] Mở rộng `PlaceRepository` bằng `JpaSpecificationExecutor<Place>`.
- [x] Tạo `backend/src/main/java/com/saigonplantravel/backend/place/repository/specification/PlaceSpecifications.java`.
- [x] Thêm active predicate bắt buộc và năm optional filter predicates.
- [x] Join category chỉ khi cần; distinct content/count đúng.
- [x] Không fetch nested collections.

### Phase E — Service, controller và error handling

- [x] Mở rộng `PlaceService` để nhận normalized search request, defaults và fixed sort.
- [x] Mở rộng `PlaceController#getPlaces`; không tạo route/controller `/search`.
- [x] Mở rộng `GlobalExceptionHandler` bằng `INVALID_REQUEST` và `fieldErrors`.
- [x] Giữ pagination/list/detail/category contracts hiện có.

### Phase F — Automated tests

- [x] Khôi phục tests phải vẫn hiện diện và pass.
- [x] Thêm unit tests cho normalizer và service mapping; predicates được kiểm tra trên PostgreSQL thật.
- [x] Mở rộng controller tests cho binding, response và validation errors.
- [x] Mở rộng PostgreSQL integration tests cho unaccent, five filters, AND,
  wildcard, distinct count, pagination và tối đa 2 queries.
- [x] Tạo test-only fixture 30 places; không thêm live seed.
- [x] Chạy `./mvnw clean test` và `./mvnw clean verify`.

### Phase G — Runtime và tài liệu

- [x] Apply V9 trên live V8; xác minh `flyway_schema_history`, extension và index.
- [x] Smoke test no-filter, từng filter, combination, invalid và out-of-range cases.
- [x] Ghi response time, dataset, query count và `EXPLAIN (ANALYZE, BUFFERS)`.
- [x] Review diff, secret scan và phạm vi.
- [x] Cập nhật API/DB/context/development log; chuyển spec `done` khi DoD đạt.

## 15. Chiến lược kiểm thử

### Unit

- Normalization và wildcard escaping.
- Defaults 0/20, size max 100 và fixed sort.
- Từng predicate cùng AND composition.
- Page → `PlacePageResponse` mapping.

### Web

- Bind mọi parameter, đặc biệt `indoor=false`.
- 7-field/11-field JSON contract.
- Validation ProblemDetail và fieldErrors.
- Không filter regression.

### PostgreSQL integration

- V1–V9, extension/index và migrate lại không trùng.
- Accent-insensitive matching trên năm fields.
- District exact, category distinct, budget và inactive exclusion.
- Pagination/name tie-breaker, wildcard literal và query count tối đa 2.

### Runtime

- Health và catalog không filter.
- Từng filter, full combination và empty result.
- Invalid type/range/slug và out-of-range page.
- Query plan và warm timing với dataset được ghi rõ.

## 16. Bằng chứng xác minh ngày 2026-07-20

- `./mvnw clean test`: 39 tests, 0 failures/errors/skipped.
- `./mvnw clean verify`: 39 tests pass, JAR được Spring Boot repackage thành công.
- Testcontainers PostgreSQL 16 chạy sạch V1–V9; migrate lại không tạo migration
  mới; Hibernate `ddl-auto=validate` thành công.
- Integration fixture 30 matching places xác nhận fixed `name,id` ordering,
  hai trang không trùng, mỗi request dùng 1–2 SQL và dưới 500 ms.
- Hibernate SQL xác nhận text/category inputs dùng bind parameters (`?`), LIKE
  escape hoạt động với `%`, `_`, `\`.
- Live database có V1–V9 `success=true`, `unaccent` 1.1 và index
  `idx_place_categories_category_place(category_id, place_id)`.
- `EXPLAIN (ANALYZE, BUFFERS)` cho full category/district/indoor/budget query
  trên 6 demo rows: 0.090 ms execution, 15 shared buffer hits. Đây là evidence
  local, không phải production benchmark.
- Runtime JAR smoke: no-filter, từng filter, full combination, valid empty,
  out-of-range, invalid requests, detail và categories đều đúng status/contract.
  Warm filter calls quan sát khoảng 5–37 ms; request đầu sau startup khoảng
  260 ms.

## 17. Rủi ro còn lại

| Rủi ro | Giảm thiểu |
| --- | --- |
| Môi trường production có thể thiếu quyền cài extension | Live dev và clean PostgreSQL đã pass; deployment khác phải preflight quyền trước release. |
| Criteria `unaccent` khác behavior native SQL | PostgreSQL integration test là nguồn bằng chứng. |
| Category join làm sai count | Distinct content/count tests với place nhiều category. |
| Source/tests/docs hiện chưa được commit đầy đủ | Không chạy cleanup; commit/push có chủ đích ở workflow Git riêng. |
| Live dataset chỉ có 6 demo rows | Gate 30 rows dùng test-only fixture; cần đo lại khi catalog thật đạt 30–100 rows. |
| `%keyword%` không dùng B-tree | Chấp nhận ở quy mô MVP; đo trước khi cân nhắc trigram. |

## 18. Definition of Done

- [x] Locked requirements được owner phê duyệt ngày 2026-07-20.
- [x] File-by-file plan được owner phê duyệt.
- [x] Implementation không vượt phạm vi mục 4.
- [x] V9 clean/live/restart và Hibernate validation pass.
- [x] Năm filters, AND semantics, normalization và validation đúng contract.
- [x] Pagination/response/sorting và FEAT-001/002 regression đúng.
- [x] Category content/count distinct và query count tối đa 2.
- [x] Full tests, clean verify và smoke tests pass.
- [x] Performance/query-plan evidence được ghi lại.
- [x] Diff không chứa secret, sửa migration cũ hoặc thay đổi ngoài scope.
- [x] API note, DB note, development log và PROJECT_CONTEXT được cập nhật.

## 19. Tài liệu và khóa luận

- API note: optional filters, normalization, validation và examples.
- DB note: V9, `unaccent`, reverse category index và query-plan evidence.
- Development log: decisions, test recovery, implementation, lỗi và verification.
- PROJECT_CONTEXT: FEAT-003 status và feature kế tiếp.
- Khóa luận Chương 3: dynamic query, API/pagination, PostgreSQL unaccent.
- Khóa luận Chương 4: test matrix, distinct count, query count/plan và latency.
- Không tạo ADR: lựa chọn JPA Specification/PostgreSQL `unaccent` nằm trong
  Place Module và chưa phải quyết định khó đảo ngược ở cấp hệ thống.

## 20. Feature kế tiếp đề xuất

Sau FEAT-003: **FEAT-004 — Trip Preferences & Draft Trip MVP**. Không triển khai
trip/scheduling trong FEAT-003.

## 21. Lịch sử thay đổi

| Ngày | Phiên bản | Thay đổi |
| --- | --- | --- |
| 2026-07-17 | 0.1 | Draft endpoint `/places/search`. |
| 2026-07-20 | 1.0 | Owner khóa mở rộng `/places`, năm filters, 7/11-field response, size 100, unaccent V9, Specification, errors, tests và performance gate; chuyển `approved`. |
| 2026-07-20 | 1.1 | Owner duyệt file-by-file plan; chuyển `in-progress`. |
| 2026-07-20 | 1.2 | Hoàn tất implementation, 39 tests, V9 live/clean, bind-parameter review, smoke/query-plan evidence và chuyển `done`. |
