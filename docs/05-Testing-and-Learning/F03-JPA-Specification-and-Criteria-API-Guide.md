# F03 — JPA Specification và Criteria API trong Place Search

> [!summary]
> Tài liệu này giải thích toàn bộ cách `PlaceSpecifications` hoạt động trong
> FEAT-003, mối quan hệ giữa Spring Data JPA `Specification` và Criteria API,
> lý do lựa chọn Specification, SQL tương ứng và các trường hợp nên hoặc không
> nên sử dụng cách tiếp cận này.

## 1. Mục tiêu học tập

Sau khi đọc tài liệu, người học có thể:

- hiểu `Specification<T>` là gì;
- nhận ra Specification vẫn sử dụng `CriteriaBuilder` và `Predicate`;
- đọc được toàn bộ `PlaceSpecifications` hiện tại;
- hiểu cách Spring Data tạo content query và count query;
- hiểu dynamic filtering, category join, `distinct`, `unaccent` và bind parameter;
- so sánh Specification với derived query, `@Query` và custom repository;
- đánh giá việc dùng Specification trong FEAT-003 có quá phức tạp hay không.

## 2. Source code liên quan

- `backend/src/main/java/com/saigonplantravel/backend/place/repository/specification/PlaceSpecifications.java`
- `backend/src/main/java/com/saigonplantravel/backend/place/repository/PlaceRepository.java`
- `backend/src/main/java/com/saigonplantravel/backend/place/service/PlaceService.java`
- `backend/src/main/java/com/saigonplantravel/backend/place/dto/PlaceSearchRequest.java`
- `backend/src/main/java/com/saigonplantravel/backend/place/search/PlaceSearchNormalizer.java`
- `backend/src/main/resources/db/migration/V9__enable_unaccent_and_place_search_index.sql`
- `docs/01-Requirements/Features/FEAT-003-place-search-filter-pagination.md`

## 3. Specification là gì?

Trong Spring Data JPA, `Specification<T>` là một object mô tả điều kiện truy
vấn cho một Entity. Có thể hiểu gần đúng interface này như sau:

```java
@FunctionalInterface
public interface Specification<T> {

    Predicate toPredicate(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder
    );
}
```

Một `Specification<Place>` nhận ba thành phần quen thuộc của Criteria API và
trả về một `Predicate`:

```java
Specification<Place> activePlaces = (root, query, cb) ->
        cb.isTrue(root.get("active"));
```

Specification không thay thế Criteria API. Nó đóng gói Criteria API thành một
object mà Spring Data có thể nhận và thực thi.

## 4. Mối quan hệ với CriteriaBuilder đã biết

### 4.1. Criteria API thủ công

Khi dùng `EntityManager` trực tiếp, code thường có dạng:

```java
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<Place> query = cb.createQuery(Place.class);
Root<Place> root = query.from(Place.class);

List<Predicate> predicates = new ArrayList<>();
predicates.add(cb.isTrue(root.get("active")));

if (keyword != null) {
    predicates.add(/* keyword predicate */);
}

query.where(cb.and(predicates.toArray(Predicate[]::new)));

TypedQuery<Place> typedQuery = entityManager.createQuery(query);
typedQuery.setFirstResult(page * size);
typedQuery.setMaxResults(size);

List<Place> content = typedQuery.getResultList();
```

Để trả response có pagination metadata, lập trình viên còn phải tự viết count
query tương ứng.

### 4.2. Cùng logic dưới dạng Specification

```java
Specification<Place> specification = (root, query, cb) -> {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.isTrue(root.get("active")));

    if (keyword != null) {
        predicates.add(/* keyword predicate */);
    }

    return cb.and(predicates.toArray(Predicate[]::new));
};
```

Phần tạo predicates gần như không thay đổi. Điểm khác là Spring Data đảm nhiệm:

- tạo `CriteriaQuery` và `Root`;
- tạo content query;
- tạo count query;
- thêm sorting;
- thêm offset và limit;
- thực thi query;
- trả về `Page<Place>`.

Vì vậy có thể ghi nhớ:

```text
CriteriaBuilder tạo điều kiện
Specification đóng gói điều kiện
JpaSpecificationExecutor thực thi điều kiện
Pageable bổ sung sorting và pagination
```

## 5. Điều kiện để repository dùng Specification

`PlaceRepository` kế thừa cả hai interface:

```java
public interface PlaceRepository
        extends JpaRepository<Place, Long>,
                JpaSpecificationExecutor<Place> {
}
```

`JpaRepository` cung cấp CRUD thông thường. `JpaSpecificationExecutor` bổ sung
các method nhận Specification, ví dụ:

```java
Page<Place> findAll(
        Specification<Place> specification,
        Pageable pageable
);
```

Service sử dụng method đó:

```java
Page<PlaceSummaryResponse> result = placeRepository.findAll(
                PlaceSpecifications.matching(request),
                PageRequest.of(
                        request.resolvedPage(),
                        request.resolvedSize(),
                        PLACE_SORT
                )
        )
        .map(placeMapper::toSummaryResponse);
```

Luồng thực thi:

```text
PlaceSearchRequest
        ↓
PlaceSpecifications.matching(request)
        ↓
Specification<Place>
        ↓
JpaSpecificationExecutor.findAll(specification, pageable)
        ↓
Hibernate tạo content query và count query
        ↓
Page<Place>
        ↓
PlacePageResponse
```

## 6. Cấu trúc `PlaceSpecifications`

```java
public final class PlaceSpecifications {

    private PlaceSpecifications() {
    }
}
```

Đây là utility class chỉ chứa static methods:

- `final` ngăn class khác kế thừa;
- constructor `private` ngăn `new PlaceSpecifications()`;
- caller sử dụng trực tiếp `PlaceSpecifications.matching(request)`.

Class không giữ state và không cần trở thành Spring bean.

## 7. Method `matching()`

Method chính nhận request rồi tạo Specification:

```java
public static Specification<Place> matching(PlaceSearchRequest request) {
    return (root, query, criteriaBuilder) -> {
        List<Predicate> predicates = new ArrayList<>();

        // Thêm các predicate phù hợp.

        return criteriaBuilder.and(
                predicates.toArray(Predicate[]::new)
        );
    };
}
```

Lambda này implement `Specification.toPredicate()`.

### 7.1. `root`

`Root<Place>` đại diện cho Entity/bảng chính:

```sql
FROM places p
```

Ví dụ:

```java
root.get("active")
```

tương ứng với:

```sql
p.active
```

### 7.2. `query`

`CriteriaQuery<?>` đại diện cho toàn bộ query đang được xây dựng. FEAT-003 dùng
nó để bật distinct khi join category:

```java
query.distinct(true);
```

### 7.3. `criteriaBuilder`

`CriteriaBuilder` là factory để tạo biểu thức và điều kiện:

```java
criteriaBuilder.isTrue(...)
criteriaBuilder.equal(...)
criteriaBuilder.like(...)
criteriaBuilder.lessThanOrEqualTo(...)
criteriaBuilder.and(...)
criteriaBuilder.or(...)
criteriaBuilder.function(...)
```

## 8. Danh sách predicates và AND semantics

```java
List<Predicate> predicates = new ArrayList<>();
```

Mỗi filter có mặt sẽ thêm một `Predicate`. Cuối cùng:

```java
return criteriaBuilder.and(
        predicates.toArray(Predicate[]::new)
);
```

Ví dụ danh sách chứa:

```text
active = true
district = "quan 1"
indoor = true
minCost <= 100000
```

SQL logic tương ứng:

```sql
WHERE active = true
  AND district = 'quan 1'
  AND indoor = true
  AND min_cost <= 100000
```

`Predicate[]::new` là array constructor reference, dùng để chuyển
`List<Predicate>` thành `Predicate[]`. Nó thay thế cách viết cũ:

```java
predicates.toArray(new Predicate[0])
```

## 9. Active predicate bắt buộc

```java
predicates.add(
        criteriaBuilder.isTrue(root.get("active"))
);
```

Predicate này luôn xuất hiện, kể cả request không có filter:

```sql
WHERE p.active = true
```

Đây là business invariant của public Place Catalog. Inactive place không được
trả về và không được tính trong `totalElements`.

## 10. Optional predicates

Các filter chỉ được thêm khi request có giá trị:

```java
if (request.keyword() != null) {
    predicates.add(
            keywordPredicate(root, criteriaBuilder, request.keyword())
    );
}

if (request.district() != null) {
    predicates.add(
            districtPredicate(root, criteriaBuilder, request.district())
    );
}
```

Category, indoor và maxCost cũng theo cùng cách. Do đó request:

```http
GET /api/v1/places?indoor=false
```

chỉ cần query:

```sql
WHERE p.active = true
  AND p.indoor = false
```

Nó không join category và không thêm keyword/district/cost condition.

## 11. Keyword predicate

Method keyword có ba nhiệm vụ:

1. lowercase input ổn định;
2. escape ký tự đặc biệt của SQL LIKE;
3. tạo OR condition trên năm text columns.

### 11.1. Lowercase bằng `Locale.ROOT`

```java
keyword.toLowerCase(Locale.ROOT)
```

`Locale.ROOT` giúp kết quả lowercase không phụ thuộc locale của máy chủ:

```text
"BAO TANG" → "bao tang"
```

### 11.2. Escape LIKE wildcard

```java
String escapedKeyword = PlaceSearchNormalizer.escapeLikePattern(
        keyword.toLowerCase(Locale.ROOT)
);
```

Trong SQL LIKE:

- `%` nghĩa là bất kỳ chuỗi ký tự nào;
- `_` nghĩa là một ký tự bất kỳ;
- `\` được dùng làm escape character.

FEAT-003 coi `%`, `_` và `\` do client gửi là ký tự literal. Ví dụ:

```text
50% → 50\%
```

Client không được biến keyword thành wildcard tùy ý.

### 11.3. Substring pattern

```java
"%" + escapedKeyword + "%"
```

Ví dụ:

```text
"bao tang" → "%bao tang%"
```

Hai dấu `%` do server thêm cho phép keyword nằm ở bất kỳ vị trí nào trong cột.

### 11.4. OR trên năm columns

```java
return criteriaBuilder.or(
        literalSubstring(criteriaBuilder, root.get("name"), pattern),
        literalSubstring(criteriaBuilder, root.get("shortDescription"), pattern),
        literalSubstring(criteriaBuilder, root.get("fullDescription"), pattern),
        literalSubstring(criteriaBuilder, root.get("address"), pattern),
        literalSubstring(criteriaBuilder, root.get("district"), pattern)
);
```

SQL tương ứng gần giống:

```sql
WHERE
       unaccent(lower(name)) LIKE unaccent(?) ESCAPE '\'
    OR unaccent(lower(short_description)) LIKE unaccent(?) ESCAPE '\'
    OR unaccent(lower(full_description)) LIKE unaccent(?) ESCAPE '\'
    OR unaccent(lower(address)) LIKE unaccent(?) ESCAPE '\'
    OR unaccent(lower(district)) LIKE unaccent(?) ESCAPE '\'
```

Chỉ cần một trong năm cột match là place phù hợp. Cột `NULL` không làm query
lỗi; biểu thức LIKE của cột đó không match và các nhánh OR khác vẫn được xét.

## 12. `literalSubstring()`

Helper này tránh lặp lại cùng một LIKE expression năm lần:

```java
private static Predicate literalSubstring(
        CriteriaBuilder criteriaBuilder,
        Expression<String> column,
        Expression<String> pattern
) {
    return criteriaBuilder.like(
            normalizedColumn(criteriaBuilder, column),
            pattern,
            PlaceSearchNormalizer.LIKE_ESCAPE_CHARACTER
    );
}
```

Ba thành phần là:

- cột đã `lower` và `unaccent`;
- pattern đã escape và được bind;
- ký tự `\` khai báo cho SQL `ESCAPE`.

SQL gần tương đương:

```sql
unaccent(lower(p.name)) LIKE unaccent(?) ESCAPE '\'
```

## 13. District predicate

```java
return criteriaBuilder.equal(
        normalizedColumn(criteriaBuilder, root.get("district")),
        normalizedLiteral(
                criteriaBuilder,
                district.toLowerCase(Locale.ROOT)
        )
);
```

District dùng `equal`, không dùng `like`:

```sql
unaccent(lower(p.district)) = unaccent(?)
```

Kết quả:

```text
"quan 1" khớp "Quận 1"
"1" không khớp "Quận 1"
```

Đây là exact normalized match, không phải substring search.

## 14. Category join

Category chỉ được join khi request có `category`:

```java
Join<Place, Category> categories = root.join("categories");

predicates.add(
        criteriaBuilder.equal(
                categories.get("slug"),
                request.category()
        )
);
```

SQL gần tương đương:

```sql
JOIN place_categories pc ON pc.place_id = p.id
JOIN categories c ON c.id = pc.category_id
WHERE c.slug = ?
```

Không dùng fetch join. List response không truy cập lazy `categories`, vì vậy
query không tạo N+1 cho summary mapping.

## 15. Tại sao category cần `distinct(true)`?

Place–Category là quan hệ many-to-many. Một place có thể liên kết nhiều
category. Join có thể tạo nhiều SQL rows cho cùng một place:

```text
place 1 | category Nghệ thuật
place 1 | category Văn hóa
```

Code bật:

```java
query.distinct(true);
```

Điều này bảo đảm:

- một Place chỉ xuất hiện một lần trong content;
- count query tính distinct places;
- `totalElements` không bị tăng do số relation rows.

Hibernate tạo query gần giống:

```sql
SELECT DISTINCT p.*
```

và count query gần giống:

```sql
SELECT COUNT(DISTINCT p.id)
```

## 16. Indoor predicate và lý do kiểm tra `null`

```java
if (request.indoor() != null) {
    predicates.add(
            criteriaBuilder.equal(
                    root.get("indoor"),
                    request.indoor()
            )
    );
}
```

`Boolean` có ba trạng thái:

```text
null  → không lọc indoor/outdoor
true  → chỉ lấy indoor places
false → chỉ lấy outdoor places
```

Phải kiểm tra `!= null`, không được viết:

```java
if (request.indoor()) {
```

vì cách đó vừa có thể gây `NullPointerException`, vừa xử lý sai
`indoor=false`.

SQL:

```sql
p.indoor = ?
```

## 17. Cost predicate

```java
predicates.add(
        criteriaBuilder.lessThanOrEqualTo(
                root.get("minCost"),
                request.maxCost()
        )
);
```

SQL:

```sql
p.min_cost <= ?
```

Ví dụ `maxCost=100000` giữ place có `minCost <= 100000`. So sánh là inclusive.

## 18. Chuẩn hóa database column

```java
private static Expression<String> normalizedColumn(
        CriteriaBuilder criteriaBuilder,
        Expression<String> column
) {
    return criteriaBuilder.function(
            "unaccent",
            String.class,
            criteriaBuilder.lower(column)
    );
}
```

`CriteriaBuilder` không có method chuẩn dành riêng cho PostgreSQL `unaccent`,
nên code dùng `function()` để gọi database function:

```sql
unaccent(lower(column))
```

Ví dụ:

```text
"BẢO TÀNG"
    ↓ lower
"bảo tàng"
    ↓ unaccent
"bao tang"
```

`String.class` khai báo kiểu giá trị function trả về.

### 18.1. PostgreSQL `unaccent` là gì?

`unaccent` là extension của PostgreSQL dùng một bộ quy tắc để loại bỏ dấu hoặc
chuyển các ký tự có dấu thành dạng không dấu. Extension cung cấp function
`unaccent(text)` có thể gọi trực tiếp trong SQL.

Ví dụ:

```sql
SELECT unaccent('Bảo tàng Thành phố Hồ Chí Minh');
```

Kết quả có ý nghĩa tương đương:

```text
Bao tang Thanh pho Ho Chi Minh
```

Nó đặc biệt hữu ích khi người dùng nhập tiếng Việt không dấu nhưng dữ liệu lưu
trong PostgreSQL có dấu.

### 18.2. Tại sao phải cài extension?

Function này không tự có sẵn trong mọi database. Flyway V9 của dự án cài nó:

```sql
CREATE EXTENSION IF NOT EXISTS unaccent;
```

`IF NOT EXISTS` giúp migration không lỗi nếu extension đã tồn tại. Việc tạo
extension có thể cần quyền phù hợp trên môi trường deploy; vì vậy quyền cài
extension phải được kiểm tra trước khi release sang database khác.

Extension được cài ở cấp database. Cài trong một database không có nghĩa là
mọi database khác trên cùng PostgreSQL server tự động có function này.

### 18.3. `unaccent` không tự lowercase

`unaccent` chỉ xử lý dấu, không chịu trách nhiệm chuyển hoa thành thường:

```sql
SELECT unaccent('BẢO TÀNG');
```

vẫn có thể cho kết quả chữ hoa:

```text
BAO TANG
```

Vì FEAT-003 cần vừa không phân biệt hoa/thường vừa không phân biệt dấu, code kết
hợp hai function:

```sql
unaccent(lower(column))
```

Luồng biến đổi:

```text
BẢO TÀNG
   ↓ lower
bảo tàng
   ↓ unaccent
bao tang
```

### 18.4. Tại sao phải áp dụng cho cả cột và input?

Giả sử database lưu:

```text
Quận 1
```

Client gửi:

```text
quan 1
```

Nếu chỉ bỏ dấu một phía, hai giá trị vẫn có thể không bằng nhau. Query của dự
án chuẩn hóa cả hai phía:

```sql
unaccent(lower(p.district)) = unaccent(?)
```

Bind parameter `?` chứa input đã lowercase. Khi thực thi:

```text
Database: Quận 1 → quận 1 → quan 1
Input:    quan 1 → quan 1
```

Hai bên mới có thể exact match.

Keyword cũng áp dụng cùng nguyên tắc:

```sql
unaccent(lower(p.name)) LIKE unaccent(?) ESCAPE '\'
```

### 18.5. `unaccent` khác Unicode NFC normalization thế nào?

Hai thao tác giải quyết hai vấn đề khác nhau:

| Thao tác | Mục đích | Ví dụ ý nghĩa |
| --- | --- | --- |
| Unicode NFC | Chuẩn hóa cách các Unicode code point biểu diễn cùng một chữ | Hai cách mã hóa chữ `ấ` trở thành cùng dạng chuẩn |
| `lower` | Không phân biệt hoa/thường | `BẢO` thành `bảo` |
| PostgreSQL `unaccent` | Bỏ dấu khi so sánh trong database | `bảo` thành `bao` |
| trim/collapse whitespace | Chuẩn hóa khoảng trắng người dùng nhập | `  bảo   tàng ` thành `bảo tàng` |

NFC không biến `Bảo tàng` thành `Bao tang`. `unaccent` cũng không thay thế việc
trim, collapse whitespace hoặc validate input. FEAT-003 cần phối hợp các bước.

### 18.6. `unaccent` không làm được gì?

`unaccent` không phải search engine. Nó không tự cung cấp:

- sửa lỗi chính tả;
- fuzzy matching;
- synonym;
- stemming;
- relevance ranking;
- autocomplete;
- full-text search;
- vector similarity.

Ví dụ `unaccent` giúp `bao tang` khớp `Bảo tàng`, nhưng không tự giúp
`bao tan` khớp `Bảo tàng` khi người dùng gõ thiếu chữ `g`.

### 18.7. Ảnh hưởng hiệu năng

Khi query gọi function trên column:

```sql
unaccent(lower(p.name))
```

PostgreSQL thường không thể dùng B-tree index thông thường trên `name` cho biểu
thức đó. Keyword còn dùng dạng substring:

```sql
LIKE '%keyword%'
```

nên B-tree index thường cũng không giúp nhiều. FEAT-003 chấp nhận trade-off này
vì MVP chỉ có khoảng 30–100 places và integration/runtime measurements đều nằm
trong mục tiêu đã khóa.

Nếu dataset tăng lớn, cần đo lại bằng `EXPLAIN (ANALYZE, BUFFERS)` trước khi cân
nhắc normalized column, expression index, trigram hoặc full-text search. Không
nên thêm các tối ưu đó chỉ vì có `unaccent` khi chưa có bằng chứng hiệu năng.

## 19. Chuẩn hóa và bind input value

```java
private static Expression<String> normalizedLiteral(
        CriteriaBuilder criteriaBuilder,
        String value
) {
    HibernateCriteriaBuilder hibernateCriteriaBuilder =
            (HibernateCriteriaBuilder) criteriaBuilder;

    return criteriaBuilder.function(
            "unaccent",
            String.class,
            hibernateCriteriaBuilder.value(value)
    );
}
```

### 19.1. Tại sao cast sang `HibernateCriteriaBuilder`?

Ứng dụng dùng Hibernate làm JPA provider. Object `CriteriaBuilder` thực tế do
Hibernate cung cấp, nên có thể dùng API mở rộng `value()`.

Đây là điểm phụ thuộc Hibernate rõ nhất trong Specification hiện tại.

### 19.2. Tại sao dùng `value(value)`?

Mục tiêu là tạo bind parameter:

```sql
unaccent(?)
```

thay vì inline input thành SQL literal:

```sql
unaccent('bao tang')
```

Bind parameter:

- không nối input của client vào SQL;
- an toàn hơn;
- hỗ trợ tái sử dụng query plan;
- đáp ứng security/query contract FEAT-003.

PostgreSQL integration log đã xác nhận Hibernate sinh `unaccent(?)`.

## 20. Query tổng hợp gần tương đương

Với request:

```http
GET /api/v1/places?keyword=bao%20tang&district=quan%201&category=van-hoa&indoor=true&maxCost=100000
```

Hibernate tạo query có ý nghĩa gần giống:

```sql
SELECT DISTINCT p.*
FROM places p
JOIN place_categories pc ON pc.place_id = p.id
JOIN categories c ON c.id = pc.category_id
WHERE p.active = true
  AND (
       unaccent(lower(p.name)) LIKE unaccent(?) ESCAPE '\'
    OR unaccent(lower(p.short_description)) LIKE unaccent(?) ESCAPE '\'
    OR unaccent(lower(p.full_description)) LIKE unaccent(?) ESCAPE '\'
    OR unaccent(lower(p.address)) LIKE unaccent(?) ESCAPE '\'
    OR unaccent(lower(p.district)) LIKE unaccent(?) ESCAPE '\'
  )
  AND unaccent(lower(p.district)) = unaccent(?)
  AND c.slug = ?
  AND p.indoor = ?
  AND p.min_cost <= ?
ORDER BY p.name ASC, p.id ASC
LIMIT ?
OFFSET ?;
```

Spring Data tạo thêm count query để tính `totalElements` và `totalPages`.
Automated test chấp nhận 1–2 SQL queries/request vì Spring Data có thể bỏ count
query khi suy ra được total từ page content.

## 21. Các cách triển khai khác

### 21.1. Derived query method

Ví dụ:

```java
findAllByActiveTrueAndIndoor(Boolean indoor, Pageable pageable);
```

Phù hợp khi query có ít điều kiện cố định. FEAT-003 có năm optional filters,
tương ứng tối đa `2^5 = 32` tổ hợp có/không, nên không nên tạo hàng chục method.

### 21.2. `@Query`

Có thể viết:

```java
@Query("""
    select p from Place p
    where p.active = true
      and (:indoor is null or p.indoor = :indoor)
""")
```

Nhưng keyword năm cột, category join, distinct và năm optional filters sẽ làm
một JPQL query dài. Mẫu `:parameter IS NULL OR condition` cũng đưa mọi điều
kiện vào query ngay cả khi filter không có mặt.

### 21.3. Criteria API với custom repository

Đây là lựa chọn hợp lệ và gần với kinh nghiệm cũ. Đổi lại, code phải tự quản lý:

- `EntityManager`;
- content query;
- count query;
- pagination;
- sorting;
- distinct;
- custom repository interface và implementation.

### 21.4. Specification

Phù hợp khi:

- có nhiều optional filters;
- cần pagination và sorting;
- muốn dùng Spring Data repository;
- chưa cần query/projection quá đặc biệt;
- muốn tránh custom `RepositoryImpl`.

### 21.5. QueryDSL, jOOQ hoặc native SQL

Các lựa chọn này phù hợp hơn nếu query phát triển thành:

- DTO projection phức tạp;
- aggregate, group by hoặc window function;
- CTE;
- nhiều PostgreSQL-specific features;
- full-text, trigram hoặc vector search;
- calculated relevance score;
- query plan cần được kiểm soát rất chi tiết.

Những nhu cầu đó chưa thuộc phạm vi FEAT-003.

## 22. Specification có cần thiết không?

Không bắt buộc. FEAT-003 hoàn toàn có thể được viết bằng Criteria API thủ công
với `EntityManager`.

Tuy nhiên, Specification hợp lý trong feature hiện tại vì:

- năm filter đều optional;
- keyword dùng OR trên năm cột;
- các filter khác kết hợp AND;
- category cần conditional join và distinct count;
- endpoint cần pagination và fixed sorting;
- Spring Data có thể tự tạo content/count queries;
- dự án đã quyết định không tạo custom repository implementation cho feature này.

Nếu bỏ Specification, phần `List<Predicate>` gần như vẫn giữ nguyên. Phần code
giảm đi sẽ được thay bằng code `EntityManager`, count query và pagination thủ
công.

## 23. Có làm vấn đề phức tạp quá không?

Trong FEAT-003, Specification không phải nguồn chính của sự phức tạp. Sự phức
tạp đến từ các yêu cầu nghiệp vụ:

- tìm kiếm không phân biệt dấu;
- literal wildcard escaping;
- năm optional filters;
- many-to-many category;
- distinct content/count;
- stable pagination;
- bound parameters.

Specification chỉ đóng gói phần Criteria và loại bỏ boilerplate thực thi.
Phần cốt lõi vẫn là mẫu quen thuộc:

```java
List<Predicate> predicates = new ArrayList<>();

if (condition) {
    predicates.add(...);
}

return criteriaBuilder.and(
        predicates.toArray(Predicate[]::new)
);
```

Vì vậy lựa chọn hiện tại là vừa đủ cho quy mô MVP 30–100 places.

## 24. Trade-offs và rủi ro kỹ thuật

### Ưu điểm

- ít boilerplate hơn custom Criteria repository;
- optional filter được thêm thật sự khi cần;
- tái sử dụng pagination và sorting của Spring Data;
- dễ thêm/bớt predicate có phạm vi rõ;
- có thể test trên PostgreSQL thật;
- không tạo repository method cho mọi tổ hợp filter.

### Hạn chế

- `root.get("fieldName")` dùng String nên đổi tên Entity field có thể chỉ lỗi ở runtime;
- `HibernateCriteriaBuilder.value()` phụ thuộc Hibernate;
- Specification phức tạp có thể khó đọc nếu tách thành quá nhiều class nhỏ;
- join/fetch/pagination phức tạp vẫn cần kiểm tra SQL thực tế;
- không thay thế query-plan profiling và integration test.

Có thể dùng JPA static metamodel để tăng type safety, nhưng việc bổ sung metamodel
chỉ cho vài field ở quy mô MVP có thể tạo thêm cấu hình không cần thiết.

## 25. Khi nào nên đổi hướng?

Nên cân nhắc custom query technology nếu Place Search sau này cần:

- full-text hoặc trigram search;
- vector similarity/RAG retrieval;
- relevance ranking;
- projection nhiều bảng phức tạp;
- aggregate/grouping;
- CTE hoặc window function;
- nhiều fetch join cùng pagination;
- kiểm soát SQL/query plan chính xác hơn Criteria abstraction.

Việc đổi hướng phải được đánh giá trong feature tương ứng, không mở rộng
FEAT-003 hiện tại.

## 26. Cách đọc và debug một Specification

Khi Specification cho kết quả sai, kiểm tra theo thứ tự:

1. Request đã được normalize/bind đúng chưa.
2. Predicate bắt buộc như `active=true` có luôn được thêm không.
3. Optional predicate có bị thêm khi value là `null` không.
4. AND/OR có được nhóm đúng không.
5. Join có tạo duplicate rows không.
6. `distinct` có làm count query đúng không.
7. Input có được bind thành `?` không.
8. SQL Hibernate sinh ra có đúng với ý định không.
9. Chạy integration test trên PostgreSQL thật.
10. Dùng `EXPLAIN (ANALYZE, BUFFERS)` nếu có vấn đề hiệu năng.

Các test quan trọng của FEAT-003 nằm trong:

```text
backend/src/test/java/com/saigonplantravel/backend/place/repository/PlaceRepositoryTest.java
```

Chúng kiểm tra:

- keyword trên năm cột;
- không phân biệt dấu;
- literal `%`, `_`, `\`;
- district exact;
- category, indoor và cost;
- full AND combination;
- inactive exclusion;
- stable pagination;
- distinct total;
- tối đa hai SQL queries.

## 27. Kết luận ghi nhớ

Specification trong dự án này có thể được hiểu ngắn gọn là:

```text
Một factory tạo Predicate bằng Criteria API,
được Spring Data dùng để tự xây và thực thi query có pagination.
```

Nó không phải một query language mới và không phủ nhận cách dùng
`CriteriaBuilder + List<Predicate>` đã biết. Nó chỉ đặt phần logic quen thuộc đó
vào abstraction phù hợp với `JpaRepository` và `Pageable`.
