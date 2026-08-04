# PROJECT_CONTEXT — SaigonPlanTravel

> Nguồn ngữ cảnh hiện hành cho coding agent. Source code và migration trong
> working tree là bằng chứng cuối cùng nếu tài liệu cũ mâu thuẫn.

## 1. Mục tiêu và phạm vi

SaigonPlanTravel là hệ thống web mobile-first hỗ trợ lập lịch du lịch tại
Thành phố Hồ Chí Minh bằng dữ liệu địa điểm, lập lịch có ràng buộc, RAG, dữ
liệu theo thời gian và dynamic replanning.

MVP giới hạn trong TP.HCM với khoảng 30–100 địa điểm. Không đưa AR, Unity,
native mobile, microservices hoặc deep learning phức tạp vào MVP.

## 2. Kiến trúc đã chốt

- Frontend: Next.js và TypeScript, mobile-first.
- Backend: Spring Boot modular monolith, package-by-feature.
- Database: PostgreSQL; Flyway migration-first.
- Hibernate: `spring.jpa.hibernate.ddl-auto=validate`.
- AI service tương lai: Python FastAPI.
- Map: Leaflet và OpenStreetMap.
- REST controller chỉ trả DTO, không trả JPA Entity.
- Ưu tiên constructor injection và read-only transaction cho read service.

## 3. Trạng thái source ngày 2026-07-27

### Place Module

FEAT-001 đến FEAT-003 đã có production code:

- `GET /api/v1/places` với keyword/filter/pagination.
- `GET /api/v1/places/{slug}`.
- `GET /api/v1/categories`.
- Entity, repository, JPA Specification, service, mapper và DTO nằm trong
  `com.saigonplantravel.backend.place`.
- Frontend có catalog, filter, map và place detail.

Mô hình vị trí của Place không còn dùng đơn vị cấp quận. Hai field nội bộ và
public contract là:

- `administrativeUnitName`, nullable;
- `administrativeUnitType`, nullable, nhận `WARD`, `COMMUNE` hoặc
  `SPECIAL_ZONE`.

Hai field phải cùng `null` hoặc cùng có giá trị. Filter hiện chỉ nhận
`administrativeUnitName`; keyword search cũng tìm trên field name này. Không
có filter theo type trong phạm vi hiện tại.

Sáu place demo chưa có mapping đã được xác minh, vì vậy cả hai field đều
`null`. Frontend hiển thị “Chưa xác định” và không đưa row null vào danh sách
filter. Không được suy diễn mapping từ địa chỉ, tọa độ hoặc dữ liệu cũ.

### Authentication

Source hiện có module authentication riêng cùng Flyway V10 tạo bảng `users`.
Refactor Place không thay đổi module hoặc migration authentication.

### Trip và Scheduling

Không coi requirements cũ của FEAT-004/005 là production baseline. Trước khi
triển khai các module này phải audit lại source và chọn next migration version
thực tế.

## 4. Migration baseline

- V1: pgvector.
- V2: canonical `places` schema.
- V3–V5: category, place-category và opening-hours schema.
- V6: sáu place demo.
- V7–V8: constraints và metadata demo.
- V9: `unaccent` và reverse category index.
- V10: bảng `users` của authentication.

Refactor administrative unit sửa trực tiếp V2 và V6 theo quyết định owner vì
database cũ sẽ bị drop và tạo lại. Không có V11 cho refactor này. Database đã
từng áp dụng V2/V6 cũ không được upgrade tại chỗ; phải recreate để tránh
Flyway checksum mismatch.

## 5. Dữ liệu và an toàn

- PostgreSQL là source of truth.
- Place seed hiện là dữ liệu demo/chưa xác minh.
- Không trình bày seed như dữ liệu địa điểm thực tế.
- Không commit `.env`, API key, access token hoặc credential.
- Không tự động gán administrative unit cho place chưa được mapping thủ công.

## 6. Verification chuẩn

Backend baseline thông thường:

```bash
cd backend
./mvnw test
```

Trong refactor administrative unit hiện tại, owner tự cập nhật tests. Agent chỉ
được phép compile production bằng:

```bash
cd backend
./mvnw -Dmaven.test.skip=true package
```

Frontend:

```bash
cd frontend
npm run lint
npm run build
```

Không tuyên bố full test suite pass cho đến khi tests được owner cập nhật và
chạy thành công.
