# SaigonPlanTravel Frontend

Next.js App Router frontend cho màn hình khám phá và chi tiết địa điểm.

## Chạy local

Từ root repository, khởi động database:

```bash
docker compose --env-file .env -f infra/compose.yaml up -d postgres
```

Khởi động backend trong terminal thứ nhất:

```bash
cd backend
set -a
source ../.env
set +a
./mvnw spring-boot:run
```

Khởi động frontend trong terminal thứ hai:

```bash
cd frontend
npm install
npm run dev
```

Mở `http://localhost:3000/places`. Backend mặc định là
`http://localhost:8080`. API client chạy ở phía Next.js server nên browser
không gọi Spring Boot trực tiếp và không phụ thuộc CORS.

Để đổi địa chỉ backend, export biến trước khi chạy:

```bash
BACKEND_API_BASE_URL=http://localhost:8081 npm run dev
```

Biến mẫu được ghi trong `../.env.example`. Không đưa secret vào file này.

## Kiểm tra

```bash
npm run lint
npm run build
```

Repository hiện chưa cấu hình frontend test runner, vì vậy không có script
`npm test`. Build không yêu cầu backend đang chạy; `/places` và
`/places/[slug]` được render động khi có request.

## Routes

- `/` chuyển hướng đến `/places`.
- `/places` đọc filter từ URL search parameters.
- `/places/[slug]` hiển thị detail, category, opening hours và vị trí thật từ API.
