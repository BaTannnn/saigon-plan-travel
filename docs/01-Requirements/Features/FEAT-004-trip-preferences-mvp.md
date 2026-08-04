---
id: FEAT-004
title: Trip Preferences MVP
aliases:
  - Sở thích và thông tin chuyến đi
  - Đầu vào cho bộ lập lịch MVP
status: ready-for-review
priority: P0
project: SaigonPlanTravel
owner: Nguyễn Bá Tân
created: 2026-07-17
updated: 2026-08-01
target_milestone: MVP 2026-08-30
canonical_path: docs/01-Requirements/Features/FEAT-004-trip-preferences-mvp.md
tags:
  - saigon-plan-travel
  - feature-spec
  - trip
  - preferences
  - backend
  - mvp
related:
  - "[[00-Dashboard/PROJECT_CONTEXT]]"
  - "[[01-Requirements/Features/FEAT-001-place-catalog-api-mvp]]"
  - "[[01-Requirements/Features/FEAT-002-place-detail-category-opening-hours]]"
  - "[[01-Requirements/Features/FEAT-003-place-search-filter-pagination]]"
  - "[[01-Requirements/Features/FEAT-005-basic-itinerary-generation-scheduling-v1]]"
  - "[[02-Architecture/ADR/ADR-001-Modular-Monolith]]"
  - "[[03-Database/Trip-Module-ERD]]"
  - "[[04-API/Trip-API-v1]]"
---

# FEAT-004 — Trip Preferences MVP

> [!summary]
> Xây dựng vertical slice đầu tiên của `Trip Module` để người dùng nhập và lưu
> các ràng buộc của một chuyến đi trong ngày tại TP.HCM. Trip gồm ngày đi,
> khung giờ, ngân sách, điểm xuất phát, nhịp độ, môi trường mong muốn và các
> category yêu thích. Feature này chỉ chuẩn hóa và lưu đầu vào cho
> `FEAT-005 — Basic Itinerary Generation`; chưa chọn địa điểm và chưa sinh lịch
> trình.

## 1. Baseline đã đối chiếu

Tài liệu này được viết lại từ source backend sau FEAT-001–003, không giả định
backend còn ở trạng thái trước FEAT-003.

| Hạng mục | Baseline thực tế ngày 2026-08-01 |
| --- | --- |
| Migration | Đã có `V1` đến `V10`; `V10` tạo bảng `users`, FEAT-004 bắt đầu từ `V11` |
| Place API | `GET /api/v1/places` dùng `PlaceSearchRequest` và `PlacePageResponse` |
| Place detail | `GET /api/v1/places/{slug}` |
| Category catalog | `GET /api/v1/categories`, trả mảng `CategoryResponse(id, name, slug)` |
| Persistence | PostgreSQL, Flyway, `spring.jpa.hibernate.ddl-auto=validate` |
| Kiến trúc | Package-by-feature; Controller → Service → Repository/Mapper |
| DTO | Java `record`; không trả Entity |
| Error | Spring `ProblemDetail` với extension `code`, `fieldErrors` |
| Test | Source snapshot có 39 phương thức `@Test` của FEAT-001–003 |
| Authentication | JWT stateless đã hoàn tất; Trip phải gắn với người dùng hiện tại |
| Module mới | Package `trip` được triển khai theo package-by-feature; `scheduling` thuộc FEAT-005 |

> [!warning]
> File source được cung cấp không có `pom.xml`, Maven Wrapper hoặc toàn bộ
> repository, vì vậy số test trên được kiểm tra từ source nhưng chưa được chạy
> lại trong lần viết tài liệu này. Khi bắt đầu code FEAT-004 phải chạy test từ
> repository backend đầy đủ và ghi kết quả thật.

### 1.1. Contract không được phá vỡ

- `GET /api/v1/places` tiếp tục trả page envelope 7 trường và place summary 11
  trường.
- Search/filter FEAT-003 giữ nguyên query semantics, fixed sort và validation.
- `GET /api/v1/places/{slug}` giữ nguyên detail contract.
- `GET /api/v1/categories` tiếp tục trả root array, sắp xếp
  `name ASC, id ASC`.
- Error hiện có của Place API tiếp tục dùng `application/problem+json`.
- Không sửa V1–V10 đã tồn tại.

## 2. Bối cảnh và vấn đề

Scheduling không thể tạo lịch trình chỉ từ danh sách địa điểm. Hệ thống cần
biết các ràng buộc của người dùng:

- đi ngày nào;
- bắt đầu và kết thúc lúc mấy giờ;
- xuất phát ở đâu;
- tổng ngân sách dự kiến;
- thích nhóm địa điểm nào;
- muốn đi thư thả hay nhanh;
- ưu tiên trong nhà, ngoài trời hay kết hợp.

Nếu FEAT-005 nhận trực tiếp một request chưa được chuẩn hóa, Scheduling Module
sẽ phải đồng thời làm validation, quản lý Trip và chạy thuật toán. Điều đó làm
feature lập lịch quá lớn, khó kiểm thử và khó giải thích trong báo cáo.

FEAT-004 tách phần **thu thập và lưu ràng buộc** thành một aggregate độc lập.
FEAT-005 chỉ đọc một snapshot hợp lệ từ Trip Module để lập lịch.

## 3. Mục tiêu

### 3.1. Mục tiêu sản phẩm

1. Cho phép người dùng tạo một Trip trong ngày.
2. Cho phép mở lại Trip bằng một identifier công khai khó đoán.
3. Cho phép thay thế toàn bộ preferences trước khi sinh hoặc sinh lại itinerary.
4. Trả về các category đã được xác minh tồn tại trong Place Module.
5. Cung cấp input ổn định, đầy đủ cho FEAT-005.

### 3.2. Mục tiêu kỹ thuật

- Tạo `Trip Module` trong Spring Boot modular monolith hiện tại.
- Tạo schema bằng Flyway `V11` và `V12`.
- Dùng `BIGINT` làm khóa nội bộ và `UUID` trong public API.
- Dùng DTO `record`, mapper và controller mỏng theo convention FEAT-001–003.
- Không để Trip Module truy cập `CategoryRepository` hoặc `Category` Entity.
- Validate cross-field rules bằng một policy/domain service thuần.
- Lưu scalar fields và category preferences trong một transaction.
- Cung cấp read contract cho Scheduling Module mà không lộ Trip Entity.
- Giữ toàn bộ FEAT-001–003 regression tests.

### 3.3. Kết quả mong đợi

Sau một request hợp lệ:

1. `POST /api/v1/trips` trả `201 Created`;
2. response có `publicId` và header `Location`;
3. `GET /api/v1/trips/{publicId}` đọc lại đúng Trip;
4. `PUT /api/v1/trips/{publicId}` thay thế toàn bộ editable fields;
5. không có itinerary, routing, AI hoặc context side effect.

## 4. Phạm vi

### 4.1. Trong phạm vi

- Một chuyến đi trong **một ngày**, không qua nửa đêm.
- Các input:
  - `tripDate`;
  - `startTime`, `endTime`;
  - `budget`;
  - `startLocation`;
  - `travelPace`;
  - `environmentPreference`;
  - 1–5 `categorySlugs`.
- Bảng `trips`.
- Bảng `trip_category_preferences`.
- `POST /api/v1/trips`.
- JWT authentication và ownership theo người dùng hiện tại.
- `GET /api/v1/trips/{publicId}`.
- `PUT /api/v1/trips/{publicId}` theo full-replacement semantics.
- Batch category lookup trong Place Module.
- Validation, `ProblemDetail`, unit/web/PostgreSQL integration tests.
- Internal read contract cho FEAT-005.

### 4.2. Ngoài phạm vi

- Chọn place hoặc sinh itinerary.
- Timeline, itinerary item, route, travel time hoặc distance.
- Kiểm tra opening hours theo ngày đi.
- RAG, LLM hoặc explanation.
- Weather, traffic, crowd hoặc time-series.
- Dynamic re-planning.
- Chuyến đi nhiều ngày hoặc qua nửa đêm.
- Danh sách place bắt buộc/phải tránh.
- Category weight hoặc priority ranking.
- Transport mode; FEAT-005 V1 dùng travel estimator riêng.
- Số người đi, accessibility hoặc dietary constraints.
- Anonymous Trip, chia sẻ Trip, phân quyền cộng tác hoặc role-based Trip management.
- List/delete/archive trips.
- Geocoding, reverse geocoding hoặc PostGIS point-in-polygon.
- Frontend implementation.

> [!important]
> `publicId` không phải cơ chế phân quyền. Điểm xuất phát có thể là dữ liệu vị
> trí nhạy cảm; MVP không được log toàn bộ request và giao diện demo nên khuyến
> khích chọn một điểm công cộng gần nơi xuất phát thay vì địa chỉ nhà chính xác.

## 5. Tác nhân và user stories

### 5.1. Tác nhân

- **Người dùng đã đăng nhập:** nhập và chỉnh sửa điều kiện chuyến đi của chính mình.
- **Next.js frontend:** gọi Trip API và category catalog.
- **Trip Module:** sở hữu Trip aggregate và validation.
- **Place Module:** xác minh category theo batch.
- **Scheduling Module:** đọc `TripSchedulingSnapshot` trong FEAT-005.

### 5.2. User stories

**US-001 — Tạo Trip**
Là người dùng đã đăng nhập, tôi muốn lưu các điều kiện chuyến đi để hệ thống có đủ đầu vào
tạo lịch trình.

**US-002 — Chọn sở thích**
Là người dùng đã đăng nhập, tôi muốn chọn các category có thật trong hệ thống để ứng viên
địa điểm sau này phù hợp với sở thích.

**US-003 — Chọn nhịp độ**
Là người dùng đã đăng nhập, tôi muốn chọn `RELAXED`, `BALANCED` hoặc `FAST` để bộ lập lịch
sau này điều chỉnh thời lượng tham quan.

**US-004 — Chọn môi trường**
Là người dùng đã đăng nhập, tôi muốn chọn `INDOOR`, `OUTDOOR` hoặc `MIXED` để hệ thống lọc
ứng viên phù hợp.

**US-005 — Mở và sửa Trip**
Là người dùng đã đăng nhập, tôi muốn mở lại và sửa toàn bộ Trip trước khi tạo itinerary.

## 6. Luồng chính

### 6.1. Tạo Trip

1. Frontend gọi `GET /api/v1/categories` để hiển thị category chips.
2. Người dùng nhập trip preferences.
3. Frontend gửi `POST /api/v1/trips` cùng JWT hợp lệ.
4. Controller lấy `userId` từ `UserPrincipal`, bind JSON và chạy Bean Validation.
5. `TripPolicy` kiểm tra date/time và category duplicates.
6. `TripService` gọi Place Module một lần để resolve toàn bộ category slugs.
7. Nếu có slug không tồn tại, API trả `400 INVALID_CATEGORY_PREFERENCE`.
8. Nếu hợp lệ, service tạo Trip aggregate và lưu trong một transaction.
9. API trả `201`, `Location` và `TripResponse`.

### 6.2. Lấy Trip

1. Frontend gọi `GET /api/v1/trips/{publicId}` cùng JWT hợp lệ.
2. Trip được load bằng `publicId + userId` cùng tập `categoryId`.
3. Place Module resolve các category IDs bằng một batch query.
4. API trả Trip; UUID hợp lệ nhưng không tồn tại hoặc không thuộc người dùng hiện tại trả `404 TRIP_NOT_FOUND`.

### 6.3. Thay thế Trip

1. Frontend gửi full payload tới `PUT /api/v1/trips/{publicId}` cùng JWT hợp lệ.
2. Hệ thống lookup bằng `publicId + userId`, rồi validate payload và category trước khi mutate aggregate.
3. Scalar fields và category IDs được thay trong cùng transaction.
4. `createdAt` được giữ nguyên; `updatedAt` được cập nhật.
5. Nếu bất kỳ bước nào thất bại, transaction rollback và Trip cũ không đổi.

## 7. Quyết định thiết kế đã chốt trong spec

| ID | Quyết định | Lý do |
| --- | --- | --- |
| DEC-001 | Chỉ hỗ trợ one-day trip. | Giảm edge case cho scheduling MVP. |
| DEC-002 | Giữ POST, GET và PUT; chưa có list/delete. | Đủ cho create–review–edit mà không mở rộng CRUD. |
| DEC-003 | Dùng internal `BIGINT` và public `UUID`. | FK/JPA đơn giản; URL không lộ ID tuần tự. |
| DEC-004 | Không có `title` trong FEAT-004. | Title không phải input của thuật toán FEAT-005 và chưa có màn hình list trip. |
| DEC-005 | PUT là full replacement, không PATCH. | Contract dễ hiểu và category update atomic. |
| DEC-006 | Category request là danh sách slug; persistence lưu category ID. | Slug phù hợp frontend, ID phù hợp FK và scheduling. |
| DEC-007 | Category không có weight/rank. | Giữ preference MVP đơn giản và deterministic. |
| DEC-008 | Dùng `@ElementCollection<Set<Long>>` cho category IDs. | Không cần Entity/composite key chỉ để lưu một value set 1–5 phần tử. |
| DEC-009 | Trip Module gọi `CategoryService` batch methods của Place Module. | Tái sử dụng service hiện có; không lộ repository/Entity. |
| DEC-010 | Date validation dùng `Clock` tại `Asia/Ho_Chi_Minh`. | Tránh test phụ thuộc ngày máy chạy. |
| DEC-011 | Không kiểm tra polygon TP.HCM. | Không kéo PostGIS/geocoding vào feature nhập liệu. |
| DEC-012 | Không thêm optimistic locking trong MVP. | Ownership đã được bảo vệ bằng `userId`; cùng một người dùng tạm chấp nhận last-write-wins. |

## 8. Yêu cầu chức năng

| ID | Yêu cầu | Ưu tiên |
| --- | --- | --- |
| FR-001 | Tạo schema mới bằng Flyway V11–V12, không sửa V1–V10. | Must |
| FR-002 | Hibernate `ddl-auto=validate` phải pass. | Must |
| FR-003 | `POST /api/v1/trips` tạo Trip và trả `201 Created`. | Must |
| FR-004 | Create response có `Location: /api/v1/trips/{publicId}`. | Must |
| FR-005 | `GET /api/v1/trips/{publicId}` trả Trip hoặc 404. | Must |
| FR-006 | `PUT /api/v1/trips/{publicId}` thay toàn bộ editable fields hoặc 404. | Must |
| FR-007 | Public Trip API chỉ dùng UUID, không trả internal trip ID. | Must |
| FR-008 | `tripDate` là ngày hiện tại hoặc tương lai theo TP.HCM. | Must |
| FR-009 | `startTime < endTime`; duration từ 60 phút đến 18 giờ. | Must |
| FR-010 | Budget nằm trong `0..100000000` VND. | Must |
| FR-011 | Start location có label và cặp tọa độ hợp lệ. | Must |
| FR-012 | Travel pace chỉ nhận ba enum đã khóa. | Must |
| FR-013 | Environment chỉ nhận ba enum đã khóa. | Must |
| FR-014 | Request có 1–5 category slugs đúng format và không trùng. | Must |
| FR-015 | Tất cả category slugs phải tồn tại trước khi lưu. | Must |
| FR-016 | Category lookup theo slug/ID phải là batch query. | Must |
| FR-017 | Trip Module không import `CategoryRepository` hoặc `Category` Entity. | Must |
| FR-018 | Create/update phải atomic. | Must |
| FR-019 | API dùng DTO record; không serialize Entity/lazy collection. | Must |
| FR-020 | Validation/type/JSON errors trả `ProblemDetail`, không gây 500. | Must |
| FR-021 | Feature không gọi scheduling, routing, context hoặc AI service. | Must |
| FR-022 | FEAT-001–003 API/test contracts không đổi. | Must |
| FR-023 | Trip Module cung cấp snapshot read contract cho FEAT-005. | Must |
| FR-024 | Mọi Trip endpoint yêu cầu người dùng đã xác thực. | Must |
| FR-025 | `userId` lấy từ `UserPrincipal`, không nhận từ request; GET/PUT lookup bằng `publicId + userId`. | Must |

## 9. Quy tắc nghiệp vụ

| ID | Quy tắc |
| --- | --- |
| BR-001 | Một Trip biểu diễn đúng một ngày theo local time TP.HCM. |
| BR-002 | Current date được tính bằng `LocalDate.now(clock)` với clock có zone `Asia/Ho_Chi_Minh`. |
| BR-003 | Trip không qua nửa đêm; start và end thuộc cùng `tripDate`. |
| BR-004 | Time window hợp lệ khi duration thuộc `[60 phút, 18 giờ]`. |
| BR-005 | Budget là tổng ngân sách ước tính cho một người trong MVP. |
| BR-006 | Budget bằng 0 hợp lệ và FEAT-005 chỉ được chọn place có `minCost=0`. |
| BR-007 | Latitude thuộc `[-90,90]`, longitude thuộc `[-180,180]`. |
| BR-008 | Label được trim; sau trim phải khác rỗng và dài tối đa 255 ký tự. |
| BR-009 | `RELAXED/BALANCED/FAST` chỉ lưu preference; multiplier thuộc FEAT-005. |
| BR-010 | `INDOOR/OUTDOOR/MIXED` chỉ lưu preference; filtering thuộc FEAT-005. |
| BR-011 | Category slug dùng exact lowercase format giống FEAT-002/003. |
| BR-012 | Category request order không biểu thị độ ưu tiên. |
| BR-013 | Duplicate category là invalid input, không tự loại trùng im lặng. |
| BR-014 | Unknown category là 400 business validation error. |
| BR-015 | Response category sắp xếp `name ASC, id ASC`, giống category catalog. |
| BR-016 | PUT không upsert; publicId không tồn tại trả 404. |
| BR-017 | UUID không phải authorization; mọi thao tác phải kiểm tra `publicId + userId`, và request vị trí không được log nguyên văn. |

## 10. API contract

### 10.1. Request dùng chung cho POST và PUT

```json
{
  "tripDate": "2026-08-20",
  "startTime": "08:00",
  "endTime": "18:00",
  "budget": 500000.00,
  "startLocation": {
    "label": "Chợ Bến Thành, Quận 1",
    "latitude": 10.7726400,
    "longitude": 106.6980500
  },
  "travelPace": "BALANCED",
  "environmentPreference": "MIXED",
  "categorySlugs": [
    "van-hoa",
    "nghe-thuat"
  ]
}
```

DTO đề xuất:

```java
public record SaveTripRequest(
        @NotNull LocalDate tripDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull
        @Digits(integer = 9, fraction = 2)
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "100000000.00")
        BigDecimal budget,
        @NotNull @Valid StartLocationRequest startLocation,
        @NotNull TravelPace travelPace,
        @NotNull EnvironmentPreference environmentPreference,
        @NotNull @Size(min = 1, max = 5)
        List<
                @NotBlank
                @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
                String
        > categorySlugs
) {}
```

> [!important]
> Request không chứa `userId`. Controller lấy internal user ID từ `UserPrincipal`;
> client không được tự khai báo chủ sở hữu Trip. Mọi endpoint trong mục 10 yêu
> cầu JWT hợp lệ.

```java
public record StartLocationRequest(
        @NotBlank @Size(max = 255) String label,
        @NotNull
        @Digits(integer = 3, fraction = 7)
        @DecimalMin(value = "-90.0000000")
        @DecimalMax(value = "90.0000000")
        BigDecimal latitude,
        @NotNull
        @Digits(integer = 3, fraction = 7)
        @DecimalMin(value = "-180.0000000")
        @DecimalMax(value = "180.0000000")
        BigDecimal longitude
) {}
```

### 10.2. Validation matrix

| Field | Required | Validation |
| --- | --- | --- |
| `tripDate` | Yes | ISO `yyyy-MM-dd`; current/future date theo TP.HCM |
| `startTime` | Yes | ISO `HH:mm`; trước `endTime` |
| `endTime` | Yes | ISO `HH:mm`; window 60 phút–18 giờ |
| `budget` | Yes | Decimal `0..100000000`, tối đa 2 chữ số thập phân ở persistence |
| `startLocation.label` | Yes | Non-blank sau trim, tối đa 255 |
| `startLocation.latitude` | Yes | `-90..90` |
| `startLocation.longitude` | Yes | `-180..180` |
| `travelPace` | Yes | `RELAXED`, `BALANCED`, `FAST` |
| `environmentPreference` | Yes | `INDOOR`, `OUTDOOR`, `MIXED` |
| `categorySlugs` | Yes | 1–5 phần tử, exact slug, không duplicate, đều tồn tại |

### 10.3. Tạo Trip

```http
POST /api/v1/trips
Content-Type: application/json
```

```http
HTTP/1.1 201 Created
Location: /api/v1/trips/7a674ef0-57c8-4d0e-b99b-dccfd342fc98
Content-Type: application/json
```

### 10.4. Lấy Trip

```http
GET /api/v1/trips/{publicId}
Accept: application/json
```

Thành công trả `200 OK`. UUID đúng format nhưng không tồn tại trả 404.

### 10.5. Thay thế Trip

```http
PUT /api/v1/trips/{publicId}
Content-Type: application/json
```

- Body phải đầy đủ giống POST.
- Thành công trả `200 OK`.
- Không upsert.
- Scalar fields và category set được thay atomically.

### 10.6. Response thành công

```json
{
  "publicId": "7a674ef0-57c8-4d0e-b99b-dccfd342fc98",
  "tripDate": "2026-08-20",
  "startTime": "08:00",
  "endTime": "18:00",
  "budget": 500000.00,
  "startLocation": {
    "label": "Chợ Bến Thành, Quận 1",
    "latitude": 10.7726400,
    "longitude": 106.6980500
  },
  "travelPace": "BALANCED",
  "environmentPreference": "MIXED",
  "categoryPreferences": [
    {
      "id": 3,
      "name": "Nghệ thuật",
      "slug": "nghe-thuat"
    },
    {
      "id": 1,
      "name": "Văn hóa",
      "slug": "van-hoa"
    }
  ],
  "createdAt": "2026-07-24T08:00:00+07:00",
  "updatedAt": "2026-07-24T08:00:00+07:00"
}
```

Response DTO đề xuất:

```java
public record TripResponse(
        UUID publicId,
        LocalDate tripDate,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        BigDecimal budget,
        StartLocationResponse startLocation,
        TravelPace travelPace,
        EnvironmentPreference environmentPreference,
        List<CategoryResponse> categoryPreferences,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
```

`CategoryResponse` là contract 3 trường hiện có của Place Module. Trip API không
trả internal numeric `Trip.id`.

### 10.7. Error contract

Mọi lỗi dùng `application/problem+json`, giữ cấu trúc của
`GlobalExceptionHandler` hiện tại.

| Trường hợp | HTTP | `code` |
| --- | --- | --- |
| JSON/date/time/enum/UUID sai kiểu | 400 | `INVALID_REQUEST` |
| Bean/cross-field validation fail | 400 | `INVALID_REQUEST` |
| Category slug không tồn tại | 400 | `INVALID_CATEGORY_PREFERENCE` |
| Trip UUID hợp lệ nhưng không tồn tại | 404 | `TRIP_NOT_FOUND` |
| Lỗi bất ngờ | 500 | Không lộ thông tin nội bộ |

Ví dụ unknown category:

```json
{
  "type": "about:blank",
  "title": "Invalid category preference",
  "status": 400,
  "detail": "One or more category preferences do not exist",
  "instance": "/api/v1/trips",
  "code": "INVALID_CATEGORY_PREFERENCE",
  "unknownCategorySlugs": [
    "khong-ton-tai"
  ]
}
```

Ví dụ trip không tồn tại:

```json
{
  "type": "about:blank",
  "title": "Trip not found",
  "status": 404,
  "detail": "Trip not found",
  "instance": "/api/v1/trips/7a674ef0-57c8-4d0e-b99b-dccfd342fc98",
  "code": "TRIP_NOT_FOUND"
}
```

### 10.8. Thay đổi cần thiết trong error handler

`GlobalExceptionHandler` hiện coi mọi
`MethodArgumentTypeMismatchException` là lỗi pagination. FEAT-004 phải refactor
nhánh này:

- nếu field là `page` hoặc `size`, giữ behavior FEAT-001/003;
- nếu field khác, trả title `Invalid request`, code `INVALID_REQUEST` và field
  error tương ứng;
- thêm handler cho `HttpMessageNotReadableException` để invalid enum/date/JSON
  trả 400 thay vì rơi vào generic 500;
- không thay đổi các test error hiện có của Place API.

## 11. Đặc tả dữ liệu

### 11.1. Migration `V11__create_trips_table.sql`

| Cột | PostgreSQL | Null | Ghi chú |
| --- | --- | --- | --- |
| `id` | `BIGSERIAL` | No | PK nội bộ, nhất quán với Place |
| `public_id` | `UUID` | No | Unique, sinh ở application |
| `user_id` | `BIGINT` | No | FK → `users(id)`, owner của Trip |
| `trip_date` | `DATE` | No | Ngày nghiệp vụ TP.HCM |
| `start_time` | `TIME` | No | Local time |
| `end_time` | `TIME` | No | Local time |
| `budget` | `NUMERIC(12,2)` | No | `0..100000000` |
| `start_location_label` | `VARCHAR(255)` | No | Non-blank |
| `start_latitude` | `NUMERIC(10,7)` | No | `-90..90` |
| `start_longitude` | `NUMERIC(10,7)` | No | `-180..180` |
| `travel_pace` | `VARCHAR(20)` | No | Enum string |
| `environment_preference` | `VARCHAR(20)` | No | Enum string |
| `created_at` | `TIMESTAMPTZ` | No | App supplies; DB có default an toàn |
| `updated_at` | `TIMESTAMPTZ` | No | App cập nhật bằng injected Clock |

DDL ràng buộc đề xuất:

```sql
CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    trip_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    budget NUMERIC(12, 2) NOT NULL,
    start_location_label VARCHAR(255) NOT NULL,
    start_latitude NUMERIC(10, 7) NOT NULL,
    start_longitude NUMERIC(10, 7) NOT NULL,
    travel_pace VARCHAR(20) NOT NULL,
    environment_preference VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_trips_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_trips_time_order
        CHECK (start_time < end_time),
    CONSTRAINT chk_trips_duration
        CHECK (
            end_time - start_time >= INTERVAL '1 hour'
            AND end_time - start_time <= INTERVAL '18 hours'
        ),
    CONSTRAINT chk_trips_budget
        CHECK (budget BETWEEN 0 AND 100000000),
    CONSTRAINT chk_trips_location_label
        CHECK (BTRIM(start_location_label) <> ''),
    CONSTRAINT chk_trips_latitude
        CHECK (start_latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_trips_longitude
        CHECK (start_longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_trips_travel_pace
        CHECK (travel_pace IN ('RELAXED', 'BALANCED', 'FAST')),
    CONSTRAINT chk_trips_environment_preference
        CHECK (
            environment_preference IN ('INDOOR', 'OUTDOOR', 'MIXED')
        ),
    CONSTRAINT chk_trips_timestamp_order
        CHECK (updated_at >= created_at)
);

CREATE INDEX idx_trips_user_id
    ON trips (user_id);
```

Không đặt `trip_date >= CURRENT_DATE` ở database vì `CURRENT_DATE` phụ thuộc
timezone của DB session và check constraint theo thời gian không phù hợp với dữ
liệu đã lưu. Quy tắc này thuộc application policy.

### 11.2. Migration `V12__create_trip_category_preferences_table.sql`

```sql
CREATE TABLE trip_category_preferences (
    trip_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,

    CONSTRAINT pk_trip_category_preferences
        PRIMARY KEY (trip_id, category_id),
    CONSTRAINT fk_trip_category_preferences_trip
        FOREIGN KEY (trip_id)
        REFERENCES trips(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_trip_category_preferences_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_trip_category_preferences_category_trip
    ON trip_category_preferences (category_id, trip_id);
```

- Không seed Trip hoặc user preferences bằng Flyway.
- Database ngăn duplicate bằng composite PK.
- Giới hạn 1–5 category là aggregate/application invariant; PostgreSQL không
  dùng trigger chỉ để đếm số row trong MVP.

### 11.3. JPA mapping

`Trip` là aggregate root. Category IDs là value collection, không phải quan hệ
JPA tới `Category`.

```java
@ElementCollection(fetch = FetchType.LAZY)
@CollectionTable(
        name = "trip_category_preferences",
        joinColumns = @JoinColumn(name = "trip_id")
)
@Column(name = "category_id", nullable = false)
private Set<Long> preferredCategoryIds = new HashSet<>();
```

Quy tắc:

- Không import `Category` Entity trong package `trip`.
- Không tạo `TripCategoryPreference` Entity/composite key nếu value collection
  đáp ứng schema.
- `TripRepository` cung cấp lookup theo `publicId + userId`; public API không dùng lookup chỉ theo `publicId`.
- GET/PUT phải load category IDs trước khi transaction kết thúc, bằng
  `@EntityGraph` hoặc query phù hợp; không dựa vào Open Session in View vì
  `open-in-view=false`.
- Entity không dùng Lombok `@Data` hoặc setter công khai cho toàn bộ field.
- State thay đổi qua constructor/factory và `replaceDetails(...)`.

## 12. Kiến trúc và module boundary

### 12.1. Package đề xuất

```text
com.saigonplantravel.backend
├── common
│   ├── config
│   │   └── TimeConfig.java
│   └── error
│       └── GlobalExceptionHandler.java
├── place
│   ├── repository
│   │   └── CategoryRepository.java
│   └── service
│       └── CategoryService.java
└── trip
    ├── controller
    │   └── TripController.java
    ├── domain
    │   ├── EnvironmentPreference.java
    │   ├── TravelPace.java
    │   └── TripPolicy.java
    ├── dto
    │   ├── SaveTripRequest.java
    │   ├── StartLocationRequest.java
    │   ├── StartLocationResponse.java
    │   └── TripResponse.java
    ├── entity
    │   └── Trip.java
    ├── exception
    │   ├── InvalidCategoryPreferenceException.java
    │   ├── InvalidTripException.java
    │   └── TripNotFoundException.java
    ├── mapper
    │   └── TripMapper.java
    ├── repository
    │   └── TripRepository.java
    └── service
        ├── TripSchedulingQuery.java
        ├── TripSchedulingSnapshot.java
        └── TripService.java
```

Tên package có thể điều chỉnh nhẹ theo convention repository đầy đủ, nhưng
không được thay đổi module ownership.

### 12.2. Batch category lookup

Mở rộng `CategoryRepository` bằng hai query batch:

```java
List<Category> findAllBySlugIn(Set<String> slugs);

List<Category> findAllByIdIn(Set<Long> ids);
```

Mở rộng `CategoryService`:

```java
public List<CategoryResponse> findBySlugs(Set<String> slugs);

public List<CategoryResponse> findByIds(Set<Long> ids);
```

Yêu cầu:

- một call cho toàn bộ set, không loop query;
- service trả DTO, không trả Entity;
- kết quả response sắp xếp `name ASC, id ASC`;
- Trip Module chỉ phụ thuộc `CategoryService` và `CategoryResponse`;
- category catalog endpoint hiện có không đổi.

### 12.3. Read contract cho FEAT-005

```java
public interface TripSchedulingQuery {
    TripSchedulingSnapshot getByPublicId(UUID publicId, Long userId);
}
```

```java
public record TripSchedulingSnapshot(
        Long tripId,
        UUID publicId,
        LocalDate tripDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal budget,
        BigDecimal startLatitude,
        BigDecimal startLongitude,
        TravelPace travelPace,
        EnvironmentPreference environmentPreference,
        Set<Long> preferredCategoryIds,
        OffsetDateTime updatedAt
) {}
```

- `tripId` chỉ dùng nội bộ cho FK của Scheduling Module.
- `userId` chỉ dùng để kiểm tra ownership tại Trip boundary, không xuất hiện trong snapshot/public API.
- Contract không trả Trip Entity hoặc TripRepository.
- FEAT-004 chỉ tạo contract/snapshot; không tạo itinerary.

### 12.4. Transaction boundaries

- `createTrip`: write transaction.
- `getTrip`: read-only transaction.
- `replaceTrip`: write transaction.
- Validate category existence trước khi persist/mutate aggregate.
- Mapping response và resolve category DTO phải hoàn tất trong service, không
  để controller truy cập lazy collection.
- Không có network/external API call trong transaction.

## 13. Yêu cầu phi chức năng

| ID | Yêu cầu |
| --- | --- |
| NFR-001 | Create/update atomic; không tồn tại partial trip/preferences. |
| NFR-002 | Category lookup dùng batch query; không N+1 theo số category. |
| NFR-003 | API local warm-up với 1–5 categories có mục tiêu dưới 500 ms; ghi rõ môi trường đo. |
| NFR-004 | Không log full request, start label hoặc tọa độ chính xác. |
| NFR-005 | Không trả Entity, internal Trip/User ID, SQL, stack trace hoặc connection data. |
| NFR-006 | Test database semantics trên PostgreSQL/Testcontainers, không chỉ H2. |
| NFR-007 | UUID sinh bằng `UUID.randomUUID()` hoặc JDK generator tương đương. |
| NFR-008 | Date validation và timestamps dùng injected `Clock`. |
| NFR-009 | `./mvnw clean test` và `./mvnw clean verify` từ repository đầy đủ phải pass. |
| NFR-010 | Không thêm dependency mới nếu JDK/Spring/JPA hiện tại đã đáp ứng. |

## 14. Tiêu chí chấp nhận

### AC-001 — Migration nối tiếp

**Given** database đang ở V10
**When** backend khởi động
**Then** V11–V12 chạy đúng một lần, Hibernate validate thành công và V1–V10
không bị sửa.

### AC-002 — Create happy path

**Given** request hợp lệ và hai category đều tồn tại
**When** gọi `POST /api/v1/trips`
**Then** API trả 201, header `Location`, response đúng contract và lưu một Trip
cùng hai category preferences.

### AC-003 — Không lộ internal Trip ID

**Given** Trip đã lưu
**When** xem POST/GET/PUT response
**Then** có UUID `publicId` nhưng không có numeric `id`/`tripId`.

### AC-004 — Date theo TP.HCM

**Given** fixed Clock tại `Asia/Ho_Chi_Minh`
**When** tripDate là hôm qua, hôm nay hoặc tương lai
**Then** hôm qua trả 400; hôm nay và tương lai hợp lệ.

### AC-005 — Time window

**Given** các case bằng nhau, đảo ngược, 59 phút, 60 phút, 18 giờ và trên 18 giờ
**When** create/PUT
**Then** chỉ window từ 60 phút đến 18 giờ và có start trước end được chấp nhận.

### AC-006 — Budget

**Given** budget `0`, `100000000`, âm và vượt trần
**When** create/PUT
**Then** hai boundary hợp lệ; giá trị ngoài khoảng trả 400 và DB constraint
cũng từ chối SQL trực tiếp.

### AC-007 — Start location

**Given** label blank, latitude/longitude ngoài phạm vi hoặc dữ liệu hợp lệ
**When** create/PUT
**Then** dữ liệu sai trả 400; label hợp lệ được trim và tọa độ giữ precision.

### AC-008 — Enum và JSON parsing

**Given** invalid enum, invalid date/time hoặc malformed JSON
**When** gọi API
**Then** trả `400 INVALID_REQUEST` dạng `ProblemDetail`, không trả 500.

### AC-009 — Category count, format và duplicate

**Given** danh sách 0, 1, 5, 6 phần tử; slug sai format; duplicate slug
**When** create/PUT
**Then** 1 và 5 hợp lệ khi tồn tại; các case còn lại trả 400.

### AC-010 — Unknown category

**Given** một slug tồn tại và một slug không tồn tại
**When** create/PUT
**Then** trả `400 INVALID_CATEGORY_PREFERENCE`, liệt kê unknown slug theo thứ
tự ổn định và không ghi/mutate Trip.

### AC-011 — Batch lookup và boundary

**Given** request có 5 category slugs
**When** create/GET/PUT
**Then** mỗi hướng lookup dùng một batch repository call; Trip Module không
import Category Entity/Repository.

### AC-012 — GET

**Given** Trip tồn tại
**When** GET bằng publicId
**Then** trả 200 và category preferences sắp xếp `name ASC, id ASC`.

### AC-013 — Malformed và missing UUID

**Given** một path không phải UUID và một UUID hợp lệ không tồn tại
**When** GET/PUT
**Then** malformed UUID trả `400 INVALID_REQUEST`; missing UUID trả
`404 TRIP_NOT_FOUND`; lỗi pagination FEAT-001/003 không đổi.

### AC-014 — PUT full replacement

**Given** Trip có dữ liệu và category cũ
**When** PUT payload hợp lệ
**Then** mọi editable field và category set được thay; `createdAt` giữ nguyên,
`updatedAt` tăng.

### AC-015 — PUT rollback

**Given** Trip hiện có
**When** PUT thất bại do validation, unknown category hoặc persistence
**Then** GET sau đó vẫn trả toàn bộ snapshot cũ.

### AC-016 — Database constraints

**Given** SQL trực tiếp vi phạm UUID unique, time, duration, budget, label,
coordinate, enum, FK hoặc duplicate composite PK
**When** insert/update
**Then** PostgreSQL từ chối bằng named constraint.

### AC-017 — Scheduling snapshot

**Given** Trip hợp lệ
**When** gọi internal `TripSchedulingQuery`
**Then** snapshot có đủ internal ID, date/window, budget, origin, pace,
environment, category IDs và updatedAt nhưng không trả Entity.

### AC-018 — Không side effect

**Given** POST/PUT thành công
**When** kiểm tra dependency calls và database
**Then** không tạo itinerary và không gọi routing, weather, RAG hoặc LLM.

### AC-019 — Regression FEAT-001–003

**Given** FEAT-004 đã triển khai
**When** chạy full tests
**Then** 39 test methods baseline cùng test mới pass; Place/list/detail/category
contracts không đổi.

### AC-020 — Authentication và ownership

**Given** hai người dùng A và B, Trip thuộc A
**When** A gọi POST/GET/PUT và B gọi GET/PUT bằng cùng `publicId`
**Then** A thao tác thành công; B nhận `404 TRIP_NOT_FOUND`; request không có `userId` và repository lookup dùng `publicId + userId`.

### AC-021 — Full verification

**Given** implementation hoàn tất
**When** chạy clean test, clean verify, migration/startup và smoke tests
**Then** mọi bước pass và evidence được ghi trong development log.

## 15. Ma trận truy vết

| Nhóm | Acceptance criteria | Bằng chứng |
| --- | --- | --- |
| Schema/persistence | AC-001, AC-016 | Flyway log, PostgreSQL integration tests |
| API/identity | AC-002, AC-003, AC-012–AC-014, AC-020 | Web/service tests, curl |
| Validation | AC-004–AC-010 | Policy, controller và DB tests |
| Module boundary | AC-011, AC-017 | Unit tests, import review |
| Transaction | AC-010, AC-014, AC-015 | Service/integration tests |
| Scope/regression | AC-018–AC-021 | Mockito verification, full suite, smoke log |

## 16. Kế hoạch triển khai theo từng file

> [!warning]
> Tên file dưới đây dựa trên source snapshot hiện tại. Trước khi code phải đọc
> repository đầy đủ, root `AGENTS.md`, `PROJECT_CONTEXT.md` và migration history
> thực tế.

### Phase A — Preflight

- [ ] Xác nhận working tree và giữ các thay đổi chưa commit của người dùng.
- [ ] Chạy `./mvnw clean test` từ backend đầy đủ; ghi số test baseline thật.
- [ ] Kiểm tra `flyway_schema_history`; xác nhận latest version là V10.
- [ ] Kiểm tra authentication hiện tại, `UserPrincipal.id` và migration `V10__create_users_table.sql`.
- [ ] Duyệt request/response/enums/one-day scope và chuyển spec thành `approved`.

### Phase B — Flyway

- [ ] Tạo `V11__create_trips_table.sql` với `user_id` và FK tới `users`.
- [ ] Tạo `V12__create_trip_category_preferences_table.sql`.
- [ ] Không chỉnh sửa V1–V10.
- [ ] Test clean migration, V10→V12, restart và Hibernate validate.
- [ ] Test named constraints bằng PostgreSQL.

### Phase C — Place Module batch lookup

- [ ] Mở rộng `CategoryRepository` bằng `findAllBySlugIn` và `findAllByIdIn`.
- [ ] Mở rộng `CategoryService` bằng hai batch methods trả `CategoryResponse`.
- [ ] Giữ `getCategories()` và `GET /api/v1/categories` không đổi.
- [ ] Thêm service/repository tests cho empty/input subset/sort.

### Phase D — Trip domain và persistence

- [ ] Tạo `TravelPace` và `EnvironmentPreference`.
- [ ] Tạo `TripPolicy` cho date/time/duplicates.
- [ ] Tạo `Trip` Entity với category ID `@ElementCollection`.
- [ ] Tạo `TripRepository` lookup bằng `publicId + userId` và fetch category IDs.
- [ ] Tạo `TimeConfig`/Clock bean nếu chưa có.
- [ ] Không import Category Entity/Repository vào Trip Module.

### Phase E — DTO, mapper và service

- [ ] Tạo request/response DTO records.
- [ ] Tạo `TripMapper`.
- [ ] Tạo exceptions cho invalid Trip/category và trip not found.
- [ ] Tạo `TripService#createTrip`, `getTrip`, `replaceTrip`.
- [ ] Resolve unknown categories trước khi persist/mutate.
- [ ] Tạo `TripSchedulingQuery` và immutable snapshot.

### Phase F — Controller và common errors

- [ ] Tạo `TripController`.
- [ ] Implement POST 201 + Location, GET 200/404, PUT 200/404.
- [ ] Refactor type-mismatch handling nhưng giữ pagination error regression.
- [ ] Handle invalid JSON/date/time/enum bằng 400.
- [ ] Map Trip exceptions sang exact `ProblemDetail` contract.

### Phase G — Automated tests

- [ ] `TripPolicyTest`: date, duration, duplicates.
- [ ] `TripTest`: create/replace, timestamp và category set.
- [ ] `TripServiceTest`: create/get/replace, ownership, batch lookup, rollback, no side effect.
- [ ] `TripMapperTest`: exact response và no internal ID.
- [ ] `TripControllerTest`: POST/GET/PUT, Location, JSON, 400/404.
- [ ] PostgreSQL integration: V11–V12, constraints, collection mapping, rollback.
- [ ] Error regression: pagination và Place not-found responses giữ nguyên.
- [ ] Chạy toàn bộ FEAT-001–004 tests.

### Phase H — Smoke test và tài liệu

- [ ] POST một Trip hợp lệ.
- [ ] GET theo `Location`.
- [ ] PUT đổi time/budget/category rồi GET lại.
- [ ] Gọi past date, invalid window, malformed enum/UUID và unknown category.
- [ ] Kiểm tra không có itinerary/AI/context rows hoặc calls.
- [ ] Cập nhật Trip ERD, Trip API, PROJECT_CONTEXT và development log.
- [ ] Review diff, migration versions và secret/location logging.

## 17. Chiến lược kiểm thử

### 17.1. Unit/domain

- Fixed Clock tại biên ngày TP.HCM.
- Duration 59/60 phút và 18 giờ/trên 18 giờ.
- Duplicate category trước khi chuyển List thành Set.
- Full replacement và timestamp.
- Scheduling snapshot mapping.

### 17.2. Web slice

- Request/response field names và exact time format.
- POST 201 + Location và owner lấy từ principal.
- GET/PUT 200, 400, 404 và cross-user ownership.
- Invalid enum/date/JSON không gây 500.
- `application/problem+json`, `code` và `fieldErrors`.
- Không trả `id` nội bộ.

### 17.3. Service

- Batch lookup theo slug và ID.
- Unknown slug set difference đúng và sort ổn định.
- Không mutate trước khi validation hoàn tất.
- Rollback khi persistence lỗi.
- Không gọi Scheduling/Routing/AI dependencies.

### 17.4. PostgreSQL integration

- Flyway V1–V12 trên database sạch.
- Upgrade V10→V12.
- Hibernate validate.
- Mọi named constraint ở mục 11.
- `@ElementCollection` insert/load/replace.
- FK delete behavior và composite PK.

### 17.5. Regression

- 39 test methods hiện có phải tiếp tục pass.
- Place page 7 fields/summary 11 fields không đổi.
- Place detail/category ordering không đổi.
- Search `unaccent`, wildcard, filter, pagination và ProblemDetail không đổi.

## 18. Definition of Done

- [x] Spec được owner duyệt và status chuyển `approved` trước khi code.
- [x] Feature không vượt phạm vi mục 4.
- [x] V11–V12 chạy trên clean DB và V10 database; V1–V10 không đổi.
- [x] Hibernate validate pass.
- [x] POST/GET/PUT đúng request, response, status và error contract.
- [x] Date/time/budget/location/enum/category rules đúng.
- [x] UUID public và internal Trip/user ID không xuất hiện ở public API.
- [x] Mọi endpoint yêu cầu authentication; cross-user GET/PUT trả 404.
- [x] Category lookup là batch và không có dependency Trip → Category Entity/Repository.
- [x] PUT atomic và rollback test pass.
- [x] `TripSchedulingSnapshot` đủ input cho FEAT-005.
- [x] Không scheduling/routing/context/AI side effect.
- [x] PostgreSQL integration và FEAT-001–003 regression pass.
- [x] `./mvnw clean test` và `./mvnw clean verify` pass.
- [x] Startup và smoke tests pass.
- [x] Không log request vị trí hoặc lộ secret/stack trace.
- [x] Trip ERD, Trip API, development log và PROJECT_CONTEXT được cập nhật.

## 19. Bàn giao cho FEAT-005

FEAT-004 được coi là đủ đầu vào cho Scheduling V1 khi
`TripSchedulingSnapshot` cung cấp:

- `tripId`, `publicId`;
- `tripDate`;
- `startTime`, `endTime`;
- `budget`;
- start latitude/longitude;
- `travelPace`;
- `environmentPreference`;
- `preferredCategoryIds`;
- `updatedAt`.

FEAT-005 chịu trách nhiệm:

- query active place candidates;
- category/environment eligibility;
- opening-hours interpretation;
- travel estimate;
- visit duration multiplier;
- scoring/greedy scheduling;
- itinerary persistence và response.

Ranh giới này bảo đảm FEAT-004 không chứa logic lập lịch và FEAT-005 chỉ đọc snapshot Trip đã được xác thực ownership.

## 20. Liên kết với giao diện và báo cáo khóa luận

### 20.1. Prototype

- Nút `Tạo lịch trình` mở form preferences.
- Category chips lấy từ `GET /api/v1/categories`, không hard-code slug.
- Map picker/current location cung cấp label và coordinates.
- Form hiển thị đúng validation limits trong mục 10.2.
- Sau POST, frontend giữ `publicId`/`Location` để gọi FEAT-005.
- Timeline và map kết quả thuộc FEAT-005/006, không thuộc FEAT-004.

### 20.2. Báo cáo khóa luận

**Chương Phân tích và thiết kế hệ thống**

- Mô hình đầu vào cho bài toán lập lịch có ràng buộc.
- Trip aggregate và quan hệ với category.
- One-day assumption.
- Module boundary giữa Trip, Place và Scheduling.
- API contract và validation rules.

**Chương Cài đặt và triển khai**

- Flyway migration-first.
- DTO/Entity separation.
- UUID public identity.
- Transactional replacement của aggregate.
- Clock-based date validation.
- Batch lookup để tránh N+1.

**Chương Thực nghiệm và đánh giá**

- Validation matrix.
- PostgreSQL constraint tests.
- Transaction rollback.
- Regression tests FEAT-001–003.
- Query count và latency trong môi trường local.

FEAT-004 chưa phải nội dung RAG hoặc time-series. Hai phần đó được trình bày ở
feature/chương tương ứng sau khi scheduling baseline hoàn tất.

## 21. Gợi ý ghi chú Obsidian

Sau khi triển khai, tạo hoặc cập nhật:

```text
docs/
├── 01-Requirements/Features/
│   └── FEAT-004-trip-preferences-mvp.md
├── 03-Database/
│   └── Trip-Module-ERD.md
├── 04-API/
│   └── Trip-API-v1.md
├── 05-Technical-Knowledge/
│   ├── JPA-ElementCollection.md
│   ├── Spring-ProblemDetail.md
│   └── Clock-and-Timezone-Testing.md
└── 09-Development-Log/
    └── YYYY-MM-DD-FEAT-004-implementation.md
```

Liên kết nên có:

- FEAT-004 ↔ Trip ERD.
- FEAT-004 ↔ Trip API.
- FEAT-004 ↔ FEAT-005.
- Development log ↔ lỗi migration/validation gặp thực tế.
- Knowledge notes ↔ đoạn code thực tế sử dụng kiến thức đó.

## 22. Lịch sử thay đổi

| Ngày | Phiên bản | Thay đổi |
| --- | --- | --- |
| 2026-07-17 | 0.1 | Đặc tả ban đầu. |
| 2026-07-24 | 1.0 | Viết lại theo source FEAT-001–003 thực tế: ProblemDetail, batch CategoryService, `@ElementCollection` và Trip scheduling snapshot. |
| 2026-08-01 | 1.1 | Chuẩn hóa toàn bộ thuật ngữ về `Trip`; dùng `SaveTripRequest`, `TripResponse`, `TripPolicy`, `InvalidTripException`, `createTrip/getTrip/replaceTrip`, đồng thời cập nhật authentication/ownership và migration V11–V12 sau V10 users. |
