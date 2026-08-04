# Place API v1

## Public endpoints

```http
GET /api/v1/places
GET /api/v1/places/{slug}
GET /api/v1/categories
```

Các endpoint là public trong source hiện tại. Response dùng DTO, không trả JPA
Entity.

## Administrative-unit contract

Place summary và detail cùng trả:

```json
{
  "administrativeUnitName": null,
  "administrativeUnitType": null
}
```

Khi đã được mapping, type chỉ nhận một trong:

```text
WARD
COMMUNE
SPECIAL_ZONE
```

Hai field cùng `null` hoặc cùng có giá trị. `null` nghĩa là chưa được mapping
thủ công; client không được suy diễn giá trị.

## Place catalog

```http
GET /api/v1/places?keyword=&administrativeUnitName=&category=&indoor=&maxCost=&page=&size=
```

| Parameter | Validation | Semantics |
| --- | --- | --- |
| `keyword` | Tối đa 100 ký tự | Literal substring normalized |
| `administrativeUnitName` | Tối đa 100 ký tự | Exact normalized name |
| `category` | Lowercase slug, tối đa 120 | Exact category slug |
| `indoor` | `true` hoặc `false` | Exact boolean |
| `maxCost` | `0..100000000` | `place.minCost <= maxCost` |
| `page` | Integer `>= 0`, default `0` | Zero-based page |
| `size` | Integer `1..100`, default `20` | Page size |

Không có filter theo `administrativeUnitType` trong contract hiện tại.

Keyword tìm trên năm field:

1. `name`;
2. `shortDescription`;
3. `fullDescription`;
4. `address`;
5. `administrativeUnitName`.

Keyword và administrative-unit name được NFC-normalize, trim, collapse
whitespace, lowercase và xử lý không dấu bằng PostgreSQL `unaccent`. `%`, `_`
và `\` trong keyword là ký tự literal. Các filter kết hợp bằng AND.

### Response

```json
{
  "content": [
    {
      "id": 1,
      "name": "Demo Art Space",
      "slug": "demo-art-space",
      "shortDescription": "Dữ liệu minh họa, chưa được xác minh",
      "administrativeUnitName": null,
      "administrativeUnitType": null,
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
  "totalElements": 5,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

Envelope có 7 field; mỗi summary item có 12 field. Chỉ active places được trả
và sorting cố định là `name ASC, id ASC`.

## Place detail

```http
GET /api/v1/places/{slug}
```

Detail có 16 field: 14 scalar fields cùng `categories` và `openingHours`.

```json
{
  "id": 1,
  "name": "Demo Art Space",
  "slug": "demo-art-space",
  "shortDescription": "Dữ liệu minh họa, chưa được xác minh",
  "fullDescription": "Địa điểm mô phỏng chỉ phục vụ phát triển và kiểm thử.",
  "address": "Địa chỉ demo 1",
  "administrativeUnitName": null,
  "administrativeUnitType": null,
  "latitude": 10.7750000,
  "longitude": 106.7000000,
  "estimatedVisitMinutes": 90,
  "minCost": 50000.00,
  "maxCost": 150000.00,
  "indoor": true,
  "categories": [],
  "openingHours": []
}
```

Slug lookup exact và case-sensitive. Missing, malformed hoặc inactive place
trả cùng `404 PLACE_NOT_FOUND`.

## Category catalog

```http
GET /api/v1/categories
```

Response là root array gồm `id`, `name`, `slug`, sort theo `name ASC, id ASC`.

## Errors

Invalid query parameters trả `400 application/problem+json`,
`code=INVALID_REQUEST` và `fieldErrors` khi phù hợp. Lỗi ngoài dự kiến trả 500
an toàn, không lộ SQL, credential, connection string hoặc stack trace.

## Frontend behavior

- Filter options được suy ra từ active catalog `page=0&size=100`.
- Place có `administrativeUnitName=null` không tạo filter option.
- Card, map overlay và detail hiển thị “Chưa xác định” khi name là null.
- Frontend không tự gán administrative-unit name hoặc type.
