# PROJECT_CONTEXT — SaigonPlanTravel

> Nguồn ngữ cảnh chính thức dành cho ChatGPT, Codex và các coding agent.
> File này được ưu tiên hơn nội dung chat cũ nếu có mâu thuẫn.
> Không lưu mật khẩu, API key, access token hoặc dữ liệu bí mật trong file này.

---

## 1. Cách agent phải sử dụng file này

Trước khi lập kế hoạch hoặc sửa code, agent phải đọc:

1. `AGENTS.md` ở root.
2. `docs/00-Dashboard/PROJECT_CONTEXT.md`.
3. File `AGENTS.md` gần thư mục đang làm việc nhất.
4. Feature spec liên quan trong `docs/01-Requirements/Features/`.
5. Tài liệu database, API hoặc ADR liên quan.

Agent không được tự mở rộng phạm vi dự án nếu chưa được phê duyệt.

Khi thông tin chưa rõ hoặc có khả năng ảnh hưởng kiến trúc, database, API hay phạm vi MVP, phải dùng `$grill-with-docs` trước khi triển khai.

---

## 2. Thông tin người thực hiện

- Sinh viên năm cuối ngành Công nghệ thông tin.
- Định hướng Software Engineer hoặc AI Engineer.
- Kỹ năng hiện có:
  - Java, Spring MVC, Spring Boot.
  - PostgreSQL.
  - Python cơ bản, Flask/Django.
  - JavaScript, ReactJS.
  - Machine Learning cơ bản.
- Chưa mạnh Deep Learning.
- Mục tiêu là hoàn thành một đồ án có thể triển khai, demo và bảo vệ được.
- Dùng Obsidian; thư mục `docs/` là Obsidian Vault và là nơi lưu tài liệu chính thức.

---

## 3. Tên dự án

### Tên tiếng Việt

**Hệ thống lập lịch du lịch thông minh tại Thành phố Hồ Chí Minh sử dụng truy xuất tăng cường, dữ liệu theo thời gian và cơ chế tái lập lịch linh động**

### Tên tiếng Anh

**An Intelligent Travel Itinerary Planning System for Ho Chi Minh City Using Retrieval-Augmented Generation, Time-Based Data Analysis, and Dynamic Replanning**

### Tên source code

**SaigonPlanTravel**

---

## 4. Bài toán

Khách du lịch tại TP.HCM phải tự tổng hợp dữ liệu từ nhiều nguồn như:

- Địa điểm.
- Vị trí.
- Giờ mở cửa.
- Chi phí.
- Thời gian tham quan.
- Khoảng cách.
- Thời gian di chuyển.
- Thời tiết.
- Giao thông.
- Mức độ đông đúc.

Lịch trình thủ công thường không xét đầy đủ sở thích, ngân sách, khung giờ, vị trí xuất phát và điều kiện thay đổi theo thời gian.

Hệ thống cần:

1. Gợi ý địa điểm phù hợp.
2. Tạo lịch trình khả thi.
3. Giải thích kết quả bằng RAG dựa trên dữ liệu đã truy xuất.
4. Đánh giá dữ liệu bối cảnh theo thời gian.
5. Đề xuất tái lập lịch khi điều kiện thực tế thay đổi.

---

## 5. Mục tiêu

### Mục tiêu tổng quát

Xây dựng hệ thống web hỗ trợ lập lịch trình du lịch tại TP.HCM dựa trên sở thích, ngân sách, thời gian, vị trí và dữ liệu bối cảnh; đồng thời có khả năng giải thích đề xuất và hỗ trợ tái lập lịch.

### Mục tiêu cụ thể

- Quản lý 30–100 địa điểm tại TP.HCM.
- Tìm kiếm và lọc địa điểm.
- Cá nhân hóa gợi ý địa điểm.
- Tạo lịch trình theo timeline.
- Xét giờ mở cửa, chi phí, thời gian, khoảng cách.
- Hiển thị địa điểm và tuyến đường trên bản đồ.
- Dùng RAG để truy xuất và giải thích thông tin.
- Phân tích dữ liệu thời tiết, giao thông hoặc đông đúc.
- Tái lập lịch khi điều kiện bất lợi.
- Đánh giá sự khác biệt giữa lịch trình tĩnh và lịch trình có dữ liệu động.

---

## 6. Phạm vi MVP

### Bắt buộc

- Chỉ TP.HCM.
- Khoảng 30–100 địa điểm.
- Web mobile-first.
- Người dùng nhập:
  - Ngày đi.
  - Giờ bắt đầu và kết thúc.
  - Vị trí xuất phát.
  - Sở thích.
  - Ngân sách.
  - Tốc độ di chuyển.
  - Loại trải nghiệm.
- Hệ thống trả:
  - Danh sách địa điểm phù hợp.
  - Timeline.
  - Bản đồ.
  - Chi phí ước tính.
  - Thời gian di chuyển.
  - Lý do chọn địa điểm.
  - Cảnh báo.
  - Phương án thay thế.

### Không thuộc MVP

- AR hoặc Unity.
- Mobile native.
- Microservices đầy đủ.
- Deep Learning phức tạp.
- LSTM/Transformer nếu không có lý do thực nghiệm rõ ràng.
- Phạm vi ngoài TP.HCM.
- Tối ưu hóa toán học quá phức tạp.

---

## 7. Kiến trúc đã chốt

```text
Next.js Mobile-first Frontend
          |
          | REST API / JSON
          v
Spring Boot Modular Monolith
          |
          +---------------------------+
          |                           |
          v                           v
PostgreSQL + pgvector         Python FastAPI AI Service
          |                           |
          |                           +--> LangChain
          |                           +--> Gemini hoặc GPT
          |
          +--> Weather / Routing / Context APIs
```

### Quyết định kiến trúc

- Backend chính: Spring Boot modular monolith.
- Tổ chức package-by-feature.
- Database: PostgreSQL.
- Vector search: pgvector khi triển khai RAG.
- Migration: Flyway migration-first.
- Hibernate: `ddl-auto: validate`.
- Frontend: Next.js + TypeScript, mobile-first.
- AI service: Python FastAPI.
- Map: Leaflet + OpenStreetMap.
- Không dùng microservices trong MVP.
- AI service tách riêng vì hệ sinh thái Python phù hợp hơn.

---

## 8. Cấu trúc repository

```text
saigon-plan-travel/
├── .env
├── .env.example
├── .gitignore
├── AGENTS.md
├── backend/
├── frontend/
├── ai-service/
├── infra/
│   └── compose.yaml
├── docs/
└── README.md
```

### Quy ước cấu hình

- Chỉ một `.env` thật ở root.
- `.env` không được commit.
- `.env.example` không chứa secret thật.
- `compose.yaml` đặt trong `infra/`.
- Spring Boot chạy trực tiếp từ IntelliJ trong giai đoạn phát triển.
- PostgreSQL chạy bằng Docker.
- Không cần chạy `mvn package` trước khi Run `BackendApplication`.
- `mvn package` dùng khi cần chạy test đầy đủ và tạo JAR.

---

## 9. Trạng thái hiện tại

### Đã có trong mã nguồn

- Cấu trúc monorepo, cấu hình datasource bằng biến môi trường và cấu hình
  PostgreSQL bằng Docker đã có.
- Đã chốt Spring Boot modular monolith, package-by-feature và Place Module là
  vertical slice đầu tiên.
- Place Module đã có catalog, detail và category vertical slices: controller,
  read-only service, repository, mapper, DTO và common error handler.
- Trip Module đã có one-day anonymous draft vertical slice: POST/GET/PUT,
  public UUID, validation theo HCMC Clock, transactional aggregate replacement
  và Place-owned batch category lookup contract.
- Flyway có V1–V11. V6 seed 5 place active/1 inactive; V7 siết category slug và
  opening-hour state; V8 seed category/relation/opening-hours demo; V9 bật
  `unaccent`; V10 tạo `trips`; V11 tạo `trip_category_preferences`.
- Các quy ước đã chốt vẫn giữ nguyên:
  - Entity không trả trực tiếp qua API.
  - DTO ưu tiên `record`.
  - Repository kế thừa `JpaRepository`.
  - Không tạo `RepositoryImpl` cho CRUD thông thường.
  - Constructor injection.
  - Lombok dùng có kiểm soát.

### Trạng thái xác minh ngày 2026-07-20

- FEAT-001 đã đạt `done` theo bằng chứng owner cung cấp và source test được audit.
- Maven clean/verify build thành công: 15 tests, 0 failures, 0 errors, 0 skipped;
  Spring Boot JAR được repackage thành công.
- PostgreSQL Testcontainers/pgvector xác nhận Flyway V1–V6, Hibernate validate,
  extension `vector`, đúng 6 places/5 active và migrate lại không seed trùng.
- Normal runtime phục vụ trên cổng 8080. Smoke test xác nhận:
  - list mặc định trả HTTP 200, 5 active places đúng fixed sort và envelope;
  - page vượt giới hạn trả HTTP 200 với `content: []` và đủ metadata;
  - pagination sai trả HTTP 400 `application/problem+json`;
  - health và các invalid-input cases còn lại được owner xác nhận thành công.
- Live response có dữ liệu V6, vì vậy V6 không còn pending. V1–V6 là baseline
  bất biến và version candidate kế tiếp phải được recheck trước implementation.
- FEAT-002 full verification thành công: 28 tests, `clean verify`, PostgreSQL
  Testcontainers V1–V8, Hibernate validate và automated detail query count đúng 3.
- Runtime trên cổng tạm 8081 xác nhận health, detail active/inactive/missing/
  case-mismatch, category catalog và regression place catalog đều đúng contract.
- Live database có V1–V8 `success=true`: 6 places/5 active, 5 categories,
  8 place-category relations và 33 opening-hour rows.
- Sáu test classes FEAT-001/002 bị mất khỏi workspace vì chưa nằm trong commit
  đã được phục hồi từ Git dangling blobs; `./mvnw clean test` chạy lại đủ 28
  tests và tất cả pass ngày 2026-07-20.

### Hoàn tất gần nhất

FEAT-004 Trip Preferences & Draft Trip MVP đạt `done` ngày 2026-07-24 sau khi
owner duyệt requirements và file-by-file plan.

- `POST /api/v1/trips`, `GET /api/v1/trips/{publicId}` và
  `PUT /api/v1/trips/{publicId}` đã triển khai/xác minh runtime.
- Trip Draft là one-day, anonymous, dùng public UUID; time/budget/location,
  pace/environment và 1–5 categories có validation/service/database guards.
- PUT full replacement atomic; unknown category được phát hiện trước mutation
  và rollback evidence giữ snapshot cũ.
- Category lookup dùng Place-owned immutable DTO contract với batch slug/ID
  query; Trip không import Place Entity/Repository.
- `./mvnw clean verify`: 60 tests pass, JAR repackage thành công.
- PostgreSQL Testcontainers chạy sạch V1–V11, rerun zero migration, Hibernate
  validate và constraints/persistence integration đều pass.
- Live database nâng V9→V11; runtime smoke POST/GET/PUT, errors, Place/Category
  regression và health đều pass; warm Trip GET quan sát 8.449 ms.
- Draft smoke-test đã được xóa sau verification; V10/V11 không seed Trip.

### Mốc tiếp theo

1. Review/commit/push có chủ đích toàn bộ source, tests, V7–V11 và docs đang có
   trong workspace; không chạy cleanup trước khi commit.
2. Bắt đầu `$grill-with-docs` cho FEAT-005 Basic Itinerary Generation /
   Scheduling V1; không mở rộng FEAT-004 bằng scheduling.
3. Chỉ đưa dữ liệu địa điểm thật vào catalog sau quy trình xác minh nguồn.

### Yêu cầu FEAT-001 đã phê duyệt

- `GET /api/v1/places` là endpoint công khai.
- Pagination dùng `page` bắt đầu từ 0 và `size` từ 1 đến 100.
- Mặc định `page=0`, `size=20`.
- Response dùng pagination envelope ổn định gồm đúng 7 field cấp cao:
  `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`.
- Empty result vẫn dùng đầy đủ envelope và `content: []`.
- Sorting cố định theo `name ASC`, sau đó `id ASC`.
- Client không được điều khiển sorting trong FEAT-001.
- Mỗi phần tử summary gồm đúng 11 field: `id`, `name`, `slug`,
  `shortDescription`, `district`, `latitude`, `longitude`,
  `estimatedVisitMinutes`, `minCost`, `maxCost`, `indoor`.
- `address` không thuộc list summary; field này dành cho detail response.
- `shortDescription` nullable và vẫn xuất hiện khi là `null`; `indoor` là
  boolean non-null.
- Lỗi input trả HTTP 400 `ProblemDetail`.
- Lỗi hệ thống trả HTTP 500 `ProblemDetail` an toàn.
- `ProblemDetail` có thể được mở rộng sau bằng `code`, `fieldErrors` và
  `requestId`, nhưng các extension này chưa phải contract hiện tại.
- V2 là schema `places` canonical. V1–V6 đã được áp dụng và là migration bất
  biến: không sửa hoặc xóa.
- V6 đã được áp dụng/xác minh cùng automated tests và runtime smoke test.
- FEAT-001 đạt `done` ngày 2026-07-20.

### Trạng thái hoàn tất FEAT-002

- Spec `FEAT-002-place-detail-category-opening-hours.md` đã được đồng bộ
  với source/migrations thực tế và được owner phê duyệt ngày 2026-07-20.
- V3–V5 đã tạo `categories`, `place_categories`, `opening_hours` và đã
  applied trên live database. FEAT-002 không tạo lại hoặc sửa các migration này.
- Source đã có Entity/Repository/Service/Mapper/DTO và hai endpoint FEAT-002.
- V7 đã bổ sung category-slug và closed/open state constraints mà không sửa V3–V5.
- V8 đã seed 5 categories, 8 relations và 33 opening-hour rows có nhãn demo.
- FEAT-002 đã khóa detail payload 15 field; nested collection dùng `[]`;
  category catalog là root array không pagination; fixed sorting; `HH:mm` và
  open/closed/unknown semantics; `ProblemDetail` có `code=PLACE_NOT_FOUND`;
  5 category demo và automated query-count/test strategy.
- Automated tests, `clean verify`, live migration, Hibernate validation và
  runtime smoke test đều thành công ngày 2026-07-20.
- Detail endpoint dùng đúng 3 queries theo Hibernate statistics, không N+1.

---

## 10. Quy tắc Spring Boot

### Package structure

```text
com.saigonplantravel.backend
├── common/
├── place/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   └── mapper/
├── trip/
├── itinerary/
├── scheduling/
├── recommendation/
├── context/
├── replanning/
└── aiintegration/
```

### Persistence

- Flyway là nguồn sự thật của schema.
- Không dùng Hibernate để tạo hoặc thay đổi schema production.
- Giữ `spring.jpa.hibernate.ddl-auto=validate`.
- Không sửa migration đã chạy trong môi trường dùng chung; tạo migration mới.
- Dùng `BigDecimal` cho tiền.
- Cẩn thận N+1 và quan hệ eager.
- Chỉ tạo custom repository implementation khi query thật sự phức tạp.

### API

- Base path: `/api/v1`.
- Controller mỏng.
- Business logic nằm trong service hoặc domain method.
- Không trả Entity trực tiếp.
- Request/response dùng DTO.
- Validation dùng Jakarta Bean Validation.
- Error response phải thống nhất.
- Read service ưu tiên `@Transactional(readOnly = true)`.

### Dependency Injection

- Ưu tiên constructor injection.
- Có thể dùng Lombok `@RequiredArgsConstructor`.
- Không cần `@Autowired` nếu chỉ có một constructor.

### Lombok

Entity:

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
```

Không dùng trên Entity:

- `@Data`
- `@Setter` toàn class
- `@ToString` không kiểm soát
- `@EqualsAndHashCode` tùy tiện

Service/Controller:

```java
@RequiredArgsConstructor
```

DTO:

- Ưu tiên Java `record`.

---

## 11. Entity, DTO và POJO

### Entity

- Đại diện bảng database.
- Có `@Entity`.
- Được Hibernate quản lý.
- Ví dụ: `Place`, `Category`, `OpeningHour`, `Trip`, `Itinerary`.

### DTO

- Nhận hoặc trả dữ liệu API.
- Không ánh xạ database.
- Ví dụ: `CreateTripRequest`, `PlaceSummaryResponse`.

### Luồng bắt buộc

```text
Database
→ Entity
→ Service
→ Mapper
→ DTO
→ Controller
→ JSON
```

Không dùng:

```text
Database
→ Entity
→ Controller
→ JSON
```

---

## 12. Place Module — thiết kế ban đầu

### Bảng `places`

- `id`
- `name`
- `slug`
- `short_description`
- `full_description`
- `address`
- `district`
- `latitude`
- `longitude`
- `estimated_visit_minutes`
- `min_cost`
- `max_cost`
- `indoor`
- `active`
- `created_at`
- `updated_at`

### Bảng metadata địa điểm đã có application feature

- `categories`
- `place_categories`
- `opening_hours`

V3–V5 là canonical schema cho ba bảng này. V7 giữ các migration đó bất biến và
bổ sung category-slug regex cùng closed/open state constraint. V8 cung cấp dữ
liệu demo; FEAT-002 application slice đã map các bảng bằng LAZY relations.

### Ràng buộc chính

- `slug` unique.
- Latitude trong `[-90, 90]`.
- Longitude trong `[-180, 180]`.
- `estimated_visit_minutes > 0`.
- `min_cost >= 0`.
- `max_cost >= min_cost`.

### Migration hiện có

```text
V1__enable_pgvector.sql
V2__create_places_table.sql
V3__create_categories_table.sql
V4__create_place_categories_table.sql
V5__create_opening_hours_table.sql
V6__seed_demo_places.sql
V7__strengthen_category_and_opening_hour_constraints.sql
V8__seed_demo_place_metadata.sql
V9__enable_unaccent_and_place_search_index.sql
V10__create_trips_table.sql
V11__create_trip_category_preferences_table.sql
```

Trạng thái xác minh ngày 2026-07-24: V1–V11 đã áp dụng thành công trên live
database và database sạch Testcontainers. Toàn bộ lịch sử này bất biến;
migration mới phải dùng version tiếp theo hợp lệ sau khi recheck.

---

## 13. Trạng thái API

### Đã có và đã xác minh runtime

```http
GET /actuator/health
GET /api/v1/places
```

`GET /api/v1/places` và health endpoint đã được xác minh bằng normal runtime
smoke test ngày 2026-07-20.

### FEAT-002 đã triển khai và xác minh runtime

```http
GET /api/v1/places/{slug}
GET /api/v1/categories
```

Detail route lookup exact/case-sensitive theo `slug`. Detail có 15 field;
category catalog là root array không pagination; nested collection dùng `[]`;
opening hours dùng ISO day, `HH:mm` và open/closed/unknown semantics; not-found
dùng Spring `ProblemDetail` với `code=PLACE_NOT_FOUND`. Smoke test xác nhận cả
hai endpoint, error cases và tương thích ngược với FEAT-001.

Bộ lọc FEAT-003 đã triển khai và xác minh:

```http
GET /api/v1/places?keyword=...
GET /api/v1/places?district=...
GET /api/v1/places?category=...
GET /api/v1/places?indoor=true
GET /api/v1/places?maxCost=...
```

Thứ tự feature hiện tại:

1. FEAT-002 place detail/category/opening hours đã `done`.
2. FEAT-003 mở rộng trực tiếp `GET /api/v1/places` bằng các filter ở
   trên; không tạo endpoint `/api/v1/places/search`. Giữ exact 7-field page
   envelope, 11-field summary, defaults 0/20, size 1..100 và sort name/id.
3. Search text dùng NFC/trim/collapse/lower/PostgreSQL `unaccent`; category một
   exact slug, `indoor=false` hợp lệ, budget dùng `minCost <= maxCost`.
4. V9 chỉ bật `unaccent` và thêm reverse category index. Full suite hiện có 39
   tests; tối đa 2 SQL queries và dưới 500 ms sau warm-up đã được xác nhận trên
   test-only fixture 30 places.

### FEAT-004 đã triển khai và xác minh runtime

```http
POST /api/v1/trips
GET  /api/v1/trips/{publicId}
PUT  /api/v1/trips/{publicId}
```

- POST trả 201 và relative `Location`; GET/PUT trả 200.
- Response có 12 top-level fields, `HH:mm`, public UUID và không lộ internal
  Trip ID/Entity.
- Validation dùng `INVALID_REQUEST`; unknown category dùng
  `INVALID_CATEGORY_PREFERENCE`; valid missing UUID dùng `TRIP_NOT_FOUND`.
- PUT là full replacement atomic; GET dùng Trip query và một category batch
  query, không N+1.
- Anonymous UUID không phải authorization; FEAT-004 không lưu PII và không gọi
  Scheduling, Routing, Context hoặc AI.
- Contract chi tiết: `docs/04-API/Trip-API.md`; schema:
  `docs/03-Database/Trip-Module-ERD.md`.

Admin API để sau:

```http
POST   /api/v1/admin/places
PUT    /api/v1/admin/places/{id}
PATCH  /api/v1/admin/places/{id}/status
DELETE /api/v1/admin/places/{id}
```

---

## 14. Dữ liệu địa điểm

- Không được bịa dữ liệu địa điểm như thể đã được xác minh.
- Seed data phục vụ test phải ghi rõ là dữ liệu demo.
- Trước khi dùng cho RAG, demo chính thức hoặc báo cáo, cần lưu nguồn và ngày truy cập.
- Các trường có thể thay đổi như giá vé, giờ mở cửa phải có cơ chế cập nhật và ghi nguồn.
- Khi thiếu dữ liệu thật, được phép dùng dữ liệu mô phỏng có cấu trúc nhưng phải ghi rõ trong báo cáo.

---

## 15. RAG Module

### Vai trò

- Truy xuất thông tin địa điểm.
- Trả lời câu hỏi dựa trên dữ liệu.
- Giải thích vì sao địa điểm phù hợp.
- Không bịa nếu context không đủ.

### Pipeline dự kiến

```text
Nguồn dữ liệu
→ Làm sạch
→ Chia chunk
→ Embedding
→ pgvector
→ Semantic search
→ Context
→ Gemini/GPT
→ Câu trả lời có căn cứ
```

RAG chưa phải bước hiện tại. Hoàn thành Place API và dữ liệu tĩnh trước.

---

## 16. Time-series / Context Module

Dữ liệu động:

- Thời tiết.
- Giao thông.
- Mức độ đông đúc.
- Thời gian di chuyển.

MVP có thể bắt đầu bằng:

- API thật.
- Rule-based.
- Dữ liệu mô phỏng có cấu trúc.

Mô hình mở rộng:

- Moving average.
- ARIMA.
- Prophet.
- Random Forest.
- XGBoost.

Không bắt buộc Deep Learning.

---

## 17. Scheduling Module

### Input

- Địa điểm ứng viên.
- Sở thích.
- Ngân sách.
- Thời gian.
- Vị trí xuất phát.
- Giờ mở cửa.
- Thời gian tham quan.
- Thời gian di chuyển.
- Context score.

### Output

- Thứ tự địa điểm.
- Timeline.
- Chi phí.
- Thời gian.
- Cảnh báo.
- Giải thích.

### MVP

- Scoring function.
- Greedy heuristic.
- Rule-based constraints.

Không cần tối ưu hóa quá phức tạp.

---

## 18. Re-planning Module

Khi điều kiện bất lợi:

- Thay địa điểm ngoài trời bằng trong nhà.
- Đổi thứ tự.
- Bỏ địa điểm nếu thiếu thời gian.
- Chọn địa điểm thay thế cùng khu vực hoặc sở thích.

MVP dùng rule-based trước.

---

## 19. Workflow Codex không dùng Spec Kit

```text
Ý tưởng/chức năng
→ $grill-with-docs
→ Feature Spec được phê duyệt
→ Kế hoạch file-by-file
→ Triển khai từng bước nhỏ
→ Test
→ Review diff
→ Cập nhật docs và context
```

### Khi phải dùng `$grill-with-docs`

- Thêm bảng hoặc thay schema.
- Thêm API contract.
- Thay kiến trúc.
- Tích hợp dịch vụ ngoài.
- Scheduling.
- RAG.
- Time-series.
- Re-planning.
- Feature có nhiều edge case.

### Khi có thể prompt trực tiếp

- Sửa typo.
- Sửa lỗi compile nhỏ.
- Refactor không đổi behavior.
- Thêm validation hiển nhiên.
- Thay đổi nhỏ đã có feature spec rõ ràng.

---

## 20. Prompt format bắt buộc cho vibe coding

Mỗi yêu cầu triển khai nên có:

```text
Goal
Context
Constraints
Done when
Verify
```

Ví dụ:

```text
Goal:
Implement GET /api/v1/places.

Context:
- @docs/00-Dashboard/PROJECT_CONTEXT.md
- @docs/01-Requirements/Features/FEAT-001-place-catalog-api-mvp.md
- @AGENTS.md
- @backend/AGENTS.md

Constraints:
- Flyway owns schema.
- Hibernate validate.
- Do not return Entity.
- Use record DTO.
- Use constructor injection.
- Do not expand scope.

Done when:
- Only active places are returned.
- Ordering is deterministic.
- HTTP 200.
- Tests pass.
- Relevant docs updated.

Verify:
cd backend && ./mvnw test
```

---

## 21. Definition of Done

Một feature chỉ được đánh dấu hoàn thành khi:

- Feature spec đã được phê duyệt.
- Code nằm đúng module/package.
- Migration hợp lệ.
- Không trả Entity trực tiếp.
- Input validation đầy đủ.
- Error handling phù hợp.
- Test liên quan chạy thành công.
- Agent báo rõ lệnh đã chạy và kết quả.
- Không có secret trong Git.
- Tài liệu kỹ thuật được cập nhật.
- Nội dung báo cáo liên quan được xác định hoặc viết nháp.
- `PROJECT_CONTEXT.md` được cập nhật nếu trạng thái dự án thay đổi đáng kể.

---

## 22. Quy tắc báo cáo khóa luận

Mỗi feature lớn phải hỗ trợ song song:

### Kỹ thuật

- Mục đích.
- Vị trí kiến trúc.
- Database.
- API.
- Service/module.
- Input/output.
- MVP.
- Hướng mở rộng.

### Báo cáo

- Cơ sở lý thuyết → Chương 2.
- Phân tích, kiến trúc, database, API → Chương 3.
- Cài đặt và demo → Chương 3.
- Thực nghiệm và đánh giá → phần đánh giá.
- Ngoài MVP → Chương 4.

---

## 23. Cách cập nhật file này

Sau mỗi mốc lớn, cập nhật:

- Trạng thái hiện tại.
- Đã hoàn thành.
- Đang thực hiện.
- Mốc tiếp theo.
- Quyết định kiến trúc mới.
- Schema.
- API.
- Vấn đề và cách xử lý.
- Kết quả test.
- Nội dung báo cáo đã viết.

Không ghi nhật ký chi tiết hằng ngày vào file này. Nhật ký đặt trong:

```text
docs/09-Development-Log/
```

---

## 24. Frontend Place Explorer — 2026-07-21

- Đã triển khai `/places` và `/places/[slug]` bằng Next.js App Router cho
  FEAT-001–003, responsive desktop/mobile theo hướng Mint Map.
- Place API được gọi server-side qua `BACKEND_API_BASE_URL`; backend chưa cần
  thay đổi CORS. Search dùng route thực tế `GET /api/v1/places`.
- Leaflet/OpenStreetMap hiển thị tọa độ thật, marker selection và attribution;
  không triển khai routing/timeline/distance thuộc FEAT-004–009.
- Frontend lint/build pass; runtime smoke với database thật trả HTTP 200 cho
  catalog và detail. Learning note: `F04-NextJS-Place-Explorer-Frontend-Guide.md`.
- Frontend đã refactor UI primitives sang shadcn/ui 4.13.1 (Radix/Nova) mà không
  đổi API/DTO/business behavior. Mint Map dùng semantic CSS variables; Button,
  Input, Label, Select, Sheet, Alert, Skeleton, Avatar, Card, Badge và Pagination
  đã được áp dụng có chọn lọc. Lint, strict type check, production build và
  runtime smoke catalog/detail/filter/empty/backend-error đều pass; chưa có
  browser screenshot automation trong môi trường hiện tại.
