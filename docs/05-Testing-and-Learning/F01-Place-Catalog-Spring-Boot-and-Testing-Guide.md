# F01 Place Catalog — Hướng dẫn Spring Boot và kiểm thử cho người mới

> [!WARNING]
> Automated test code được mô tả trong tài liệu này đã được gỡ khỏi repository
> ngày 2026-07-18 theo quyết định của owner. Các phần JUnit, Mockito, MockMvc và
> Testcontainers bên dưới được giữ làm tài liệu tham khảo để viết test lại sau;
> chúng không mô tả test suite hiện đang tồn tại.

## 1. Mục tiêu của tài liệu

Tài liệu này giải thích toàn bộ luồng code của Place Catalog API MVP và cách
kiểm thử tính năng.

Đối tượng đọc là người:

- đã biết Java cơ bản;
- đã học Spring MVC;
- chưa quen Spring Boot;
- từng viết `pytest` nhưng chưa quen JUnit, Mockito, MockMvc và Testcontainers.

API đang được giải thích:

```http
GET /api/v1/places?page=0&size=20
```

Kết quả trả về là danh sách địa điểm đang hoạt động, có phân trang và sắp xếp
cố định.

---

## 2. Bức tranh tổng thể

Khi client gọi API, dữ liệu đi qua các lớp theo thứ tự:

```text
HTTP request
    ↓
PlaceController
    ↓
PlaceService
    ↓
PlaceRepository
    ↓
Hibernate / JPA
    ↓
PostgreSQL
```

Khi PostgreSQL trả dữ liệu, luồng đi ngược lại:

```text
PostgreSQL row
    ↓
Place entity
    ↓
PlaceMapper
    ↓
PlaceSummaryResponse
    ↓
PlacePageResponse
    ↓
JSON response
```

Vai trò ngắn gọn:

| Thành phần | Trách nhiệm |
|---|---|
| Controller | Nhận HTTP request và trả HTTP response |
| Service | Điều phối nghiệp vụ |
| Repository | Truy vấn database |
| Entity | Ánh xạ object Java với bảng PostgreSQL |
| Mapper | Chuyển entity thành DTO |
| DTO | Định nghĩa dữ liệu API được phép trả cho client |
| Exception handler | Chuyển exception thành HTTP error response |
| Flyway | Tạo và cập nhật schema database |

---

## 3. Spring MVC và Spring Boot khác nhau thế nào?

### 3.1 Spring MVC

Spring MVC là framework xử lý web theo mô hình:

```text
Request → Controller → Service → Response
```

Các annotation quen thuộc gồm:

- `@RestController`
- `@RequestMapping`
- `@GetMapping`
- `@RequestParam`
- `@ExceptionHandler`

### 3.2 Spring Boot

Spring Boot không thay thế Spring MVC. Spring Boot giúp cấu hình và khởi động
ứng dụng Spring dễ hơn.

Trong dự án này, Spring Boot tự động:

- tạo web server Tomcat nhúng;
- cấu hình Spring MVC;
- tìm controller, service, repository và component;
- cấu hình datasource;
- cấu hình Hibernate/JPA;
- chạy Flyway khi ứng dụng khởi động;
- đọc `application.yaml`;
- chuyển Java record thành JSON.

Nếu dùng Spring MVC theo cách cũ, lập trình viên phải cấu hình nhiều thành phần
này bằng tay.

### 3.3 Khái niệm bean

Bean là object được Spring tạo và quản lý.

Các class sau trở thành bean vì có annotation:

```java
@RestController
public class PlaceController { }
```

```java
@Service
public class PlaceService { }
```

```java
@Component
public class PlaceMapper { }
```

Repository là trường hợp đặc biệt. Spring Data tự tạo implementation cho
interface kế thừa `JpaRepository`.

### 3.4 Dependency injection

Ví dụ:

```java
public PlaceController(PlaceService placeService) {
    this.placeService = placeService;
}
```

`PlaceController` cần `PlaceService`. Spring:

1. tạo `PlaceService`;
2. tạo `PlaceController`;
3. truyền `PlaceService` vào constructor.

Đây là constructor injection.

So với Python, có thể hình dung gần giống:

```python
service = PlaceService(repository, mapper)
controller = PlaceController(service)
```

Khác biệt là Spring tự thực hiện việc khởi tạo và kết nối các object.

---

## 4. Cấu hình và khởi động ứng dụng

### 4.1 `BackendApplication.java`

File:

```text
backend/src/main/java/com/saigonplantravel/backend/BackendApplication.java
```

Code chính:

```java
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
```

`@SpringBootApplication` kết hợp ba ý tưởng:

- đây là class cấu hình Spring;
- bật auto-configuration;
- quét component trong package hiện tại và package con.

Class nằm trong package:

```text
com.saigonplantravel.backend
```

Vì vậy Spring tìm thấy các class nằm dưới:

```text
com.saigonplantravel.backend.place
com.saigonplantravel.backend.common
```

Đó là lý do Place module phải nằm dưới package root này.

`SpringApplication.run(...)`:

1. tạo Spring Application Context;
2. đọc cấu hình;
3. tạo bean;
4. kết nối database;
5. chạy Flyway;
6. khởi tạo Hibernate;
7. khởi động web server ở port 8080.

### 4.2 `application.yaml`

File:

```text
backend/src/main/resources/application.yaml
```

Datasource:

```yaml
spring:
  datasource:
    url: ${JDBC_DB_URL}
    username: ${JDBC_DB_USERNAME}
    password: ${JDBC_DB_PASSWORD}
```

Spring lấy giá trị từ biến môi trường:

- `JDBC_DB_URL`
- `JDBC_DB_USERNAME`
- `JDBC_DB_PASSWORD`

Ví dụ URL:

```text
jdbc:postgresql://localhost:5432/saigon_plan_travel
```

JPA:

```yaml
jpa:
  open-in-view: false
  hibernate:
    ddl-auto: validate
```

`ddl-auto: validate` có nghĩa:

- Hibernate kiểm tra entity có tương thích schema không;
- Hibernate không tự tạo bảng;
- Hibernate không tự sửa bảng.

Flyway mới là thành phần quản lý schema.

```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
```

Khi ứng dụng khởi động, Flyway tìm migration trong:

```text
backend/src/main/resources/db/migration/
```

---

## 5. Flyway và database

### 5.1 Flyway hoạt động thế nào?

Migration có tên theo quy tắc:

```text
V<version>__<description>.sql
```

Ví dụ:

```text
V2__create_places_table.sql
V6__seed_demo_places.sql
```

Flyway tạo bảng lịch sử:

```text
flyway_schema_history
```

Bảng này ghi migration nào đã chạy. Migration đã chạy không nên bị sửa vì
checksum sẽ thay đổi và Flyway có thể báo lỗi.

### 5.2 `V2__create_places_table.sql`

Migration này tạo bảng `places`.

Các constraint quan trọng:

```sql
CHECK (latitude BETWEEN -90 AND 90)
CHECK (longitude BETWEEN -180 AND 180)
CHECK (estimated_visit_minutes > 0)
CHECK (min_cost >= 0)
CHECK (max_cost >= min_cost)
```

Constraint là hàng phòng thủ ở database. Dù application có bug, PostgreSQL vẫn
không chấp nhận dữ liệu vi phạm.

`slug` có `UNIQUE`, nghĩa là không được có hai place dùng cùng slug.

### 5.3 `V6__seed_demo_places.sql`

Migration này chèn:

- 5 place có `active = true`;
- 1 place có `active = false`.

Place inactive giúp test chứng minh repository không trả dữ liệu bị ẩn.

Một place có `short_description = NULL` để test JSON vẫn chứa:

```json
"shortDescription": null
```

Dữ liệu đều có chữ `Demo` và ghi rõ chưa xác minh, vì đây không phải dữ liệu
du lịch thật.

---

## 6. Entity `Place`

File:

```text
backend/src/main/java/com/saigonplantravel/backend/place/entity/Place.java
```

### 6.1 `@Entity` và `@Table`

```java
@Entity
@Table(name = "places")
public class Place {
}
```

`@Entity` nói với Hibernate rằng class này là một JPA entity.

`@Table(name = "places")` ánh xạ class với bảng `places`.

### 6.2 Primary key

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

- `@Id`: primary key.
- `IDENTITY`: PostgreSQL tạo ID bằng cột identity/sequence tương ứng với
  `BIGSERIAL`.

### 6.3 Ánh xạ column

```java
@Column(nullable = false, unique = true, length = 180)
private String slug;
```

Các thuộc tính này giúp Hibernate validate mapping:

- `nullable = false`: cột không được null;
- `unique = true`: giá trị duy nhất;
- `length = 180`: độ dài varchar.

Khi tên Java dùng camelCase nhưng database dùng snake_case:

```java
@Column(name = "short_description", length = 500)
private String shortDescription;
```

### 6.4 Vì sao tiền dùng `BigDecimal`?

```java
private BigDecimal minCost;
private BigDecimal maxCost;
```

Không nên dùng `double` cho tiền vì số thực nhị phân có sai số.

Ví dụ:

```java
0.1 + 0.2
```

có thể không bằng chính xác `0.3` nếu dùng floating point.

`BigDecimal` phù hợp với PostgreSQL `NUMERIC`.

### 6.5 Timestamp do database quản lý

```java
@Column(
    name = "created_at",
    nullable = false,
    insertable = false,
    updatable = false
)
private OffsetDateTime createdAt;
```

`insertable = false` nói Hibernate không đưa cột này vào câu `INSERT`.

`updatable = false` nói Hibernate không đưa cột này vào câu `UPDATE`.

Database dùng:

```sql
DEFAULT CURRENT_TIMESTAMP
```

để tạo giá trị.

### 6.6 Lombok

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
```

`@Getter` tạo getter khi compile.

`@NoArgsConstructor(PROTECTED)` tạo constructor không tham số cho Hibernate,
nhưng hạn chế code bên ngoài gọi tùy tiện.

Không dùng `@Data` trên entity vì nó tự sinh quá nhiều behavior như setter,
`equals`, `hashCode` và `toString`, có thể gây vấn đề với JPA.

### 6.7 Domain methods

```java
public void deactivate() {
    this.active = false;
}
```

Thay vì mở setter cho toàn bộ field, entity cung cấp method có ý nghĩa nghiệp
vụ như `activate()` và `deactivate()`.

---

## 7. Repository

File:

```text
backend/src/main/java/com/saigonplantravel/backend/place/repository/PlaceRepository.java
```

Code:

```java
public interface PlaceRepository extends JpaRepository<Place, Long> {

    Page<Place> findAllByActiveTrue(Pageable pageable);
}
```

### 7.1 Vì sao interface không có implementation?

Spring Data JPA đọc tên method:

```text
findAllByActiveTrue
```

và tự tạo query tương đương ý tưởng:

```sql
SELECT *
FROM places
WHERE active = TRUE
ORDER BY ...
LIMIT ...
OFFSET ...;
```

Không cần tự viết `PlaceRepositoryImpl` cho query CRUD đơn giản.

### 7.2 `JpaRepository<Place, Long>`

- `Place`: entity repository quản lý.
- `Long`: kiểu primary key.

`JpaRepository` cung cấp sẵn:

- `findById`
- `findAll`
- `save`
- `delete`
- `count`

### 7.3 `Page`, `Pageable` và `PageRequest`

`Pageable` mô tả yêu cầu phân trang:

- page number;
- page size;
- sorting.

`Page<Place>` chứa:

- dữ liệu của trang hiện tại;
- tổng số record;
- tổng số trang;
- trang đầu hay cuối.

Nếu có 5 record và `size=2`:

```text
page 0 → 2 record
page 1 → 2 record
page 2 → 1 record
totalPages = 3
```

---

## 8. DTO và Java record

### 8.1 Tại sao không trả entity?

Nếu trả `Place` entity trực tiếp:

- API bị phụ thuộc chặt vào schema;
- có thể vô tình lộ `active`, timestamps hoặc field nội bộ;
- quan hệ JPA có thể gây lazy-loading hoặc vòng lặp JSON;
- thay đổi database có thể làm hỏng API contract.

Vì vậy API trả DTO.

### 8.2 `PlaceSummaryResponse`

File:

```text
backend/src/main/java/com/saigonplantravel/backend/place/dto/PlaceSummaryResponse.java
```

```java
public record PlaceSummaryResponse(
    Long id,
    String name,
    String slug,
    String shortDescription,
    String district,
    BigDecimal latitude,
    BigDecimal longitude,
    Integer estimatedVisitMinutes,
    BigDecimal minCost,
    BigDecimal maxCost,
    Boolean indoor
) {
}
```

Java record phù hợp với DTO vì:

- immutable;
- tự có constructor;
- tự có accessor như `name()`;
- tự có `equals`, `hashCode`, `toString`;
- ít boilerplate.

Record trên định nghĩa đúng 11 field API được phép trả.

### 8.3 `PlacePageResponse`

```java
public record PlacePageResponse(
    List<PlaceSummaryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
}
```

DTO này tạo JSON envelope ổn định. API không trả trực tiếp `Page<Place>` vì:

- `Page` là type của framework;
- JSON của framework có thể thay đổi;
- envelope tự định nghĩa giúp API contract rõ ràng.

---

## 9. Mapper

File:

```text
backend/src/main/java/com/saigonplantravel/backend/place/mapper/PlaceMapper.java
```

```java
@Component
public class PlaceMapper {

    public PlaceSummaryResponse toSummaryResponse(Place place) {
        return new PlaceSummaryResponse(...);
    }
}
```

Mapper chỉ chịu trách nhiệm chuyển:

```text
Place entity → PlaceSummaryResponse DTO
```

Lợi ích:

- service không chứa một khối mapping dài;
- mapping có thể test riêng;
- controller không biết entity;
- khi DTO thay đổi, vị trí sửa rõ ràng.

`@Component` giúp Spring tạo mapper bean và inject vào service.

---

## 10. Service

File:

```text
backend/src/main/java/com/saigonplantravel/backend/place/service/PlaceService.java
```

### 10.1 `@Service`

```java
@Service
@Transactional(readOnly = true)
public class PlaceService {
}
```

`@Service` đánh dấu class thuộc tầng nghiệp vụ.

`@Transactional(readOnly = true)` mở transaction chỉ đọc. Nó thể hiện method
không có ý định thay đổi dữ liệu và cho phép persistence provider tối ưu trong
một số trường hợp.

### 10.2 Sorting cố định

```java
private static final Sort PLACE_SORT = Sort.by(
    Sort.Order.asc("name"),
    Sort.Order.asc("id")
);
```

Kết quả luôn được sắp xếp:

```text
name ASC, id ASC
```

`id` là tie-breaker. Nếu hai place trùng tên, thứ tự vẫn xác định.

Thứ tự ổn định rất quan trọng với pagination. Nếu không có sorting ổn định,
record có thể xuất hiện lặp lại hoặc bị bỏ qua giữa hai trang.

### 10.3 Luồng `getActivePlaces`

```java
Page<PlaceSummaryResponse> result = placeRepository
    .findAllByActiveTrue(PageRequest.of(page, size, PLACE_SORT))
    .map(placeMapper::toSummaryResponse);
```

Từng bước:

1. `PageRequest.of(...)` tạo yêu cầu phân trang.
2. Repository truy vấn place active.
3. Repository trả `Page<Place>`.
4. `Page.map(...)` chuyển từng `Place` thành `PlaceSummaryResponse`.
5. Metadata pagination được giữ lại.

Sau đó service tạo DTO API:

```java
return new PlacePageResponse(
    result.getContent(),
    result.getNumber(),
    result.getSize(),
    result.getTotalElements(),
    result.getTotalPages(),
    result.isFirst(),
    result.isLast()
);
```

---

## 11. Controller

File:

```text
backend/src/main/java/com/saigonplantravel/backend/place/controller/PlaceController.java
```

### 11.1 Mapping URL

```java
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {
}
```

`@RestController` tương đương ý tưởng:

```text
@Controller + tự serialize return value thành JSON
```

`@RequestMapping` định nghĩa base path.

### 11.2 GET endpoint

```java
@GetMapping
public PlacePageResponse getPlaces(...) {
}
```

Kết hợp với base path, endpoint là:

```http
GET /api/v1/places
```

### 11.3 Query parameters

```java
@RequestParam(defaultValue = "0") int page,
@RequestParam(defaultValue = "20") int size
```

Request:

```http
GET /api/v1/places?page=2&size=10
```

sẽ tạo:

```text
page = 2
size = 10
```

Nếu client không truyền, Spring dùng:

```text
page = 0
size = 20
```

Nếu client truyền:

```http
?page=abc
```

Spring không thể chuyển `"abc"` thành `int` và ném
`MethodArgumentTypeMismatchException`.

### 11.4 Validation

```java
if (page < 0) {
    throw new InvalidPaginationException(...);
}
```

```java
if (size < 1 || size > 100) {
    throw new InvalidPaginationException(...);
}
```

Controller chỉ kiểm tra input HTTP đơn giản rồi gọi service. Nó không truy vấn
database và không mapping entity.

### 11.5 Strict Boolean binding: phân biệt request `Boolean` và response `boolean`

Source liên quan:

- [`PlaceController.java`](../../backend/src/main/java/com/saigonplantravel/backend/place/controller/PlaceController.java)
- [`PlaceSearchRequest.java`](../../backend/src/main/java/com/saigonplantravel/backend/place/dto/PlaceSearchRequest.java)
- [`PlaceDetailResponse.java`](../../backend/src/main/java/com/saigonplantravel/backend/place/dto/PlaceDetailResponse.java)
- [`PlaceControllerTest.java`](../../backend/src/test/java/com/saigonplantravel/backend/place/controller/PlaceControllerTest.java)
- [FEAT-003 Place search/filter/pagination](../01-Requirements/Features/FEAT-003-place-search-filter-pagination.md)

Hai hướng dữ liệu phải được tách riêng:

```text
Query string "false"
    -> WebDataBinder
    -> PlaceSearchRequest.indoor (Boolean)
    -> search predicate

Place entity indoor
    -> PlaceMapper
    -> PlaceDetailResponse.indoor (boolean)
    -> Jackson
    -> JSON true/false
```

`configureStrictBooleanBinding` và `StrictBooleanEditor` chỉ tham gia hướng
đầu tiên. Chúng đọc query parameter của `GET /api/v1/places`, không đọc hoặc
serialize `PlaceDetailResponse`.

#### Vì sao cần editor riêng?

Boolean editor mặc định của Spring chấp nhận nhiều cách viết như `true`,
`on`, `yes`, `1` và các giá trị false tương ứng; việc so sánh chữ cũng không
phân biệt hoa thường. Contract của Place API hẹp hơn: chỉ chấp nhận đúng
`true` hoặc `false` viết thường.

```java
@InitBinder
void configureStrictBooleanBinding(WebDataBinder binder) {
    binder.registerCustomEditor(
            Boolean.class,
            "indoor",
            new StrictBooleanEditor()
    );
}
```

Ba đối số có nghĩa là:

- editor áp dụng cho giá trị đích kiểu `Boolean`;
- chỉ áp dụng cho property tên `indoor`;
- dùng quy tắc parse do `StrictBooleanEditor` định nghĩa.

```java
if (text == null || text.isEmpty()) {
    setValue(null);
} else if ("true".equals(text)) {
    setValue(Boolean.TRUE);
} else if ("false".equals(text)) {
    setValue(Boolean.FALSE);
} else {
    throw new IllegalArgumentException("indoor must be true or false");
}
```

Do dùng `String.equals` theo đúng case và không `trim`, các giá trị như `yes`,
`1`, `TRUE` hoặc ` true ` đều bị từ chối. Lỗi conversion được error handler
chuyển thành `400 INVALID_REQUEST`. Controller test khóa hành vi `yes` và
`TRUE` phải bị từ chối.

#### Tại sao request dùng `Boolean`, không dùng `boolean`?

`indoor` là filter tùy chọn nên cần ba trạng thái:

| Giá trị Java | Ý nghĩa tìm kiếm |
|---|---|
| `null` | Client không yêu cầu lọc indoor/outdoor |
| `true` | Chỉ lấy địa điểm indoor |
| `false` | Chỉ lấy địa điểm outdoor |

Primitive `boolean` chỉ có hai trạng thái. Nếu đổi
`PlaceSearchRequest.indoor` thành `boolean`, request không truyền `indoor`
không còn cách biểu diễn “không lọc”. Tùy cơ chế tạo/binding object, giá trị
thiếu phải trở thành mặc định `false` hoặc gây lỗi binding; cả hai đều không
đúng contract filter tùy chọn. Đây mới là nơi khác biệt `Boolean`/`boolean`
ảnh hưởng trực tiếp tới binder và nghiệp vụ.

#### Nếu đổi `PlaceDetailResponse.indoor` thành `Boolean` thì sao?

Không thể bỏ hai phương thức binding chỉ vì thay đổi này. `PlaceDetailResponse`
là DTO **đầu ra**, còn binder được đăng ký cho `PlaceSearchRequest` **đầu vào**.
Hai việc độc lập với nhau.

Thay kiểu response chỉ thay đổi khả năng biểu diễn JSON:

| Kiểu trong response | Giá trị Java có thể có | JSON có thể trả |
|---|---|---|
| `boolean` | `true`, `false` | `true`, `false` |
| `Boolean` | `true`, `false`, `null` | `true`, `false`, `null` |

Database hiện đặt `places.indoor` là `NOT NULL`, nên `boolean` trong response
thể hiện contract chặt hơn: detail luôn phải nói rõ indoor hay outdoor. Dùng
`Boolean` sẽ cho phép mapper hoặc dữ liệu lỗi truyền `null` ra public API; nó
chỉ phù hợp nếu nghiệp vụ thật sự có trạng thái “chưa biết”. Nếu nguồn luôn
không null, autounboxing từ `Boolean` entity sang `boolean` response hoạt động
bình thường; nếu nguồn bất ngờ null, autounboxing ném
`NullPointerException`, giúp lộ vi phạm invariant thay vì âm thầm trả `null`.

#### Khi nào nên và không nên dùng cách này?

Nên giữ strict editor khi API chủ ý khóa lexical contract của query parameter
thành đúng hai chuỗi lowercase. Không cần custom editor nếu API chấp nhận quy
tắc Boolean rộng mặc định của Spring.

Các lựa chọn khác là nhận `String` rồi validate/parse, hoặc tạo
`Converter`/`Formatter`. Nhận `String` làm DTO kém đúng kiểu hơn;
converter toàn cục có thể vô tình thay đổi mọi Boolean parameter. Editor hiện
tại được scope theo property `indoor`, nên thay đổi nhỏ và ít ảnh hưởng nhất.

---

## 12. Error handling

### 12.1 Custom exception

```java
public class InvalidPaginationException extends RuntimeException {
}
```

Exception này biểu diễn lỗi pagination do client gửi.

### 12.2 `GlobalExceptionHandler`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

`@RestControllerAdvice` cho phép bắt exception từ tất cả controller.

### 12.3 HTTP 400

```java
@ExceptionHandler({
    InvalidPaginationException.class,
    MethodArgumentTypeMismatchException.class
})
```

Hai trường hợp được chuyển thành HTTP 400:

- số nằm ngoài giới hạn;
- giá trị không chuyển được thành integer.

Response dùng `ProblemDetail`:

```json
{
  "type": "about:blank",
  "title": "Invalid pagination parameters",
  "status": 400,
  "detail": "size must be between 1 and 100",
  "instance": "/api/v1/places"
}
```

### 12.4 HTTP 500

```java
@ExceptionHandler(Exception.class)
```

Đây là fallback cho lỗi ngoài dự kiến.

Chi tiết exception được ghi vào log:

```java
log.error("Unexpected error while handling {}", request.getRequestURI(), exception);
```

Client chỉ nhận:

```json
{
  "title": "Internal server error",
  "status": 500,
  "detail": "An unexpected error occurred"
}
```

Không trả SQL, stack trace, username, password hoặc JDBC URL cho client.

---

## 13. Cách chạy ứng dụng để test thủ công

### 13.1 Điều kiện cần

Cần có:

- Java 21;
- Docker;
- Docker Compose;
- file `.env` ở root với biến cấu hình phù hợp.

Kiểm tra:

```bash
java -version
docker version
docker compose version
```

### 13.2 Khởi động PostgreSQL

Từ root repository:

```bash
docker compose --env-file .env -f infra/compose.yaml up -d postgres
```

Kiểm tra container:

```bash
docker compose --env-file .env -f infra/compose.yaml ps
```

Xem log:

```bash
docker compose --env-file .env -f infra/compose.yaml logs postgres
```

Container cần đạt trạng thái healthy.

### 13.3 Chạy Spring Boot từ IntelliJ

Mở:

```text
BackendApplication.java
```

Tạo Run Configuration và cung cấp ba biến môi trường:

```text
JDBC_DB_URL
JDBC_DB_USERNAME
JDBC_DB_PASSWORD
```

Sau đó Run `BackendApplication`.

Khi thành công, log phải cho thấy:

- Flyway migration hoàn tất;
- Hibernate khởi tạo thành công;
- Tomcat chạy trên port 8080.

### 13.4 Chạy từ terminal

Export các biến `JDBC_DB_*` trước, sau đó:

```bash
cd backend
./mvnw spring-boot:run
```

Không in password hoặc nội dung `.env` ra terminal log dùng chung.

---

## 14. Test API thủ công bằng curl

### 14.1 Health check

```bash
curl -i http://localhost:8080/actuator/health
```

Kỳ vọng:

```text
HTTP/1.1 200
```

### 14.2 Pagination mặc định

```bash
curl -i "http://localhost:8080/api/v1/places"
```

Kỳ vọng:

- HTTP 200;
- `page` bằng 0;
- `size` bằng 20;
- `totalElements` bằng 5 với seed hiện tại;
- không có `demo-temporarily-hidden-place`.

### 14.3 Pagination tùy chỉnh

```bash
curl -i "http://localhost:8080/api/v1/places?page=0&size=2"
```

Kỳ vọng:

- `content` có 2 item;
- `totalElements` bằng 5;
- `totalPages` bằng 3;
- `first` bằng true;
- item được sắp xếp theo tên.

Trang cuối:

```bash
curl -i "http://localhost:8080/api/v1/places?page=2&size=2"
```

Kỳ vọng có 1 item.

### 14.4 Trang vượt giới hạn

```bash
curl -i "http://localhost:8080/api/v1/places?page=100&size=20"
```

Kỳ vọng:

- HTTP 200;
- `content` là mảng rỗng.

### 14.5 Page âm

```bash
curl -i "http://localhost:8080/api/v1/places?page=-1"
```

Kỳ vọng:

- HTTP 400;
- content type `application/problem+json`;
- title là `Invalid pagination parameters`.

### 14.6 Size không hợp lệ

```bash
curl -i "http://localhost:8080/api/v1/places?size=101"
```

Kỳ vọng HTTP 400.

### 14.7 Sai kiểu dữ liệu

```bash
curl -i "http://localhost:8080/api/v1/places?page=abc"
```

Kỳ vọng:

- HTTP 400;
- detail là `page and size must be valid integers`.

---

## 15. Dừng và xóa môi trường local

Dừng container nhưng giữ data volume:

```bash
docker compose --env-file .env -f infra/compose.yaml down
```

Xóa cả data volume chỉ khi thật sự muốn tạo lại database từ đầu:

```bash
docker compose --env-file .env -f infra/compose.yaml down -v
```

Lệnh `down -v` xóa dữ liệu PostgreSQL local. Không dùng nếu còn dữ liệu cần
giữ.

---

## 16. JUnit dành cho người đã biết pytest

### 16.1 So sánh nhanh

| pytest | JUnit/Java |
|---|---|
| `def test_x():` | method có `@Test` |
| `assert value == 1` | AssertJ `assertThat(value).isEqualTo(1)` |
| fixture | `@BeforeEach`, Spring Context hoặc Testcontainers |
| `monkeypatch`/mock | Mockito |
| Flask/FastAPI test client | MockMvc |
| pytest plugin | JUnit extension |
| `pytest` | `./mvnw test` |
| `pytest path::test_name` | Maven `-Dtest=ClassName#methodName` |

JUnit test cơ bản:

```java
class CalculatorTest {

    @Test
    void addsTwoNumbers() {
        int result = 1 + 2;

        assertThat(result).isEqualTo(3);
    }
}
```

JUnit không yêu cầu method bắt đầu bằng `test_`; annotation `@Test` đánh dấu
test.

### 16.2 Arrange, Act, Assert

Nên đọc test theo ba phần:

```text
Arrange → chuẩn bị dữ liệu và mock
Act     → gọi code cần test
Assert  → kiểm tra kết quả
```

Ví dụ:

```java
// Arrange
when(repository.findSomething()).thenReturn(data);

// Act
Result result = service.doSomething();

// Assert
assertThat(result).isEqualTo(expected);
```

---

## 17. Mockito

Mockito tạo object giả để test một class mà không gọi dependency thật.

Ví dụ service phụ thuộc repository:

```java
@Mock
private PlaceRepository placeRepository;
```

Mockito không kết nối PostgreSQL. Nó tạo repository giả.

### 17.1 Stubbing với `when`

```java
when(placeMapper.toSummaryResponse(place)).thenReturn(summary);
```

Nghĩa là:

> Khi mapper giả nhận `place`, hãy trả `summary`.

Gần giống Python:

```python
mapper.to_summary_response.return_value = summary
```

### 17.2 Verify

```java
verify(placeRepository).findAllByActiveTrue(pageable);
```

Nghĩa là kiểm tra repository đã được gọi.

Gần giống:

```python
repository.find_all.assert_called_once_with(pageable)
```

### 17.3 ArgumentCaptor

```java
ArgumentCaptor<Pageable> captor =
    ArgumentCaptor.forClass(Pageable.class);

verify(placeRepository)
    .findAllByActiveTrue(captor.capture());
```

Captor lấy argument thực tế được truyền vào mock. Test dùng nó để kiểm tra
service đã khóa sorting:

```text
name: ASC,id: ASC
```

---

## 18. Giải thích từng test hiện tại

### 18.1 `PlaceMapperTest`

Mục tiêu:

- mapper đọc đúng field từ entity;
- tạo DTO đúng 11 field;
- giữ `shortDescription = null`.

Test mock entity vì mapper chỉ cần gọi getter, không cần database.

Đây là unit test thuần:

```text
Không Spring Context
Không HTTP
Không PostgreSQL
```

### 18.2 `PlaceServiceTest`

Annotation:

```java
@ExtendWith(MockitoExtension.class)
```

JUnit dùng Mockito extension để khởi tạo các field `@Mock`.

Dependencies giả:

```java
@Mock
private PlaceRepository placeRepository;

@Mock
private PlaceMapper placeMapper;
```

Class thật đang được test:

```java
new PlaceService(placeRepository, placeMapper)
```

Test kiểm tra:

- service tạo page response đúng;
- metadata đúng;
- repository nhận sorting `name ASC, id ASC`.

Đây cũng là unit test, không khởi động Spring.

### 18.3 `PlaceControllerTest`

Annotation:

```java
@WebMvcTest(PlaceController.class)
```

Spring chỉ khởi động phần web cần cho controller, không khởi động toàn bộ JPA
và database.

```java
@MockitoBean
private PlaceService placeService;
```

Spring đưa mock service vào Application Context để controller dùng.

```java
@Import(GlobalExceptionHandler.class)
```

Đưa error handler thật vào web test.

`MockMvc` giả lập HTTP request:

```java
mockMvc.perform(get("/api/v1/places"))
```

Nó không mở port 8080 và không cần chạy server thật.

Kiểm tra HTTP status:

```java
.andExpect(status().isOk())
```

Kiểm tra JSON bằng JSONPath:

```java
.andExpect(jsonPath("$.content[0].name").value("Demo Place"))
```

Ý nghĩa JSONPath:

- `$`: root JSON;
- `.content`: field content;
- `[0]`: phần tử đầu tiên;
- `.name`: field name.

Năm controller tests hiện tại kiểm tra:

1. pagination mặc định và 11 response fields;
2. pagination tùy chỉnh và trang rỗng;
3. page/size ngoài giới hạn trả 400;
4. page/size sai kiểu trả 400;
5. lỗi ngoài dự kiến trả 500 và không lộ JDBC URL.

### 18.4 `PlaceRepositoryTest`

Đây là integration test.

Nó dùng:

```java
@SpringBootTest
@Testcontainers
```

`@SpringBootTest` khởi động gần như toàn bộ Spring Boot application.

`@Testcontainers` tích hợp vòng đời Docker container với JUnit.

Container:

```java
@Container
static final PostgreSQLContainer postgres =
    new PostgreSQLContainer(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres")
    );
```

Khi test chạy:

1. Testcontainers khởi động container pgvector;
2. PostgreSQL dùng port ngẫu nhiên;
3. Testcontainers cung cấp JDBC URL, username và password;
4. Spring kết nối vào container;
5. Flyway chạy V1–V6;
6. Hibernate validate entity;
7. test gọi repository thật;
8. container bị dừng sau test.

`@DynamicPropertySource` thay datasource của application bằng datasource của
container:

```java
registry.add("spring.datasource.url", postgres::getJdbcUrl);
```

Test kiểm tra:

- có đúng 5 place active;
- page size 2 tạo 3 trang;
- sorting đúng;
- place inactive không xuất hiện.

### 18.5 `BackendApplicationTests`

Đây là smoke integration test cho Application Context.

Method test trống:

```java
@Test
void contextLoadsWithFlywayAndHibernateValidation() {
}
```

Nó không thật sự “không làm gì”. Nếu Spring không thể:

- tạo bean;
- kết nối PostgreSQL;
- chạy Flyway;
- validate entity;

thì Application Context không khởi động được và test thất bại trước khi method
hoàn tất.

---

## 19. Testcontainers khác Docker Compose thế nào?

Docker Compose dùng để chạy database trong lúc phát triển thủ công:

```text
Bạn khởi động → database tiếp tục chạy → bạn tự dừng
```

Testcontainers dùng trong automated test:

```text
JUnit khởi động → chạy test → tự dừng container
```

Testcontainers có ưu điểm:

- test độc lập với database local;
- database sạch và có thể tái tạo;
- dùng PostgreSQL thật thay vì H2;
- kiểm tra được Flyway và constraint thật;
- phù hợp CI.

Testcontainers yêu cầu Docker đang chạy.

---

## 20. Các lệnh chạy test

Chạy từ thư mục backend:

```bash
cd backend
```

### 20.1 Chạy toàn bộ test

```bash
./mvnw test
```

Kết quả hiện tại:

```text
Tests run: 9
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### 20.2 Chỉ compile production code

```bash
./mvnw -DskipTests compile
```

### 20.3 Chạy một test class

```bash
./mvnw -Dtest=PlaceServiceTest test
```

```bash
./mvnw -Dtest=PlaceControllerTest test
```

```bash
./mvnw -Dtest=PlaceRepositoryTest test
```

### 20.4 Chạy một test method

```bash
./mvnw \
  -Dtest=PlaceControllerTest#rejectsNonNumericPaginationWithProblemDetail \
  test
```

### 20.5 Chạy nhóm unit/web test không cần Docker

```bash
./mvnw \
  -Dtest=PlaceMapperTest,PlaceServiceTest,PlaceControllerTest \
  test
```

### 20.6 Xem test report

Maven Surefire ghi report tại:

```text
backend/target/surefire-reports/
```

Các file `.txt` có summary dễ đọc. File `.xml` thường được CI sử dụng.

---

## 21. Cách đọc kết quả Maven

Thành công:

```text
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Phân biệt:

- `Failure`: assertion không đúng.
- `Error`: test không chạy xong vì exception hoặc lỗi môi trường.
- `Skipped`: test bị bỏ qua.

Ví dụ failure:

```text
Expected: 5
Actual: 6
```

Nghĩa là code chạy được nhưng kết quả sai.

Ví dụ error:

```text
Could not find a valid Docker environment
```

Nghĩa là Testcontainers không truy cập được Docker; chưa chắc production code
bị sai.

---

## 22. Các lỗi thường gặp

### 22.1 Docker chưa chạy

Thông báo:

```text
Could not find a valid Docker environment
```

Kiểm tra:

```bash
docker version
docker ps
```

### 22.2 Port PostgreSQL bị chiếm

Docker Compose có thể báo port đã được dùng. Testcontainers thường không gặp
vấn đề này vì nó chọn port ngẫu nhiên.

### 22.3 Thiếu biến môi trường khi chạy application

Thông báo có thể liên quan:

```text
JDBC_DB_URL
url must start with "jdbc"
```

Đây là lỗi chạy application local. Testcontainers tự cung cấp datasource cho
integration test.

### 22.4 Flyway checksum mismatch

Nguyên nhân thường là migration đã chạy bị sửa.

Không sửa V1–V6 sau khi đã dùng trong database cần giữ. Khi cần thay đổi schema,
tạo migration version mới.

### 22.5 Hibernate schema validation failed

Nguyên nhân:

- entity thiếu/sai kiểu column;
- độ dài hoặc nullability không khớp;
- migration chưa chạy;
- kết nối nhầm database.

### 22.6 Test kỳ vọng sai vì seed thay đổi

Repository test đang kỳ vọng:

```text
5 active places
```

Nếu seed được thay đổi có chủ ý, phải cập nhật feature spec và test tương ứng.

---

## 23. Trình tự học code được đề xuất

Không nên đọc mọi file cùng lúc. Đọc theo thứ tự:

1. `V2__create_places_table.sql`
2. `V6__seed_demo_places.sql`
3. `Place.java`
4. `PlaceRepository.java`
5. `PlaceSummaryResponse.java`
6. `PlacePageResponse.java`
7. `PlaceMapper.java`
8. `PlaceService.java`
9. `PlaceController.java`
10. `GlobalExceptionHandler.java`
11. `PlaceMapperTest.java`
12. `PlaceServiceTest.java`
13. `PlaceControllerTest.java`
14. `PlaceRepositoryTest.java`
15. `BackendApplicationTests.java`

Sau mỗi file, trả lời ba câu:

1. Input của class/method là gì?
2. Output là gì?
3. Class này có được phép biết tầng bên dưới hay bên trên không?

Ví dụ controller:

- biết service;
- không biết repository;
- không biết entity;
- không viết SQL.

---

## 24. Bài tập tự luyện

Các bài tập dưới đây không cần thay đổi production behavior.

### Bài 1: Viết lại luồng request

Tự mô tả bằng lời:

```text
GET /api/v1/places?page=1&size=2
```

đi qua class nào, method nào và tạo query như thế nào.

### Bài 2: Dự đoán metadata

Với 5 place active và `size=2`, tự tính:

- `totalPages`;
- content size của page 0;
- content size của page 1;
- content size của page 2;
- kết quả page 3.

### Bài 3: Viết một controller test

Thêm test cho:

```http
GET /api/v1/places?size=0
```

Kỳ vọng HTTP 400.

### Bài 4: Viết repository assertion

Kiểm tra tất cả place trả về đều có:

```java
Place::getActive
```

bằng `true`.

### Bài 5: Làm test thất bại có chủ ý

Tạm đổi một assertion từ 5 thành 6, chạy test, đọc failure report, sau đó hoàn
tác thay đổi. Mục tiêu là làm quen với cách JUnit báo lỗi.

---

## 25. Checklist test F01 trước khi bàn giao

### Automated tests

```bash
cd backend
./mvnw test
```

Xác nhận:

- [ ] `BUILD SUCCESS`
- [ ] 9 tests chạy
- [ ] 0 failures
- [ ] 0 errors
- [ ] 0 skipped

### Manual smoke test

- [ ] PostgreSQL container healthy
- [ ] Spring Boot khởi động thành công
- [ ] `/actuator/health` trả 200
- [ ] `/api/v1/places` trả 200
- [ ] có đúng 5 active demo places
- [ ] không trả inactive demo place
- [ ] `page=-1` trả 400
- [ ] `size=101` trả 400
- [ ] `page=abc` trả 400
- [ ] trang vượt cuối trả 200 với content rỗng

---

## 26. Tóm tắt cần nhớ

- Spring Boot tự cấu hình và khởi động các thành phần Spring.
- Controller xử lý HTTP, service xử lý nghiệp vụ, repository truy cập database.
- Entity ánh xạ database; DTO định nghĩa API contract.
- Không trả entity trực tiếp.
- Flyway quản lý schema; Hibernate chỉ validate.
- Unit test cô lập class bằng mock.
- `@WebMvcTest` kiểm tra tầng HTTP mà không cần database.
- Testcontainers kiểm tra với PostgreSQL thật trong Docker.
- `./mvnw test` tương đương vai trò chính của lệnh `pytest`.
- Test tốt không chỉ kiểm tra happy path mà còn kiểm tra input sai, trang rỗng,
  sorting, inactive data và lỗi hệ thống.
