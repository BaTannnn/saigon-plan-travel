# Place API v1

## Trạng thái

Contract FEAT-001, FEAT-002 và FEAT-003 đạt `done` ngày 2026-07-20. Source,
Flyway V1–V9, 39 automated tests, Hibernate validation và runtime smoke test
đã thành công.

## Endpoint đầu tiên

```http
GET /api/v1/places?page=0&size=20
```

Đây là public contract, không yêu cầu đăng nhập và đã được xác minh trên normal
runtime ngày 2026-07-20.

## Query parameters

| Parameter | Kiểu | Mặc định | Giới hạn |
|---|---:|---:|---|
| `page` | integer | `0` | `page >= 0` |
| `size` | integer | `20` | `1 <= size <= 100` |

Không hỗ trợ client-controlled sorting trong FEAT-001.

## Response thành công

```json
{
  "content": [
    {
      "id": 1,
      "name": "Demo Place",
      "slug": "demo-place",
      "shortDescription": null,
      "district": "Quận 1",
      "latitude": 10.0000000,
      "longitude": 106.0000000,
      "estimatedVisitMinutes": 60,
      "minCost": 0,
      "maxCost": 100000,
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

Envelope có đúng 7 field cấp cao: `content`, `page`, `size`, `totalElements`,
`totalPages`, `first`, `last`.

Mỗi item trong `content` có đúng 11 field trong ví dụ:

- `id`, `name`, `slug`, `shortDescription`, `district`;
- `latitude`, `longitude`, `estimatedVisitMinutes`;
- `minCost`, `maxCost`, `indoor`.

`address` không thuộc list summary và chỉ dành cho detail response.
`shortDescription` là nullable nhưng vẫn xuất hiện với giá trị `null`;
`indoor` là boolean non-null.

## Quy tắc

- Chỉ trả địa điểm active.
- Không trả Entity.
- Kết quả sắp xếp cố định theo `name ASC`, sau đó `id ASC`.
- Dữ liệu seed chưa xác minh phải ghi rõ demo.
- Pagination được áp dụng sau active filter và sorting.

## Empty result

- Không có địa điểm active: HTTP 200 với `content: []`.
- `page` hợp lệ nhưng vượt trang cuối: HTTP 200 với `content: []`.

Response vẫn giữ đầy đủ envelope:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

## Errors

Pagination sai kiểu hoặc ngoài giới hạn trả HTTP 400 theo Spring
`ProblemDetail`, gồm tối thiểu:

- `type`
- `title`
- `status`
- `detail`
- `instance`

Lỗi database hoặc lỗi hệ thống ngoài dự kiến trả HTTP 500 `ProblemDetail`.
Response không để lộ SQL, stack trace, credential hoặc thông tin kết nối.

Các extension `code`, `fieldErrors` và `requestId` có thể được phê duyệt/thêm
sau trên nền Spring `ProblemDetail`; chúng không phải field bắt buộc của
contract FEAT-001 hiện tại.

## FEAT-002 — Place Detail và Category Catalog

Hai public route được triển khai và xác minh runtime ngày 2026-07-20:

```http
GET /api/v1/places/{slug}
GET /api/v1/categories
```

Contract đã khóa:

- Detail payload gồm đúng 15 field: 13 scalar field `id`, `name`, `slug`,
  `shortDescription`, `fullDescription`, `address`, `district`, `latitude`,
  `longitude`, `estimatedVisitMinutes`, `minCost`, `maxCost`, `indoor`, cùng
  `categories` và `openingHours`.
- `categories` item gồm `id`, `name`, `slug`.
- `openingHours` item gồm `dayOfWeek`, `closed`, `openTime`, `closeTime`;
  `dayOfWeek` dùng `1..7` và time format chính xác `HH:mm`.
- Nested collection rỗng trả `[]`, không trả `null`.
- Category catalog là public root array không pagination, trả toàn bộ category,
  sort `name ASC`, sau đó `id ASC`; empty response là `200 OK` với `[]`.
- Detail slug lookup exact/case-sensitive; không trim, lowercase hoặc redirect.
- Missing, malformed và inactive slug cùng trả 404 Spring `ProblemDetail` với
  extension `code=PLACE_NOT_FOUND` và cùng thông điệp công khai.
- `closed=true` trả time `null`; missing day là unknown và không xuất hiện;
  `closed=false` yêu cầu `openTime < closeTime`.

FEAT-002 không thay đổi pagination envelope, 11-field summary, validation,
fixed sort hoặc empty behavior của `GET /api/v1/places`.

### Place Detail response đã xác minh

```json
{
  "id": 1,
  "name": "Demo Art Space",
  "slug": "demo-art-space",
  "shortDescription": "Dữ liệu minh họa, chưa được xác minh",
  "fullDescription": "Địa điểm mô phỏng chỉ phục vụ phát triển và kiểm thử.",
  "address": "Địa chỉ demo 1",
  "district": "Quận 1",
  "latitude": 10.7750000,
  "longitude": 106.7000000,
  "estimatedVisitMinutes": 90,
  "minCost": 50000.00,
  "maxCost": 150000.00,
  "indoor": true,
  "categories": [
    { "id": 3, "name": "Nghệ thuật", "slug": "nghe-thuat" },
    { "id": 1, "name": "Văn hóa", "slug": "van-hoa" }
  ],
  "openingHours": [
    {
      "dayOfWeek": 1,
      "closed": false,
      "openTime": "09:00",
      "closeTime": "17:00"
    },
    {
      "dayOfWeek": 2,
      "closed": true,
      "openTime": null,
      "closeTime": null
    }
  ]
}
```

`shortDescription` và `fullDescription` có thể là `null`. `categories` và
`openingHours` luôn là array. Không có opening-hour row cho một ISO day nghĩa
là unknown và ngày đó không xuất hiện trong array.

### Place Detail not-found đã xác minh

```json
{
  "type": "about:blank",
  "title": "Place not found",
  "status": 404,
  "detail": "Place not found",
  "instance": "/api/v1/places/slug-khong-ton-tai",
  "code": "PLACE_NOT_FOUND"
}
```

Đây là contract chung cho slug missing, malformed hoặc thuộc inactive place.

### Category Catalog response đã xác minh

```json
[
  { "id": 4, "name": "Khoa học", "slug": "khoa-hoc" },
  { "id": 2, "name": "Lịch sử", "slug": "lich-su" },
  { "id": 3, "name": "Nghệ thuật", "slug": "nghe-thuat" },
  { "id": 5, "name": "Ngoài trời", "slug": "ngoai-troi" },
  { "id": 1, "name": "Văn hóa", "slug": "van-hoa" }
]
```

Endpoint do `CategoryController` trong Place Module sở hữu, không pagination và
trả `[]` nếu bảng category rỗng.

## FEAT-003 — đã triển khai và xác minh

FEAT-003 **mở rộng endpoint hiện có**, không tạo `/api/v1/places/search`:

```http
GET /api/v1/places?keyword=&district=&category=&indoor=&maxCost=&page=&size=
```

Năm filter đều optional. Không có filter phải giữ nguyên response FEAT-001.

| Parameter | Validation | Semantics |
| --- | --- | --- |
| `keyword` | tối đa 100 ký tự sau chuẩn hóa | literal substring trên name, short/full description, address, district |
| `district` | tối đa 100 ký tự | exact match sau trim/lower/unaccent |
| `category` | lowercase slug, tối đa 120 | exact một category slug |
| `indoor` | `true` hoặc `false` | exact boolean; `false` không phải absent |
| `maxCost` | decimal `0..100000000` | `place.minCost <= maxCost` |
| `page` | integer `>= 0`, default 0 | zero-based |
| `size` | integer `1..100`, default 20 | giữ FEAT-001 |

Quy tắc đã khóa:

- filters kết hợp AND; năm keyword fields kết hợp OR;
- keyword/district không phân biệt hoa thường và dấu bằng PostgreSQL `unaccent`;
- NFC normalize, trim, collapse whitespace; blank text là absent;
- `%`, `_`, `\` được coi là literal;
- sorting cố định `name ASC, id ASC`;
- chỉ active places; category join không duplicate hoặc làm sai total;
- valid-but-missing category và page vượt phạm vi trả `200` empty page;
- invalid input trả Spring `ProblemDetail` có `code=INVALID_REQUEST` và
  `fieldErrors`;
- response vẫn đúng 7-field `PlacePageResponse` và 11-field
  `PlaceSummaryResponse`; không thêm `address` hoặc `numberOfElements`.

Requirements và file-by-file plan được owner phê duyệt ngày 2026-07-20.
Implementation đã hoàn tất cùng ngày và giữ nguyên contract FEAT-001/002.

## Thứ tự mở rộng đã duyệt

1. FEAT-001 và FEAT-002 đã `done`; detail API tại
   `GET /api/v1/places/{slug}` chứa `address`, categories và opening hours.
2. FEAT-003 đã mở rộng trực tiếp endpoint
   `GET /api/v1/places` bằng `keyword`, `district`, `category`, `indoor` và
   `maxCost`.
3. FEAT-003 không tạo endpoint `/api/v1/places/search` và phải giữ defaults,
   validation, envelope cùng fixed sort của FEAT-001.

FEAT-002 regression suite xác nhận list contract FEAT-001 không đổi.

## Verification

```bash
curl "http://localhost:8080/api/v1/places?page=0&size=20"
curl "http://localhost:8080/api/v1/places?keyword=demo&district=quan%201&category=van-hoa&indoor=true&maxCost=100000"
curl "http://localhost:8080/api/v1/places/demo-art-space"
curl "http://localhost:8080/api/v1/categories"
```

Kết quả xác minh ngày 2026-07-20:

- catalog FEAT-001: HTTP 200 với 5 active places;
- detail active: HTTP 200, đúng 15 field, nested ordering và `HH:mm`;
- missing, inactive và case-mismatch slug: HTTP 404 với `PLACE_NOT_FOUND`;
- category catalog: HTTP 200 root array, sort `name ASC, id ASC`;
- response time detail sau startup là 31 ms trong smoke run cục bộ, dưới mục
  tiêu 500 ms nhưng không được coi là benchmark production.
- Constraint tests bao phủ category slug, open/closed state, time ordering và
  uniqueness của place-category/place-day.
- Hibernate statistics xác nhận detail active dùng đúng 3 SQL queries, không
  tăng theo số category hoặc opening-hour rows.
- FEAT-003 full suite có 39 tests pass; fixture 30 matching places xác nhận
  stable pagination, tối đa 2 SQL/request và local response dưới 500 ms.
- Final JAR smoke xác nhận no-filter và full combination trả 200; invalid input
  (kể cả `indoor=TRUE`) trả 400 với `INVALID_REQUEST`/`fieldErrors`; warm filter
  call quan sát 37 ms.
- Hibernate SQL xác nhận search values dùng bind parameters; response list vẫn
  đúng 7-field envelope/11-field item và detail/categories regression trả 200.

## Frontend consumer — 2026-07-21

Next.js frontend tại `frontend/src/lib/api/place-api.ts` tiêu thụ contract này
cho `/places` và `/places/[slug]`. FEAT-003 dùng trực tiếp
`GET /api/v1/places`; frontend không gọi `/api/v1/places/search` vì route đó
không tồn tại trong source backend.

Backend chưa khai báo CORS. API client chạy server-side trong Next.js với
`BACKEND_API_BASE_URL` (mặc định `http://localhost:8080`), nên browser không gọi
Spring Boot trực tiếp. District select được suy ra từ active catalog
`page=0&size=100`; đây là quyết định MVP, không phải metadata contract mới.
