---
title: Next.js Place Explorer Frontend Guide
feature: FEAT-001, FEAT-002, FEAT-003
created: 2026-07-21
updated: 2026-07-22
tags:
  - nextjs
  - react
  - typescript
  - leaflet
  - learning
---

# Next.js Place Explorer Frontend Guide

## 1. Động lực và execution flow

Vertical slice này biến Place API thành hai luồng hoàn chỉnh:

```text
URL /places
  → Server Component đọc searchParams
  → API client gọi Spring Boot
  → Client Component nhận DTO qua props
  → danh sách + Leaflet tương tác

URL /places/[slug]
  → Server Component đọc params.slug
  → API client gọi detail endpoint
  → trang chi tiết + bản đồ một marker
```

Backend chưa cấu hình CORS. Vì vậy `place-api.ts` gọi Spring Boot từ Next.js
server, không gọi trực tiếp từ browser. Cách này không yêu cầu sửa backend,
không lộ base URL trong public JavaScript và vẫn cho phép `next build` chạy khi
backend tắt. Đổi lại, Next.js server phải truy cập được Spring Boot khi xử lý
request thật.

Source chính:

- `frontend/src/app/places/page.tsx`
- `frontend/src/app/places/[slug]/page.tsx`
- `frontend/src/lib/api/place-api.ts`
- `frontend/src/features/places/components/places-explorer.tsx`
- `frontend/src/features/map/map-shell.tsx`

## 2. `page.tsx` và `layout.tsx`

Trong App Router, thư mục dưới `src/app` biểu diễn URL:

```text
src/app/layout.tsx                → khung dùng chung cho mọi trang
src/app/page.tsx                  → /
src/app/places/page.tsx           → /places
src/app/places/[slug]/page.tsx    → /places/{slug}
```

`layout.tsx` không phải trang độc lập. Nó bọc các `page.tsx` qua prop
`children`. Dự án dùng layout để khai báo `lang="vi"`, metadata, CSS toàn cục
và `SiteHeader`.

`page.tsx` là leaf route và làm URL truy cập được. `/places/page.tsx` đọc
filter, lấy dữ liệu rồi truyền xuống giao diện. `[slug]/page.tsx` dùng dynamic
segment: tên thư mục trong ngoặc vuông trở thành `params.slug`.

Nên dùng layout cho khung chung nhiều route và page cho nội dung riêng một URL.
Không nên fetch theo query string trong root layout vì layout không nhận
`searchParams` của page và không nên render lại cho mọi lần lọc.

## 3. Server Component và Client Component

Component App Router mặc định là Server Component. Nó chạy trên server, được
đọc private environment variable và gọi backend trực tiếp.

Trong source này:

- `app/places/page.tsx` và `[slug]/page.tsx` là Server Components;
- `places-explorer.tsx` là Client Component vì dùng state và event;
- `map-shell.tsx`/`place-map.tsx` là Client Components vì Leaflet cần DOM.

Client Component bắt đầu bằng `"use client"`. Chỉ dùng nó khi cần `useState`,
`useEffect`, event hoặc browser API. Trade-off là Client Component làm tăng
JavaScript gửi xuống browser. Vì vậy API client và phần detail tĩnh vẫn ở server.

## 4. Props và TypeScript type

Props là dữ liệu component cha truyền cho component con:

```tsx
type PlaceCardProps = {
  place: PlaceSummary;
  selected: boolean;
  onSelect: (slug: string) => void;
};
```

`PlaceSummary` khớp 11 field DTO backend; `PlacePage` khớp envelope 7 field;
`PlaceDetail` bổ sung `fullDescription`, `address`, `categories` và
`openingHours`. Type nằm ở `frontend/src/types/place.ts`.

Nhờ vậy TypeScript báo khi gõ sai field, quên xử lý `null`, truyền callback sai
hoặc dùng field detail trong card summary. Không cần `any`.

Type chỉ kiểm tra lúc build, không tự validate JSON runtime. Nếu API thay đổi
ngoài contract, phải cập nhật DTO, type và contract tests cùng lúc.

## 5. URL search parameters

Ví dụ:

```text
/places?keyword=bao+tang&category=van-hoa&page=0
```

`search-params.ts` chuyển object Next.js thành `PlacesSearchFilters`. API client
chỉ append giá trị có nội dung. Form dùng GET; category và pagination dùng
`Link`. Vì state nằm trong URL:

- reload không mất filter;
- URL có thể bookmark/chia sẻ;
- Back/Forward hoạt động;
- Server Component có đủ input để fetch đúng kết quả.

Khi filter đổi, link và form không giữ `page`, nên backend nhận page mặc định 0.
Pagination hiển thị `page + 1` cho người dùng nhưng gửi page zero-based.

State cục bộ thuần `useState` ngắn hơn lúc đầu, nhưng reload sẽ mất filter và
khó đồng bộ lịch sử browser. Redux không cần thiết cho state đã có biểu diễn tự
nhiên trong URL.

## 6. Tránh lỗi SSR với Leaflet

Leaflet truy cập `window`/DOM khi module chạy. Server không có các API này.
`map-shell.tsx` là Client Component và dùng:

```tsx
const PlaceMap = dynamic(() => import("./place-map"), {
  ssr: false,
  loading: () => <MapLoading />,
});
```

Các điểm quan trọng:

1. `ssr: false` nằm trong Client Component, đúng quy tắc Next.js 16.
2. Module chứa `leaflet` chỉ tải ở browser.
3. CSS Leaflet import một lần trong root layout.
4. Marker dùng `divIcon`, tránh lỗi đường dẫn ảnh marker mặc định khi bundle.
5. OpenStreetMap attribution được giữ trong `TileLayer`.

Cách này phù hợp với thư viện phụ thuộc DOM. Với map tĩnh chỉ để minh họa, ảnh
hoặc server-rendered SVG nhẹ hơn, nhưng không đáp ứng zoom/định vị/marker select.

## 7. Failure flow, alternatives và giới hạn

- `content: []`: empty state “Không tìm thấy địa điểm”.
- Network/HTTP 5xx: connection error và retry, không chuyển sang mock.
- HTTP 400: filter request không hợp lệ.
- Detail HTTP 404: `notFound()` và UI 404 riêng.
- Quận suy ra từ catalog active `size=100`, phù hợp MVP 30–100 places; backend
  chưa có district metadata API.
- Summary không có category, nên card không bịa category.
- Không có `imageUrl`, nên detail dùng gradient/category illustration.

Không nên dùng catalog `size=100` khi quy mô vượt 100 hoặc cần quận không có
active place. Khi đó nên thiết kế district metadata endpoint riêng và cập nhật
contract có phê duyệt.

## 8. Tự kiểm tra

```bash
cd frontend
npm run lint
npm run build
```

Sau khi backend chạy, thử `/places`, một URL có filter, một keyword không có kết
quả và `/places/demo-art-space`.

## 9. shadcn/ui khác thư viện UI đóng gói như thế nào?

Thư viện UI đóng gói thông thường cung cấp component từ `node_modules`. Ứng
dụng import component nhưng phần lớn implementation vẫn thuộc package và được
nâng cấp theo version package đó.

shadcn/ui dùng registry và CLI để **chép source component vào repository**. Sau
`npx shadcn@latest add button`, source thật nằm tại
`frontend/src/components/ui/button.tsx`. Dự án có thể đọc, sửa variant, màu,
size và accessibility behavior của Button như code nội bộ. Dependency nền như
Radix vẫn nằm trong `node_modules`, nhưng lớp component của ứng dụng không phải
một “hộp đen”.

Điểm mạnh là dễ học, dễ tùy biến Mint Map và không phải chống lại theme đóng.
Trade-off là repository sở hữu code đã chép: khi registry có bản mới, developer
phải review diff và merge có chủ đích thay vì chỉ tăng một package version.

## 10. Vai trò của `components/ui`

`frontend/src/components/ui/` chỉ chứa primitive tái sử dụng như `Button`,
`Input`, `Select`, `Sheet`, `Alert`, `Skeleton`, `Avatar`, `Card`, `Badge` và
`Pagination`. Primitive biết cách render, style, focus và expose props; nó không
biết Place API, `slug`, `district` hay quy tắc page zero-based.

Ngược lại:

| Business component | Trách nhiệm riêng |
| --- | --- |
| `PlacesExplorer` | list/map view và place selection |
| `FilterPanel` | ánh xạ filter domain vào GET form |
| `PlaceCard` | trình bày `PlaceSummary` và đồng bộ marker |
| `PlaceMap`/`MapShell` | Leaflet, marker, viewport và SSR boundary |
| `Pagination` feature | đổi page nhưng giữ URL filter |

Không đưa `getPlaces()` vào `components/ui/card.tsx`, vì Card phải tái sử dụng
được mà không phụ thuộc SaigonPlanTravel.

## 11. `cn()` dùng để làm gì?

`frontend/src/lib/utils.ts` kết hợp hai việc:

```ts
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

- `clsx` ghép class tĩnh và class có điều kiện;
- `tailwind-merge` giải quyết utility Tailwind xung đột, ví dụ `h-8` và `h-11`
  để class override sau cùng thắng có chủ đích.

Ví dụ tư duy:

```tsx
className={cn("border-border", selected && "border-accent", className)}
```

Nên dùng `cn()` trong primitive hoặc khi có class điều kiện. Không cần dùng nó
cho một chuỗi class cố định đơn giản.

## 12. Variant của Button hoạt động thế nào?

`button.tsx` dùng `class-variance-authority` (`cva`) để định nghĩa hai trục:

- `variant`: `default`, `accent`, `outline`, `secondary`, `ghost`, ...;
- `size`: `default`, `sm`, `lg`, `icon`, ...

`<Button variant="accent" size="lg">` ghép base class, class của `accent` và
class của `lg`. Variant `accent` dùng semantic token `--accent` và
`--shadow-accent`, nên nút Tạo lịch trình vẫn là Mint Map thay vì màu mặc định
của shadcn. `asChild` dùng Radix Slot để truyền style/behavior xuống `Link`:

```tsx
<Button asChild variant="outline">
  <Link href="/places">Xóa bộ lọc</Link>
</Button>
```

Cách này giữ đúng semantic link và client navigation của Next.js, không tạo
HTML sai kiểu `<button><a>...</a></button>`.

## 13. Vì sao Select và Sheet là Client Components?

Native `<select>` và `<dialog>` đơn giản có thể server-render. Shadcn Select và
Sheet trong refactor này bọc Radix primitives để quản lý:

- open/closed state;
- keyboard navigation;
- focus trap và trả focus về trigger;
- Escape, click outside và portal;
- ARIA attributes theo trạng thái.

Những hành vi này cần event handler và DOM browser, nên file primitive có
`"use client"`. Điều đó không biến `app/places/page.tsx` thành Client Component:

```text
Server page fetch API
  → PlacesExplorer (client vì đã có list/map state)
      → FilterPanel → Select
      → Sheet mobile
```

Detail page vẫn là Server Component vì nó chỉ dùng Button/Card/Badge không cần
state. Chỉ `MapShell` tiếp tục là client boundary do Leaflet cần DOM.

## 14. URL state và draft state của Select

URL vẫn là nguồn sự thật của filter đã áp dụng. Radix Select không dùng empty
string như một selectable item, nên `FilterPanel` giữ một draft value nhỏ cho
mỗi Select và ghi giá trị tương ứng vào hidden input của GET form. Khi nhấn “Áp
dụng”, form điều hướng tới `/places`; page server đọc URL và fetch lại dữ liệu.

Đây không phải state nghiệp vụ thứ hai: draft chỉ tồn tại trước submit. Key của
`FilterPanel` thay đổi khi URL filter thay đổi để form khởi tạo lại từ source of
truth. Redux hoặc React Hook Form không mang thêm giá trị cho form nhỏ này.

## 15. Khi nên và không nên dùng cách này

Nên dùng shadcn primitive khi cần style nhất quán, variant lặp lại hoặc hành vi
khó tự làm đúng như focus management của Sheet. Nên giữ native HTML/Next Link
khi semantics đã rõ và primitive chỉ làm code phức tạp hơn; category chip trong
dự án vẫn là Link vì click là điều hướng URL.

Không nên chuyển layout danh sách + bản đồ sang shadcn Sidebar: đó là layout
nghiệp vụ gắn với Leaflet và mobile view switcher. Cũng không nên đưa API call,
DTO hoặc marker state vào primitive chỉ để giảm số file.

## 16. Ba bài tập tự luyện

1. Đổi `--radius` từ `0.75rem` thành `0.625rem`, chạy app và ghi lại Button,
   Input, Select nào thay đổi; không sửa business component.
2. Thêm variant `soft` vào `button.tsx` bằng `--primary-soft` và dùng nó cho nút
   chọn marker; giữ `aria-pressed` và vùng nhấn 44 px.
3. Thêm một `Badge variant="outline"` cạnh số kết quả để hiển thị “Có bộ lọc”
   khi `hasActiveFilters(filters)` là true; không tạo state mới và không đổi URL.

## 17. Function declaration và arrow function trong React

React và Next.js chấp nhận cả hai cách khai báo component sau; dự án không có
quy tắc ESLint bắt buộc phải dùng một kiểu:

```tsx
export function PlaceCard(props: PlaceCardProps) {
  return <article>...</article>;
}

export const PlaceCard = (props: PlaceCardProps) => {
  return <article>...</article>;
};
```

Source hiện tại dùng một quy ước kết hợp:

- function declaration cho component và helper cấp module có tên rõ ràng;
- arrow function cho callback ngắn trong `map`, `find`, `useEffect`, dynamic
  import và event handler.

Ví dụ `frontend/src/features/places/components/place-card.tsx` dùng cả hai đúng
vai trò:

```tsx
export function PlaceCard({ place, onSelect }: PlaceCardProps) {
  return (
    <Button onClick={() => onSelect(place.slug)}>
      Xem trên bản đồ
    </Button>
  );
}
```

`PlaceCard` là đơn vị cấp module nên function declaration giúp tên component
nổi bật, có thể được gọi trước vị trí khai báo nhờ hoisting và khớp cách viết
phổ biến của page/layout trong Next.js. Callback của `onClick` dùng arrow vì nó
ngắn và đóng trên giá trị `place.slug` của lần render hiện tại.

Luồng thực thi là:

```text
Module được nạp
  → Next.js/React gọi PlaceCard với props
  → PlaceCard trả JSX và tạo callback cho nút
  → người dùng click
  → callback đọc place.slug rồi gọi onSelect
```

Khác biệt ngôn ngữ chính:

| Tiêu chí | `function Name()` | `const Name = () =>` |
| --- | --- | --- |
| Hoisting | Có thể gọi trước dòng khai báo | Không dùng được trước khi `const` khởi tạo |
| `this`, `arguments` | Có binding riêng | Kế thừa lexical scope |
| Tên khi debug | Tên được khai báo trực tiếp | Thường được suy ra từ tên biến |
| Hợp với source này | Component/helper cấp module | Callback và handler ngắn |

Function Component không dùng `this`, nên khác biệt về `this` hầu như không ảnh
hưởng tới dự án. Hai cách đều dùng được với props TypeScript, Hooks, Server
Components và Client Components; cũng không có chênh lệch hiệu năng đáng kể chỉ
vì chọn cú pháp nào.

Nên giữ function declaration cho component/helper mới để nhất quán với source,
và dùng arrow cho callback. Không nên refactor hàng loạt chỉ để đổi cú pháp vì
diff lớn nhưng không cải thiện hành vi. Nếu đội dự án muốn chuyển sang arrow-only
thì nên thống nhất convention, thêm lint rule và thực hiện trong một commit cơ
học riêng, không trộn với thay đổi nghiệp vụ.

## 18. Giải phẫu toàn bộ `place-api.ts`

File `frontend/src/lib/api/place-api.ts` là lớp adapter giữa Next.js frontend và
REST contract của Spring Boot. Nó không render UI và không giữ React state.

### 18.1. Type-only imports

```ts
import type {
  ApiProblem,
  Category,
  PlaceDetail,
  PlacePage,
  PlacesSearchFilters,
} from "@/types/place";
```

`import type` chỉ phục vụ TypeScript và bị loại khỏi JavaScript build. Các type
đảm bảo mỗi hàm công khai trả đúng hình dạng frontend mong đợi, nhưng không tự
kiểm tra JSON ở runtime.

### 18.2. Base URL

```ts
const DEFAULT_BACKEND_URL = "http://localhost:8080";

function getBackendBaseUrl() {
  return (process.env.BACKEND_API_BASE_URL ?? DEFAULT_BACKEND_URL).replace(
    /\/$/,
    "",
  );
}
```

Server ưu tiên `BACKEND_API_BASE_URL`; nếu chưa khai báo thì dùng Spring Boot
cục bộ. `replace(/\/$/, "")` bỏ dấu `/` cuối để ghép với path bắt đầu bằng `/`
mà không tạo `//api/...`.

Biến này không có prefix `NEXT_PUBLIC_`, nên mục đích là dùng phía server.
Import graph hiện tại chỉ gọi API client từ Server Components. Nếu sau này cần
bảo vệ ranh giới chặt hơn, có thể cân nhắc `import "server-only"`; đó là hardening,
không phải yêu cầu của contract hiện tại.

### 18.3. `PlaceApiError`

```ts
export class PlaceApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly problem: ApiProblem | null,
  ) {
    super(problem?.detail ?? "Không thể kết nối đến Place API.");
    this.name = "PlaceApiError";
  }
}
```

Class lỗi gom hai thông tin mà UI cần:

- `status`: HTTP status; dự án quy ước `0` cho lỗi chưa nhận được response;
- `problem`: Spring `ProblemDetail`, hoặc `null` nếu body không đọc được.

Vì nó kế thừa `Error`, page có thể dùng `error instanceof PlaceApiError` để
phân biệt lỗi API dự kiến với lỗi lập trình/render không dự kiến.

### 18.4. Hàm lõi `requestJson<T>()`

```ts
async function requestJson<T>(path: string): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${getBackendBaseUrl()}${path}`, {
      cache: "no-store",
      headers: { Accept: "application/json" },
    });
  } catch {
    throw new PlaceApiError(0, null);
  }

  if (!response.ok) {
    let problem: ApiProblem | null = null;
    try {
      problem = (await response.json()) as ApiProblem;
    } catch {}
    throw new PlaceApiError(response.status, problem);
  }

  return (await response.json()) as T;
}
```

Luồng xử lý:

1. Ghép base URL và endpoint.
2. Gửi `Accept: application/json` để nói client mong nhận JSON.
3. `cache: "no-store"` buộc lấy dữ liệu mới cho mỗi request, không dùng Next
   Data Cache.
4. Nếu `fetch` không tạo được response, ví dụ backend tắt hoặc DNS lỗi, ném
   `PlaceApiError(0, null)`.
5. Nếu có response nhưng status không thuộc `200..299`, cố đọc body
   `ProblemDetail`, sau đó ném `PlaceApiError` với HTTP status thật.
6. Nếu thành công, parse JSON và trả về dưới type generic `T`.

`response.ok` không chỉ kiểm tra `200`; mọi status `200..299` đều được coi là
thành công. Cast `as T` không validate runtime. Nếu backend trả JSON sai contract,
HTML với status 200 hoặc body rỗng, lỗi parse/shape có thể đi lên error boundary.
Ở quy mô hiện tại, frontend tin vào API contract và backend tests. Khi nhận dữ
liệu ngoài hệ thống hoặc contract biến động mạnh, schema validator như Zod là
một lựa chọn, nhưng sẽ thêm dependency và chi phí parse.

Hàm hiện chưa đặt timeout/`AbortSignal`. Điều đó đơn giản cho MVP, nhưng request
có thể chờ theo timeout của runtime/hạ tầng nếu backend bị treo.

#### Có nên tách `requestJson()` thành API client dùng chung?

Chưa cần tách chỉ vì hàm dùng generic `T`. Hiện repository mới có một consumer
là Place API, trong khi implementation vẫn phụ thuộc trực tiếp vào:

- `BACKEND_API_BASE_URL` và default URL của Spring Boot;
- `PlaceApiError`;
- hình dạng Spring `ProblemDetail`;
- chính sách `cache: "no-store"` áp dụng cho mọi request;
- giả định mọi response thành công đều có JSON body.

Vì vậy nó generic về **kiểu response**, nhưng chưa phải transport abstraction
hoàn toàn tổng quát. Tách sớm sẽ tạo thêm file và tên abstraction trước khi biết
Trip, Itinerary hoặc AI API có cùng cache/error/auth behavior hay không.

Nên tách khi xuất hiện consumer thứ hai có cùng quy trình HTTP, ví dụ
`trip-api.ts` cũng lặp lại base URL, `fetch`, parse ProblemDetail và error class.
Khi đó cấu trúc hợp lý là:

```text
src/lib/api/api-client.ts   → requestJson(), base URL, ApiError
src/lib/api/place-api.ts    → endpoint và query riêng của Place
src/lib/api/trip-api.ts     → endpoint và payload riêng của Trip
src/types/api.ts            → ApiProblem dùng chung
```

Phần dùng chung chỉ nên sở hữu transport concern. `appendIfPresent()` và cách
serialize `PlacesSearchFilters` vẫn có thể ở `place-api.ts` vì đó là contract
của Place. Nếu API mới cần JWT, POST body, `204 No Content`, file/blob, timeout
khác hoặc cache khác, nên mở rộng bằng request options có chủ đích thay vì ép
mọi endpoint qua một helper quá tổng quát.

### 18.5. `appendIfPresent()`

```ts
function appendIfPresent(params, key, value) {
  if (value === undefined || String(value).trim() === "") return;
  params.set(key, String(value));
}
```

Helper bỏ `undefined`, empty string và chuỗi chỉ có khoảng trắng. Nó vẫn giữ số
`0`, vì `String(0)` là `"0"`. `URLSearchParams` chịu trách nhiệm encode khoảng
trắng, dấu tiếng Việt và ký tự đặc biệt đúng chuẩn query string.

### 18.6. Bốn hàm API công khai

`getPlaces(filters)` tạo query từ năm filter cùng `page`, `size`, rồi gọi:

```http
GET /api/v1/places?keyword=...&district=...&category=...&indoor=...&maxCost=...&page=...&size=...
```

Kết quả là `PlacePage`, gồm `content` và metadata phân trang.

`getPlaceCatalog()` gọi cố định `page=0&size=100`. Nó không dùng để render toàn
bộ kết quả hiện tại mà để suy ra danh sách quận từ tối đa 100 active places.
Đây là giải pháp MVP; khi dữ liệu vượt quá 100 hoặc cần cả quận chưa có active
place, backend nên có metadata endpoint riêng.

`getCategories()` gọi `/api/v1/categories` và nhận root array `Category[]`.

`getPlaceDetail(slug)` gọi detail endpoint. `encodeURIComponent(slug)` giữ slug
trong đúng một URL path segment và encode ký tự đặc biệt. Nó không lowercase,
trim hay sửa slug vì backend định nghĩa lookup exact/case-sensitive.

## 19. Bản đồ trách nhiệm của frontend

| Lớp | File chính | Trách nhiệm |
| --- | --- | --- |
| Route shell | `app/layout.tsx`, `app/page.tsx` | Header, global CSS, metadata và redirect `/` |
| Server orchestration | `app/places/page.tsx`, `[slug]/page.tsx` | Đọc URL, fetch dữ liệu, chọn nhánh success/error/404 |
| API adapter | `lib/api/place-api.ts` | HTTP, query serialization và chuẩn hóa lỗi |
| Contract | `types/place.ts` | Type của summary, page, detail, filter và ProblemDetail |
| URL helpers | `features/places/search-params.ts` | Parse query, tạo link mới, nhận biết filter active |
| Business UI | `features/places/components/*` | Search, filter, list, card, selection, pagination, detail |
| Map boundary | `features/map/map-shell.tsx`, `place-map.tsx` | Tách Leaflet khỏi SSR, marker và viewport |
| UI primitives | `components/ui/*` | Style, variant, focus, portal và accessibility cơ bản |
| Presentation | `globals.css`, `formatters.ts` | Responsive Mint Map và format dữ liệu hiển thị |

Quy tắc phụ thuộc đi từ business component xuống primitive. Primitive không
import `place-api.ts`, `PlaceSummary` hay logic filter.

## 20. Luồng đầy đủ của `/places`

### 20.1. Mở trang và tải dữ liệu

```text
Browser yêu cầu /places?category=van-hoa&page=0
  → Next.js chạy app/places/page.tsx trên server
  → await searchParams
  → readPlacesSearchParams()
  → Promise.all(getPlaces, getCategories, getPlaceCatalog)
  → Spring Boot trả JSON
  → Next.js render PlacesExplorer với props
  → browser hydrate Client Components
```

Ba API call chạy song song bằng `Promise.all`, nên tổng thời gian gần với request
chậm nhất thay vì tổng của ba request. Đổi lại, chỉ cần một request thất bại thì
cả nhóm reject và trang hiện error state; source không hiển thị list khi thiếu
category hoặc catalog.

Catalog được biến thành danh sách quận bằng `Set` để bỏ trùng và
`localeCompare(..., "vi")` để sắp xếp theo locale tiếng Việt.

### 20.2. Parse URL

`readPlacesSearchParams()` xử lý query trước khi gọi backend:

- query lặp như `?page=1&page=2`: lấy phần tử đầu;
- text: trim, blank thành `undefined`;
- `page`/`size`: chỉ chuỗi toàn chữ số mới được nhận, ngược lại dùng mặc định
  `page=0`, `size=10`;
- `indoor`: chỉ nhận đúng `"true"` hoặc `"false"`;
- các giới hạn sâu hơn như max cost, category slug và size tối đa vẫn do backend
  validate.

Do đó `page=-1` hoặc `page=abc` được frontend đưa về `0`, còn `size=999` vẫn tới
backend và có thể nhận HTTP 400.

### 20.3. Search, filter, category và pagination

URL là state đã áp dụng:

- `SearchForm` dùng GET form, giữ các filter khác bằng hidden inputs và không gửi
  page, nên search mới trở về page 0;
- `FilterPanel` cũng dùng GET form, giữ keyword và bỏ page;
- category chip dùng `Link`, toggle category hiện tại và đặt `page: 0`;
- pagination dùng `Link`, chỉ đổi page và giữ filter.

`createPlacesHref()` merge filter cũ với thay đổi mới, bỏ giá trị rỗng và không
đưa `size` lên URL. Khi route mới được đọc lại, frontend tiếp tục áp dụng default
`size=10`.

Radix Select không phải native form control và empty string không được dùng làm
item value. `FilterPanel` vì vậy:

1. dùng sentinel `__all__` cho lựa chọn “Tất cả”;
2. giữ district/category/indoor đang chỉnh trong local state;
3. ghi state đó vào hidden native inputs;
4. chỉ thay URL khi người dùng submit.

`useId()` nối label với trigger mà không trùng ID giữa bộ lọc desktop và mobile.
`filterPanelKey` làm component remount khi applied filter đổi, nhờ đó draft Select
và `defaultValue` của max cost được đồng bộ lại với URL.

### 20.4. State tương tác list–map

`PlacesExplorer` chỉ giữ hai UI state không phù hợp để lưu trong URL:

- `selectedSlug`: địa điểm đang highlight;
- `mobileView`: đang xem `list` hay `map`.

Mặc định chọn item đầu tiên. Sau navigation/filter, React có thể giữ state cũ,
nên `effectiveSelectedSlug` kiểm tra slug cũ còn nằm trong page mới hay không;
nếu không, nó fallback về item đầu tiên. Cách này tránh marker/card selection bị
trỏ vào địa điểm không còn trong kết quả.

Khi bấm nút pin trên card:

```text
PlaceCard onClick
  → PlacesExplorer.selectPlace(slug)
  → setSelectedSlug(slug)
  → mobile tự chuyển sang map
  → PlaceMap flyTo marker
  → PlaceList scroll card tương ứng vào vùng nhìn thấy
```

Khi bấm marker, `PlaceMap` gọi callback optional với slug. Ở catalog callback là
`setSelectedSlug`; ở detail không truyền callback vì chỉ có một marker tĩnh.

`PlaceList` render empty state khi `content.length === 0`; ngược lại map mỗi DTO
thành `PlaceCard`. `key={place.id}` giúp React nhận diện ổn định từng card.

### 20.5. Pagination

Backend dùng zero-based page, UI dùng one-based label:

```text
backend page=0 → giao diện “Trang 1”
backend page=1 → giao diện “Trang 2”
```

Nút trước/sau dựa trên `first` và `last` từ response, không tự suy đoán. Nếu chỉ
có tối đa một trang, pagination không render.

## 21. Luồng Leaflet và lý do có `MapShell`

Leaflet cần `window` và DOM. `MapShell` là Client Component dùng dynamic import
với `ssr: false`, nên module `place-map.tsx` chỉ được tải trong browser; skeleton
được hiển thị trong lúc chunk bản đồ tải.

Trong `PlaceMap`:

1. `MapContainer` chọn item đầu làm tâm, hoặc dùng tâm Sài Gòn khi list rỗng.
2. `TileLayer` tải tile từ OpenStreetMap và giữ attribution.
3. Effect thứ nhất của `MapViewport` fit toàn bộ markers; một marker dùng zoom 15.
4. Effect thứ hai tìm selected slug và `flyTo` marker đó.
5. Mỗi place tạo một `Marker`; `divIcon` hiển thị số thứ tự khớp card.
6. `useMemo` tìm DTO được chọn để render preview bên dưới map.
7. `LocationControl` gọi browser geolocation qua `map.locate()`; khi tìm thấy,
   map bay tới vị trí và thêm circle marker.

Current limitations: chưa có UI cho `locationerror`, chưa dọn marker vị trí cũ
khi bấm định vị nhiều lần, và tile map phụ thuộc mạng tới OpenStreetMap. Đây là
giới hạn của implementation hiện tại, không phải dữ liệu fallback.

## 22. Luồng `/places/[slug]`

```text
Link “Xem chi tiết”
  → /places/{slug}
  → Server Component await params
  → getPlaceDetail(encodeURIComponent(slug))
  → 200: PlaceDetailView
  → 404 PlaceApiError: null → notFound() → not-found.tsx
  → lỗi khác: throw → error boundary gần nhất
```

`PlaceDetail` kế thừa `PlaceSummary`, nên cùng object có thể truyền cho
`MapShell` mà không chuyển đổi.

`PlaceDetailView` tạo `Map<dayOfWeek, OpeningHour>` để lookup theo ngày, rồi luôn
lặp đủ Thứ Hai–Chủ Nhật:

- không có record: “Chưa có dữ liệu”;
- `closed=true`: “Đóng cửa”;
- còn lại: `openTime – closeTime`.

Sự phân biệt này quan trọng: thiếu dữ liệu không được suy diễn thành đóng cửa.
Các formatter chuyển phút sang giờ/phút và chi phí sang tiền Việt; dữ liệu gốc
vẫn giữ nguyên number trong DTO.

## 23. Loading, error và responsive behavior

- `loading.tsx` là fallback route tự động trong lúc Server Component chờ dữ liệu.
- `PlacesErrorState` xử lý `PlaceApiError` có chủ đích: status 0/5xx là lỗi kết
  nối, status khác được trình bày như request/filter không hợp lệ.
- `error.tsx` là Client Component cho lỗi không được page bắt; `reset()` yêu cầu
  Next.js thử render lại segment và effect ghi lỗi ra console.
- `not-found.tsx` dành riêng cho detail 404.
- Không có dữ liệu giả khi backend lỗi.

CSS giữ cùng component tree nhưng đổi presentation:

- desktop: sidebar danh sách và map nằm cạnh nhau;
- mobile dưới 768 px: chỉ list hoặc map hiện tại được hiển thị, switcher cố định
  ở đáy; filter mở trong Sheet từ dưới lên;
- `prefers-reduced-motion` giảm animation cho người dùng cần hạn chế chuyển động.

Sheet và Select dựa trên Radix để xử lý focus, keyboard, portal và ARIA. Button,
Card, Badge, Alert... là source shadcn nằm trong repository; chúng chỉ cung cấp
primitive, còn nghiệp vụ vẫn nằm trong `features/places` và `features/map`.

## 24. Khi kiến trúc này phù hợp và các trade-off

Kiến trúc hiện tại phù hợp cho MVP vì SEO/server fetch tốt, URL có thể chia sẻ,
browser không cần CORS tới Spring Boot và chỉ hydrate vùng thực sự tương tác.

Các trade-off cần nhớ:

- `no-store` ưu tiên dữ liệu mới nhưng mất lợi ích cache;
- list page tạo ba backend request cho mỗi navigation;
- TypeScript type không phải runtime validation;
- `Promise.all` cho tốc độ tốt nhưng một dependency lỗi làm toàn trang lỗi;
- URL state rõ ràng nhưng mỗi filter/search/page navigation cần server round-trip;
- local selection nhanh, nhưng không bookmark được và chủ đích không nằm trong URL;
- catalog `size=100` chỉ hợp với phạm vi MVP 30–100 địa điểm.

Không nên chuyển toàn bộ page thành Client Component và fetch bằng `useEffect`
chỉ để tránh Server Components: cách đó tăng JavaScript, tạo loading state thủ
công và đưa bài toán CORS trở lại browser. Client-side data library chỉ đáng cân
nhắc khi cần polling, optimistic update, cache client phức tạp hoặc tương tác dữ
liệu dày đặc mà kiến trúc hiện tại chưa có.

## 25. UI ownership map: file nào quản lý thành phần nào?

UI được tổ chức theo bốn tầng. Tầng trên biết nghiệp vụ và được phép ghép tầng
dưới; tầng dưới không import ngược lên tầng trên:

```text
app/                 route, layout, loading/error boundary
  ↓
features/            component và state theo nghiệp vụ Place/Map
  ↓
components/          layout dùng chung và UI primitive
  ↓
globals.css + utils  token, responsive style và helper class
```

`lib/api` và `types` đứng cạnh cây UI: chúng cung cấp dữ liệu/contract, không
render giao diện.

### 25.1. Cây component của `/places`

```text
RootLayout                         app/layout.tsx
├── SiteHeader                     components/layout/site-header.tsx
│   ├── Avatar                     components/ui/avatar.tsx
│   └── Badge                      components/ui/badge.tsx
└── PlacesPage                     app/places/page.tsx
    ├── PlacesErrorState           khi API lỗi có chủ đích
    │   ├── Alert                  components/ui/alert.tsx
    │   └── Button
    └── PlacesExplorer             features/places/components/places-explorer.tsx
        ├── SearchForm             search-form.tsx
        │   ├── Input + Label
        │   └── Button
        ├── CategoryChips          category-chips.tsx
        │   └── Badge + Link
        ├── FilterPanel            filter-panel.tsx (desktop)
        │   ├── FilterSelect
        │   │   └── Select + Label
        │   ├── Input
        │   └── Button
        ├── Sheet                  components/ui/sheet.tsx (mobile)
        │   └── FilterPanel        cùng business component, presentation khác
        ├── PlaceList              place-list.tsx
        │   ├── PlaceCard[]        place-card.tsx
        │   │   ├── Card + Badge + Button
        │   │   └── Link
        │   └── Empty Card         khi content rỗng
        ├── Pagination             features/places/components/pagination.tsx
        │   └── UI Pagination + Button + Link
        ├── MapShell               features/map/map-shell.tsx
        │   └── PlaceMap           dynamic import, chỉ chạy browser
        │       ├── MapViewport
        │       ├── LocationControl
        │       └── React Leaflet markers/tile/zoom
        └── mobile view switcher   nằm trực tiếp trong PlacesExplorer
```

`PlacesExplorer` là owner của bố cục list–map và state chọn địa điểm; nó không
sở hữu HTTP request. `PlacesPage` là owner của fetch; nó không sở hữu click/state
browser.

### 25.2. Cây component của `/places/[slug]`

```text
RootLayout
├── SiteHeader
└── PlaceDetailPage                app/places/[slug]/page.tsx
    └── PlaceDetailView            place-detail-view.tsx
        ├── back Link + Button
        ├── detail hero            Card + Badge
        ├── description Card
        ├── opening-hours Card
        └── location Card
            └── MapShell
                └── PlaceMap       detailMode, một marker, không có preview
```

Khi detail API trả 404, `PlaceDetailPage` gọi `notFound()` và Next.js thay subtree
bằng `app/places/[slug]/not-found.tsx`.

### 25.3. Route và shell files

| File | Thành phần quản lý |
| --- | --- |
| `app/layout.tsx` | `<html lang="vi">`, font, Leaflet/global CSS, metadata mặc định, `SiteHeader` và `children` |
| `app/page.tsx` | Không có UI riêng; redirect `/` sang `/places` |
| `app/places/page.tsx` | Route catalog, fetch ba nguồn dữ liệu, tạo districts, chọn success/API-error UI |
| `app/places/loading.tsx` | Skeleton route trong lúc catalog page đang chờ |
| `app/places/error.tsx` | Error boundary cho lỗi không được xử lý; log và nút `reset()` |
| `app/places/[slug]/page.tsx` | Fetch detail và chuyển API 404 thành `notFound()` |
| `app/places/[slug]/not-found.tsx` | Giao diện 404 riêng của place detail |
| `components/layout/site-header.tsx` | Logo, navigation, “Sắp ra mắt” và user chip dùng chung |

Route file quyết định **trang nào** xuất hiện; nó không chứa CSS chi tiết hoặc
logic Leaflet.

### 25.4. Business components của Place

| File | Owner của UI/logic |
| --- | --- |
| `places-explorer.tsx` | Khung sidebar/map, selected slug, mobile list/map state, desktop/mobile filter placement |
| `search-form.tsx` | GET keyword form và hidden inputs giữ filter đã áp dụng |
| `category-chips.tsx` | Link bật/tắt category và reset page |
| `filter-panel.tsx` | Draft Select state, hidden form inputs, max cost và clear filters |
| `place-list.tsx` | Empty state, map DTO thành cards, scroll tới selected card |
| `place-card.tsx` | Nội dung một summary, nút chọn marker và link detail |
| `pagination.tsx` | Chuyển zero-based page thành nhãn UI và tạo link trước/sau |
| `place-detail-view.tsx` | Hero, mô tả, quick facts, đủ bảy ngày mở cửa, địa chỉ và detail map |
| `places-error-state.tsx` | Phân biệt connection/server error với invalid request và cung cấp retry/reset link |
| `formatters.ts` | Format VND, thời lượng và tên ngày; không render component |
| `search-params.ts` | Parse/tạo URL filter; không render component |

Hai file cùng tên `pagination.tsx` có vai trò khác nhau:

- `components/ui/pagination.tsx`: primitive `<nav>/<ul>/<li>` tổng quát;
- `features/places/components/pagination.tsx`: biết `PlacePage`, page zero-based
  và filter URL của nghiệp vụ.

### 25.5. Map components

| File/component | Owner của UI/logic |
| --- | --- |
| `features/map/map-shell.tsx` | Client/SSR boundary, dynamic import và map loading skeleton |
| `PlaceMap` trong `place-map.tsx` | MapContainer, tile, markers, selected preview và detail mode |
| `MapViewport` | Fit bounds khi places đổi và fly tới selected place |
| `LocationControl` | Nút geolocation và marker vị trí người dùng |
| `markerIcon()` | HTML/CSS marker đánh số, selected state |

Không đưa `MapViewport` hoặc `LocationControl` ra `components/ui`, vì chúng phụ
thuộc Leaflet và nghiệp vụ bản đồ của feature.

### 25.6. UI primitives

| File | Primitive cung cấp | Không được biết |
| --- | --- | --- |
| `ui/button.tsx` | Variant/size, `asChild`, focus và disabled styles | Place slug, filter, API |
| `ui/input.tsx` | Native input đã chuẩn hóa style | Search hay max cost |
| `ui/label.tsx` | Radix Label và accessibility style | Filter field cụ thể |
| `ui/select.tsx` | Trigger/content/item, portal và keyboard behavior | Category/district semantics |
| `ui/sheet.tsx` | Dialog overlay/content/close cho panel trượt | Bộ lọc mobile cụ thể |
| `ui/card.tsx` | Card container và các vùng header/content/footer | Place/detail/empty semantics |
| `ui/badge.tsx` | Badge variants và `asChild` | Category hoặc indoor semantics |
| `ui/alert.tsx` | Alert/title/description/action | HTTP status cụ thể |
| `ui/skeleton.tsx` | Khối loading pulse | Dữ liệu nào đang tải |
| `ui/avatar.tsx` | Avatar image/fallback/group | User authentication |
| `ui/pagination.tsx` | Semantic pagination building blocks | Zero-based backend page |
| `ui/icons.tsx` | SVG icon Mint Map dùng chung | Click handler hoặc nghiệp vụ |

`asChild` cho phép Button/Badge truyền style xuống `Link` mà không sinh HTML sai
như `<button><a>...</a></button>`.

### 25.7. Server/Client ownership

| Boundary | Lý do |
| --- | --- |
| `PlacesPage`, `PlaceDetailPage`, `PlaceDetailView`, `SiteHeader` | Có thể render server; không cần browser state trực tiếp |
| `PlacesExplorer` | `useState`, click handler và `window.matchMedia` |
| `FilterPanel` | Draft state và interactive Radix Select |
| `PlaceList` | `useEffect` dùng DOM `scrollIntoView` |
| `PlacesErrorState` | `useRouter().refresh()` |
| `MapShell`, `PlaceMap` | Dynamic import, Leaflet, DOM và browser geolocation |
| `app/places/error.tsx` | Error boundary cần effect và nút reset |
| `Select`, `Sheet`, `Avatar`, `Label` primitives | Radix interactive/client implementation |

Một file không có `"use client"` vẫn có thể đi vào client bundle nếu được import
bên dưới một Client Component. Ví dụ `PlaceCard` không tự khai báo client, nhưng
`PlaceList` là Client Component và import nó; do đó card tham gia client subtree.
Ngược lại `PlaceDetailView` có thể ở server và chỉ nhúng `MapShell` như một client
island nhỏ.

### 25.8. Ai quản lý style?

Style có hai lớp:

1. `components/ui/*.tsx` chứa Tailwind utility của primitive và variants;
2. `app/globals.css` chứa Mint Map tokens, business class, Leaflet overrides và
   responsive layout của toàn trang.

`globals.css` sở hữu:

- semantic colors/radius/shadow trong `:root`;
- header, sidebar, list, card, map, detail và state screens;
- breakpoint tablet/mobile;
- marker Leaflet và control positioning;
- reduced-motion behavior.

`lib/utils.ts` cung cấp `cn()` để ghép class có điều kiện và giải quyết Tailwind
class xung đột. `components.json` chỉ cấu hình shadcn registry/alias; nó không
render UI và không điều khiển business layout.

### 25.9. Tìm đúng file khi muốn thay đổi UI

| Muốn thay đổi | File bắt đầu đọc |
| --- | --- |
| Header/logo/user chip | `components/layout/site-header.tsx` + header selectors trong `globals.css` |
| Bố cục tỷ lệ list/map | `places-explorer.tsx` + `.places-explorer` trong `globals.css` |
| Nội dung một card | `place-card.tsx` |
| Card co giãn, spacing, selected border | `.place-card*` trong `globals.css` |
| Search giữ/bỏ filter nào | `search-form.tsx` |
| Filter submit và draft Select | `filter-panel.tsx` |
| URL sau khi bấm chip/page | `search-params.ts` |
| Mobile filter modal | placement trong `places-explorer.tsx`, behavior trong `ui/sheet.tsx`, appearance trong `globals.css` |
| Marker, zoom, fly-to | `features/map/place-map.tsx` |
| Tránh SSR Leaflet | `features/map/map-shell.tsx` |
| Detail hero/opening hours | `place-detail-view.tsx` |
| Màu Button dùng toàn app | token `:root` và `ui/button.tsx` |
| Lỗi API hiển thị thế nào | route page + `places-error-state.tsx` |

Nguyên tắc chọn nơi sửa: thay đổi **nghiệp vụ/nội dung** ở `features`, thay đổi
**primitive tái sử dụng** ở `components/ui`, thay đổi **route lifecycle** ở
`app`, và thay đổi **presentation responsive** ở `globals.css`.

Liên quan: [[01-Requirements/Features/FEAT-001-place-catalog-api-mvp]],
[[01-Requirements/Features/FEAT-002-place-detail-category-opening-hours]],
[[01-Requirements/Features/FEAT-003-place-search-filter-pagination]] và
[[01-Requirements/Features/Mint-Map-shadcn-ui-refactor]],
[[04-API/Place-API-v1]].
