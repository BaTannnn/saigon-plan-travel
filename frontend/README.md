# SaigonPlanTravel Frontend

Next.js App Router frontend cho màn hình khám phá và chi tiết địa điểm.

## Chạy local

Từ root repository, khởi động database:

```bash
docker compose --env-file .env -f infra/compose.yaml up -d
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
