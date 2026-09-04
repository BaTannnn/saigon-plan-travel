SaigonPlanTravel - seed_hcm_catalog_70.sql

Mục đích
- Thay thế seed_hcm_catalog.sql 42 Place hiện tại bằng bản 70 Place.
- 70 slug trong seed khớp 1-1 với 70 corpus hiện tại.
- Tất cả 70 Place được để active = TRUE trong development seed để scripts/ingest_corpus.py có thể resolve place_id.
- File vẫn là development seed, KHÔNG phải Flyway migration.

Cách dùng
1. Copy file:
   seed_hcm_catalog_70.sql
   -> backend/src/main/resources/db/dev/seed_hcm_catalog.sql

2. Từ project root chạy:
   docker compose --env-file .env -f infra/compose.yaml exec -T postgres \
   sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
   < backend/src/main/resources/db/dev/seed_hcm_catalog.sql

3. Kiểm tra số Place active:
   SELECT COUNT(*) FROM places WHERE active = TRUE;

4. Kiểm tra Quang San:
   SELECT id, name, slug, active
   FROM places
   WHERE slug = 'bao-tang-nghe-thuat-quang-san';

5. Sau đó ingest lại:
   cd ai-service
   python -m scripts.ingest_corpus

28 Place được bổ sung so với seed 42 Place:
- dia-dao-cu-chi
- chua-buu-long
- cong-vien-bo-song-sai-gon
- khu-du-lich-sinh-thai-vam-sat
- lam-vien-can-gio-dao-khi
- khu-di-tich-nga-ba-giong
- bao-tang-chien-dich-ho-chi-minh
- cho-hanh-thong-tay
- chua-xa-loi
- nha-tho-huyen-sy
- cho-ba-chieu
- bao-tang-quan-khu-7
- cong-vien-hoang-van-thu
- den-mariamman
- mieu-noi-phu-chau
- dinh-thong-tay-hoi
- chua-pho-quang
- khu-du-lich-binh-quoi-2
- viet-nam-quoc-tu
- chua-phap-hoa-truong-sa
- hoi-quan-nghia-an
- bao-tang-biet-dong-sai-gon-gia-dinh
- du-lich-cong-dong-thieng-lieng
- chua-hoang-phap
- chua-nam-thien-nhat-tru
- mieu-nhi-phu
- cong-vien-lich-su-van-hoa-dan-toc
- bao-tang-nghe-thuat-quang-san
