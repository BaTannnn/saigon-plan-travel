# 2026-08-09 — FEAT-005 Backend Milestone

## Kết quả

Backend FEAT-005 hiện có luồng hoàn chỉnh từ Trip đã xác thực đến lịch trình
`GREEDY_V1` được lưu và đọc lại:

```text
ItineraryController
→ SchedulingService
→ TripSchedulingQuery + PlaceSchedulingQuery
→ GreedyItineraryScheduler
→ Itinerary aggregate
→ ItineraryRepository
→ ItineraryMapper
→ ItineraryResponse
```

Hai endpoint đã có trong source:

```http
POST /api/v1/trips/{tripPublicId}/itineraries
GET  /api/v1/itineraries/{itineraryPublicId}
```

Mỗi POST thành công tạo một public UUID mới và trả `201 Created` cùng header
`Location`. GET đọc snapshot đã lưu, không chạy lại scheduler, và tính `stale`
từ `Trip.updatedAt` hiện tại so với timestamp snapshot.

## Bằng chứng implementation

### Module boundary

- Scheduling đọc Trip qua `TripSchedulingQuery`/`TripSchedulingSnapshot`.
- Scheduling đọc Place qua `PlaceSchedulingQuery`/`PlaceSchedulingCandidate`.
- Scheduling không import Trip/Place entity hoặc repository.
- `userId` lấy từ `UserPrincipal`; client không gửi ownership ID.

### GREEDY_V1

Pure domain hiện thực:

- Haversine cho khoảng cách đường chim bay;
- thời gian di chuyển từ tốc độ trung bình cấu hình và transfer overhead cố định;
- điều chỉnh visit duration theo `RELAXED`, `BALANCED`, `FAST`;
- kiểm tra environment, budget, closed hours, trip window và closing time;
- waiting khi đến trước giờ mở cửa;
- score gồm preference, distance, cost, opening confidence và time efficiency;
- tie-break theo score, waiting, distance, cost, name và place ID;
- đánh giá lại toàn bộ candidate còn lại sau mỗi lần chọn.

Thuật toán không gọi routing, weather, RAG, LLM hoặc AI service. Với cùng
snapshot/config, business outcome deterministic; public UUID và `generatedAt` là
hai giá trị được phép thay đổi.

### Persistence

| Migration | Nội dung thực tế |
| --- | --- |
| V13 | `itineraries`, `itinerary_preferred_categories` |
| V14 | `itinerary_items`, `itinerary_warnings` |
| V15 | cho phép cặp administrative-unit snapshot cùng null |

`Itinerary` là aggregate root. Preferred categories dùng `@ElementCollection`;
items và warnings dùng ordered `@OneToMany` với cascade. Aggregate lưu Trip/config,
origin, preferences, timeline, place facts, opening facts, chi phí, score, totals và
warning tại thời điểm generate.

V15 là forward migration. Nó giải quyết xung đột thực tế: Place source cho phép
demo place chưa có administrative-unit mapping, trong khi V14 ban đầu yêu cầu item
snapshot luôn có hai giá trị này. Không migration V1–V14 nào bị sửa.

### API và lỗi

MockMvc contract hiện bao phủ:

- POST `201` và `Location`;
- GET `200` với `stale`;
- malformed UUID `400 INVALID_REQUEST`;
- ownership-scoped Itinerary missing `404 ITINERARY_NOT_FOUND`;
- invalid scheduling snapshot `409 SCHEDULING_DATA_CONFLICT`;
- zero feasible `422 NO_FEASIBLE_ITINERARY` với rejection summary;
- POST/GET không authentication trả `401`;
- response không có internal `id`, `tripId`, `userId`.

Zero-feasible được phát hiện trước `ItineraryRepository#save`. Một itinerary có ít
nhất một item được persist như một snapshot sử dụng được, kể cả khi còn candidate
không thể xếp.

## Automated verification

Các lệnh thực tế đã chạy từ `backend/` trong milestone:

```bash
./mvnw test
./mvnw clean verify
./mvnw clean test
```

Kết quả console của cả ba lần cuối đều báo `BUILD SUCCESS`. Báo cáo Surefire mới
nhất sau `./mvnw clean test`, sinh lúc 2026-08-09 16:20 +07:00, được tổng hợp:

| Evidence | Kết quả |
| --- | ---: |
| Test suites | 33 |
| Tests | 130 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

`./mvnw clean verify` còn xác nhận clean compilation, chạy Flyway V1–V15 trên
Testcontainers PostgreSQL, Hibernate `ddl-auto=validate`, tạo JAR và Spring Boot
repackage thành công.

Các test FEAT-005 có bằng chứng cho:

- feasibility, pace, Haversine, score và tie-break rules;
- state recomputation và input-permutation determinism;
- schema constraints, foreign-key/delete behavior và unknown opening hours;
- persist/load aggregate với ordered children và nullable administrative-unit pair;
- generate hai immutable versions qua real Trip/Place contracts;
- stored GET, stale false/true và cross-user Itinerary not-found;
- web/security/ProblemDetail contract.

## Ánh xạ vào báo cáo khóa luận

### Cơ sở lý thuyết

- công thức Haversine và giới hạn của khoảng cách đường chim bay;
- heuristic greedy, độ phức tạp `O(n²)` và lý do không tuyên bố tối ưu toàn cục;
- constraint feasibility, weighted score và deterministic tie-breaking;
- `BigDecimal` và chính sách rounding cho tiền, distance, score và duration.

### Phân tích và thiết kế hệ thống

- modular monolith và cross-module immutable read contracts;
- sequence generate/GET và ownership boundary;
- aggregate `Itinerary` cùng bốn bảng FEAT-005;
- DTO/Entity boundary và error contract 400/404/409/422.

### Cài đặt và triển khai

- `SchedulingConfiguration`, pure domain packages, `SchedulingService`;
- JPA aggregate, Flyway V13–V15, repository, mapper và controller;
- injected `Clock`, transactional generate và read-only GET.

### Thực nghiệm và đánh giá

- 130-test regression evidence;
- deterministic permutation test;
- PostgreSQL/Testcontainers constraint và persistence evidence;
- immutable-version, stale và ownership scenarios.

### Hướng phát triển

- FEAT-006 thay Haversine baseline bằng route snapshot nhưng không đổi thứ tự
  GREEDY_V1 đã lưu;
- FEAT-007 giải thích kết quả bằng grounded RAG, không thay scheduler;
- FEAT-008 bổ sung context theo thời gian;
- FEAT-009 tạo re-planning proposal, không mutate base itinerary.

## Đoạn học thuật nháp

Hệ thống xây dựng lịch trình một ngày bằng heuristic tham lam xác định. Tại mỗi
vòng lặp, toàn bộ địa điểm chưa được chọn được đánh giá lại theo trạng thái hiện
tại gồm vị trí, thời gian và ngân sách còn lại. Các địa điểm vi phạm ràng buộc
môi trường, ngân sách hoặc thời gian bị loại trước bước tính điểm. Trong tập khả
thi, hệ thống tính điểm có trọng số từ mức phù hợp sở thích, khoảng cách, chi phí,
độ tin cậy của giờ mở cửa và hiệu quả sử dụng thời gian, sau đó áp dụng chuỗi
tie-break cố định. Cách tiếp cận này không bảo đảm nghiệm tối ưu toàn cục, nhưng
tạo ra baseline lặp lại được, giải thích được và có thể kiểm thử bằng các invariant
cụ thể.

Kết quả lập lịch được lưu dưới dạng snapshot bất biến thay vì chỉ giữ tham chiếu
đến trạng thái Place và Trip hiện tại. Snapshot bao gồm giả định thuật toán, timeline,
chi phí, khoảng cách đường chim bay, dữ liệu giờ mở cửa và cảnh báo tại thời điểm
sinh. Thiết kế này hỗ trợ audit lịch sử và phát hiện `stale` khi Trip thay đổi, đồng
thời tạo nền ổn định cho các bước enrichment và re-planning sau này.

## Hình và bảng nên bổ sung

1. Sequence diagram: POST → Trip/Place contracts → GREEDY_V1 → aggregate → response.
2. ERD: `itineraries` với category snapshot, ordered items và warnings.
3. Flowchart một vòng lặp GREEDY_V1 và thứ tự rejection reasons.
4. Bảng thành phần score, trọng số, rounding và tie-breaker.
5. Bảng test evidence theo pure/service/web/PostgreSQL layers.

Chưa tạo số liệu biểu đồ runtime hoặc query-count vì chưa có phép đo tương ứng.

## Hạn chế phải nêu

- Haversine là khoảng cách đường chim bay, không phải quãng đường đường bộ.
- Travel time dùng tốc độ trung bình và transfer overhead cấu hình, không có traffic.
- Cost dùng `minCost`, không phải chi phí giao dịch thực tế.
- Place seed data là demo/chưa xác minh; không được trình bày như dữ liệu du lịch
  đã kiểm chứng.
- Chưa hỗ trợ multi-day, overnight, return-to-origin, nhiều opening intervals,
  ngày lễ, transport mode hoặc tối ưu toàn cục.
- Chưa có routing, weather, RAG/LLM hoặc re-planning trong FEAT-005.

## Evidence còn thiếu trước khi tuyên bố FEAT-005 hoàn tất toàn bộ DoD

- live HTTP/JWT smoke flow trên ứng dụng chạy thật;
- phép đo warm runtime với 30–100 candidates và mô tả môi trường đo;
- query-count instrumentation với 30–100 candidates;
- test failure-injection xác nhận rollback toàn aggregate;
- cập nhật ERD/API/algorithm notes và `PROJECT_CONTEXT.md` ngoài phạm vi lần sync này;
- final owner review/commit của milestone trong experimental worktree.

## Files chính để kiểm tra

- `backend/src/main/java/com/saigonplantravel/backend/scheduling/service/SchedulingService.java`
- `backend/src/main/java/com/saigonplantravel/backend/scheduling/domain/algorithm/GreedyItineraryScheduler.java`
- `backend/src/main/java/com/saigonplantravel/backend/scheduling/entity/Itinerary.java`
- `backend/src/main/java/com/saigonplantravel/backend/scheduling/controller/ItineraryController.java`
- `backend/src/main/resources/db/migration/V13__create_itineraries_and_preference_snapshots.sql`
- `backend/src/main/resources/db/migration/V14__create_itinerary_items_and_warnings.sql`
- `backend/src/main/resources/db/migration/V15__allow_unknown_itinerary_item_administrative_unit.sql`
