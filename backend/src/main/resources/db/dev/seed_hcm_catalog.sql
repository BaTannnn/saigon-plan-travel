-- SaigonPlanTravel - curated HCMC development catalog
-- Generated for local development / thesis demo.
--
-- IMPORTANT:
-- 1) This is intentionally NOT a Flyway migration.
--    The current integration tests rely on the canonical V11 demo seed.
--    Keep this file outside db/migration and execute it manually on the local development database.
-- 2) Place names and approximate coordinates represent real HCMC attractions.
--    Opening hours and costs in this seed are DEVELOPMENT ESTIMATES, not official real-time data.
--    They are deliberately complete (7 rows/place) so the scheduler can exercise opening-hours constraints.
-- 3) The five slugs currently present in ai-service/corpus are included exactly:
--    dinh-doc-lap, bao-tang-my-thuat-tphcm, buu-dien-trung-tam-sai-gon,
--    thao-cam-vien-sai-gon, cho-ben-thanh.
--
-- The script is idempotent for the seeded catalog: rerunning updates place/category data,
-- rebuilds category mappings and opening-hour rows, and keeps generated public_id values stable.

BEGIN;

-- Hide the old canonical Flyway demo records from the public catalog.
-- They remain in the database so integration-test assumptions tied to V11 are untouched.
UPDATE places
SET active = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE slug LIKE 'demo-%';

-- Curated categories.
INSERT INTO categories (name, slug, description)
VALUES
    ('Văn hóa', 'van-hoa', 'Không gian, di sản và trải nghiệm gắn với đời sống văn hóa.'),
    ('Lịch sử', 'lich-su', 'Di tích, bảo tàng và địa điểm phù hợp để tìm hiểu lịch sử.'),
    ('Nghệ thuật', 'nghe-thuat', 'Bảo tàng, triển lãm và trải nghiệm nghệ thuật.'),
    ('Khoa học', 'khoa-hoc', 'Địa điểm có yếu tố giáo dục, khoa học hoặc khám phá.'),
    ('Ngoài trời', 'ngoai-troi', 'Không gian chủ yếu phù hợp cho hoạt động ngoài trời.'),
    ('Kiến trúc', 'kien-truc', 'Công trình có giá trị hoặc điểm nhấn kiến trúc.'),
    ('Tôn giáo', 'ton-giao', 'Công trình và không gian tín ngưỡng, tôn giáo.'),
    ('Mua sắm', 'mua-sam', 'Chợ, khu thương mại và trải nghiệm mua sắm.'),
    ('Ẩm thực', 'am-thuc', 'Địa điểm phù hợp kết hợp ăn uống và khám phá ẩm thực.'),
    ('Gia đình', 'gia-dinh', 'Điểm đến phù hợp cho nhóm gia đình và trẻ em.'),
    ('Thiên nhiên', 'thien-nhien', 'Không gian xanh, sinh thái và trải nghiệm thiên nhiên.'),
    ('Vui chơi', 'vui-choi', 'Điểm đến thiên về giải trí và hoạt động tương tác.'),
    ('Ngắm cảnh', 'ngam-canh', 'Điểm dừng phù hợp để ngắm cảnh, chụp ảnh và quan sát đô thị.')
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- Curated place catalog.
INSERT INTO places (
    name,
    slug,
    short_description,
    full_description,
    address,
    latitude,
    longitude,
    estimated_visit_minutes,
    min_cost,
    max_cost,
    indoor,
    active
)
VALUES
    (
        'Dinh Độc Lập', 'dinh-doc-lap',
        'Di tích lịch sử và công trình kiến trúc tiêu biểu ở trung tâm TP.HCM.',
        'Điểm tham quan phù hợp với người quan tâm lịch sử Việt Nam thế kỷ XX, kiến trúc hiện đại và không gian di sản đô thị.',
        '135 Nam Kỳ Khởi Nghĩa, TP.HCM',
        10.7770410, 106.6953290, 120, 40000, 65000, TRUE, TRUE
    ),
    (
        'Bảo tàng Mỹ thuật TP.HCM', 'bao-tang-my-thuat-tphcm',
        'Bảo tàng mỹ thuật trong một công trình kiến trúc cổ nổi bật.',
        'Không gian trưng bày mỹ thuật cổ, cận đại và hiện đại; phù hợp với trải nghiệm nghệ thuật, văn hóa và kiến trúc.',
        '97A Phó Đức Chính, TP.HCM',
        10.7699260, 106.6999040, 120, 30000, 50000, TRUE, TRUE
    ),
    (
        'Bưu điện Trung tâm Sài Gòn', 'buu-dien-trung-tam-sai-gon',
        'Công trình bưu chính lịch sử nằm tại khu trung tâm thành phố.',
        'Điểm dừng ngắn phù hợp để quan sát kiến trúc, chụp ảnh và kết hợp đi bộ với các địa điểm lân cận.',
        '2 Công xã Paris, TP.HCM',
        10.7798870, 106.6999660, 45, 0, 50000, TRUE, TRUE
    ),
    (
        'Thảo Cầm Viên Sài Gòn', 'thao-cam-vien-sai-gon',
        'Vườn thú và vườn thực vật lâu đời với nhiều mảng xanh.',
        'Phù hợp với gia đình, người yêu thiên nhiên và các hoạt động học tập ngoài trời về động thực vật.',
        '2 Nguyễn Bỉnh Khiêm, TP.HCM',
        10.7871760, 106.7054280, 180, 60000, 100000, FALSE, TRUE
    ),
    (
        'Chợ Bến Thành', 'cho-ben-thanh',
        'Chợ truyền thống nổi tiếng tại khu vực trung tâm TP.HCM.',
        'Không gian tham quan, mua sắm và trải nghiệm nhịp sống thương mại địa phương; chi phí phụ thuộc nhu cầu mua sắm và ăn uống.',
        'Lê Lợi, TP.HCM',
        10.7725150, 106.6980410, 90, 0, 500000, TRUE, TRUE
    ),
    (
        'Bảo tàng Chứng tích Chiến tranh', 'bao-tang-chung-tich-chien-tranh',
        'Bảo tàng chuyên đề về chiến tranh và lịch sử Việt Nam hiện đại.',
        'Phù hợp với người muốn tìm hiểu lịch sử, tư liệu, hiện vật và các góc nhìn về hậu quả chiến tranh.',
        '28 Võ Văn Tần, TP.HCM',
        10.7794870, 106.6920260, 120, 40000, 50000, TRUE, TRUE
    ),
    (
        'Bảo tàng Lịch sử TP.HCM', 'bao-tang-lich-su-tphcm',
        'Bảo tàng giới thiệu nhiều giai đoạn lịch sử và văn hóa Việt Nam.',
        'Điểm tham quan trong nhà phù hợp với người quan tâm khảo cổ, lịch sử, văn hóa và hiện vật truyền thống.',
        '2 Nguyễn Bỉnh Khiêm, TP.HCM',
        10.7871120, 106.7048180, 120, 30000, 50000, TRUE, TRUE
    ),
    (
        'Bảo tàng Thành phố Hồ Chí Minh', 'bao-tang-thanh-pho-ho-chi-minh',
        'Bảo tàng nằm trong một công trình kiến trúc cổ ở trung tâm thành phố.',
        'Nội dung tham quan gắn với lịch sử hình thành, phát triển đô thị và đời sống văn hóa của Sài Gòn - TP.HCM.',
        '65 Lý Tự Trọng, TP.HCM',
        10.7765930, 106.6994890, 90, 30000, 50000, TRUE, TRUE
    ),
    (
        'Bảo tàng Phụ nữ Nam Bộ', 'bao-tang-phu-nu-nam-bo',
        'Không gian trưng bày về lịch sử, đời sống và đóng góp của phụ nữ Nam Bộ.',
        'Phù hợp với trải nghiệm văn hóa - lịch sử trong nhà, thời lượng tham quan vừa phải.',
        '202 Võ Thị Sáu, TP.HCM',
        10.7879120, 106.6907130, 90, 0, 30000, TRUE, TRUE
    ),
    (
        'Bảo tàng Tôn Đức Thắng', 'bao-tang-ton-duc-thang',
        'Bảo tàng chuyên đề về Chủ tịch Tôn Đức Thắng.',
        'Điểm tham quan lịch sử trong nhà, phù hợp với hành trình tìm hiểu nhân vật và lịch sử Việt Nam hiện đại.',
        '5 Tôn Đức Thắng, TP.HCM',
        10.7813310, 106.7076690, 75, 0, 30000, TRUE, TRUE
    ),
    (
        'Bến Nhà Rồng - Bảo tàng Hồ Chí Minh', 'ben-nha-rong-bao-tang-ho-chi-minh',
        'Di tích lịch sử ven sông gắn với Bến Nhà Rồng.',
        'Kết hợp trải nghiệm lịch sử, kiến trúc và cảnh quan sông Sài Gòn; phù hợp cho hành trình khu trung tâm.',
        '1 Nguyễn Tất Thành, TP.HCM',
        10.7681690, 106.7066330, 90, 0, 40000, TRUE, TRUE
    ),
    (
        'Nhà thờ Đức Bà Sài Gòn', 'nha-tho-duc-ba-sai-gon',
        'Công trình tôn giáo và kiến trúc biểu tượng tại khu trung tâm.',
        'Dữ liệu seed coi đây chủ yếu là điểm tham quan ngoại thất và chụp ảnh; việc vào bên trong phụ thuộc hoạt động tôn giáo thực tế.',
        '1 Công xã Paris, TP.HCM',
        10.7797850, 106.6990180, 30, 0, 0, FALSE, TRUE
    ),
    (
        'Nhà hát Thành phố Hồ Chí Minh', 'nha-hat-thanh-pho-ho-chi-minh',
        'Công trình biểu diễn nghệ thuật với kiến trúc nổi bật tại trung tâm.',
        'Phù hợp để ngắm kiến trúc, chụp ảnh và kết hợp với phố đi bộ; chi phí trong seed không bao gồm vé chương trình biểu diễn.',
        '7 Công trường Lam Sơn, TP.HCM',
        10.7764490, 106.7030610, 30, 0, 0, FALSE, TRUE
    ),
    (
        'Phố đi bộ Nguyễn Huệ', 'pho-di-bo-nguyen-hue',
        'Không gian đi bộ, sự kiện và sinh hoạt đô thị ở trung tâm TP.HCM.',
        'Phù hợp với lịch trình chiều tối, đi bộ, chụp ảnh và kết hợp các điểm trung tâm gần nhau.',
        'Nguyễn Huệ, TP.HCM',
        10.7743570, 106.7039180, 60, 0, 100000, FALSE, TRUE
    ),
    (
        'Công viên Bến Bạch Đằng', 'cong-vien-ben-bach-dang',
        'Không gian công viên ven sông Sài Gòn ở khu trung tâm.',
        'Phù hợp với nghỉ chân, đi bộ, ngắm sông và kết hợp lịch trình với Nguyễn Huệ hoặc khu trung tâm.',
        'Tôn Đức Thắng, TP.HCM',
        10.7736570, 106.7070570, 45, 0, 50000, FALSE, TRUE
    ),
    (
        'Bến Bạch Đằng', 'ben-bach-dang',
        'Khu bến ven sông gắn với cảnh quan trung tâm Sài Gòn.',
        'Điểm dừng ngắn để ngắm sông, chụp ảnh và kết nối với các trải nghiệm ven sông khác.',
        'Tôn Đức Thắng, TP.HCM',
        10.7727030, 106.7078050, 45, 0, 100000, FALSE, TRUE
    ),
    (
        'Landmark 81', 'landmark-81',
        'Tòa nhà cao tầng nổi bật bên sông Sài Gòn.',
        'Phù hợp với trải nghiệm kiến trúc hiện đại, mua sắm, ăn uống và ngắm cảnh khu đô thị.',
        '720A Điện Biên Phủ, TP.HCM',
        10.7949670, 106.7218470, 120, 0, 500000, TRUE, TRUE
    ),
    (
        'Saigon Skydeck - Bitexco Financial Tower', 'saigon-skydeck-bitexco',
        'Đài quan sát trên cao tại khu trung tâm thành phố.',
        'Phù hợp với trải nghiệm ngắm toàn cảnh đô thị; chi phí seed dùng khoảng giá ước tính cho hoạt động tham quan.',
        '36 Hồ Tùng Mậu, TP.HCM',
        10.7716370, 106.7043310, 75, 200000, 300000, TRUE, TRUE
    ),
    (
        'Đường sách Nguyễn Văn Bình', 'duong-sach-nguyen-van-binh',
        'Không gian sách và sinh hoạt văn hóa ngay khu trung tâm.',
        'Phù hợp với lịch trình nhẹ nhàng, đọc sách, nghỉ chân và kết hợp cùng Bưu điện Trung tâm hoặc Nhà thờ Đức Bà.',
        'Nguyễn Văn Bình, TP.HCM',
        10.7802310, 106.6996350, 60, 0, 200000, FALSE, TRUE
    ),
    (
        'Công viên Tao Đàn', 'cong-vien-tao-dan',
        'Công viên xanh lớn ở khu vực trung tâm.',
        'Phù hợp với đi bộ, nghỉ ngơi và cân bằng lịch trình có nhiều điểm tham quan trong nhà.',
        'Trương Định, TP.HCM',
        10.7744840, 106.6932860, 60, 0, 0, FALSE, TRUE
    ),
    (
        'Hồ Con Rùa', 'ho-con-rua',
        'Không gian công cộng và điểm gặp gỡ quen thuộc ở trung tâm.',
        'Phù hợp với điểm dừng ngắn, ăn vặt, nghỉ chân và kết hợp các điểm gần Nhà thờ Đức Bà.',
        'Công trường Quốc tế, TP.HCM',
        10.7824550, 106.6958730, 45, 0, 100000, FALSE, TRUE
    ),
    (
        'Chùa Ngọc Hoàng', 'chua-ngoc-hoang',
        'Ngôi chùa nổi tiếng với không gian tín ngưỡng và kiến trúc đặc trưng.',
        'Phù hợp với người quan tâm văn hóa tâm linh, kiến trúc tôn giáo và trải nghiệm không gian yên tĩnh.',
        '73 Mai Thị Lựu, TP.HCM',
        10.7918920, 106.6983560, 60, 0, 50000, TRUE, TRUE
    ),
    (
        'Chùa Vĩnh Nghiêm', 'chua-vinh-nghiem',
        'Ngôi chùa lớn với kiến trúc Phật giáo nổi bật.',
        'Điểm tham quan văn hóa tâm linh phù hợp cho lịch trình nhẹ nhàng và trải nghiệm kiến trúc tôn giáo.',
        '339 Nam Kỳ Khởi Nghĩa, TP.HCM',
        10.7880620, 106.6822910, 60, 0, 50000, TRUE, TRUE
    ),
    (
        'Nhà thờ Tân Định', 'nha-tho-tan-dinh',
        'Nhà thờ có màu hồng đặc trưng và kiến trúc nổi bật.',
        'Phù hợp với tham quan kiến trúc, chụp ảnh ngoại thất và khám phá khu vực Tân Định.',
        '289 Hai Bà Trưng, TP.HCM',
        10.7889710, 106.6903020, 45, 0, 0, FALSE, TRUE
    ),
    (
        'Chùa Giác Lâm', 'chua-giac-lam',
        'Ngôi chùa cổ mang giá trị lịch sử và kiến trúc.',
        'Phù hợp với hành trình tìm hiểu văn hóa Phật giáo, kiến trúc truyền thống và di sản đô thị.',
        '565 Lạc Long Quân, TP.HCM',
        10.7719310, 106.6494010, 75, 0, 50000, TRUE, TRUE
    ),
    (
        'Chùa Bà Thiên Hậu', 'chua-ba-thien-hau',
        'Cơ sở tín ngưỡng nổi bật trong không gian văn hóa Chợ Lớn.',
        'Phù hợp với trải nghiệm kiến trúc, tín ngưỡng và văn hóa cộng đồng người Hoa tại TP.HCM.',
        '710 Nguyễn Trãi, TP.HCM',
        10.7527450, 106.6606410, 60, 0, 50000, TRUE, TRUE
    ),
    (
        'Nhà thờ Cha Tam', 'nha-tho-cha-tam',
        'Nhà thờ Công giáo trong khu vực Chợ Lớn với dấu ấn kiến trúc riêng.',
        'Phù hợp với hành trình khám phá kiến trúc tôn giáo và sự đa dạng văn hóa ở khu vực Chợ Lớn.',
        '25 Học Lạc, TP.HCM',
        10.7520860, 106.6547780, 45, 0, 0, TRUE, TRUE
    ),
    (
        'Lăng Ông Bà Chiểu', 'lang-ong-ba-chieu',
        'Quần thể di tích tín ngưỡng và lịch sử tại khu vực Bà Chiểu.',
        'Phù hợp với người quan tâm văn hóa Nam Bộ, kiến trúc truyền thống và lịch sử địa phương.',
        '1 Vũ Tùng, TP.HCM',
        10.8035530, 106.6969760, 60, 0, 50000, FALSE, TRUE
    ),
    (
        'Chợ Bình Tây', 'cho-binh-tay',
        'Chợ đầu mối lâu đời và công trình kiến trúc nổi bật của khu vực Chợ Lớn.',
        'Phù hợp với trải nghiệm thương mại địa phương, kiến trúc chợ và văn hóa cộng đồng người Hoa.',
        '57A Tháp Mười, TP.HCM',
        10.7491610, 106.6514040, 90, 0, 300000, TRUE, TRUE
    ),
    (
        'Chợ Hồ Thị Kỷ', 'cho-ho-thi-ky',
        'Khu chợ hoa và ẩm thực có nhịp sống sôi động.',
        'Phù hợp với trải nghiệm chợ địa phương, ẩm thực và chụp ảnh; mức chi phí phụ thuộc nhu cầu ăn uống và mua sắm.',
        'Hồ Thị Kỷ, TP.HCM',
        10.7624260, 106.6822170, 90, 0, 300000, FALSE, TRUE
    ),
    (
        'Chợ Tân Định', 'cho-tan-dinh',
        'Chợ truyền thống nằm gần Nhà thờ Tân Định.',
        'Phù hợp với lịch trình kết hợp mua sắm, ẩm thực và khám phá đời sống khu dân cư trung tâm.',
        '336 Hai Bà Trưng, TP.HCM',
        10.7892340, 106.6893290, 60, 0, 300000, TRUE, TRUE
    ),
    (
        'Phố đi bộ Bùi Viện', 'pho-di-bo-bui-vien',
        'Khu phố sôi động với nhiều hoạt động ăn uống và giải trí về đêm.',
        'Phù hợp với lịch trình buổi tối; seed giới hạn giờ đến trước nửa đêm để phù hợp mô hình opening-hours hiện tại.',
        'Bùi Viện, TP.HCM',
        10.7674600, 106.6938450, 90, 0, 500000, FALSE, TRUE
    ),
    (
        'Công viên Gia Định', 'cong-vien-gia-dinh',
        'Công viên nhiều cây xanh gần khu vực sân bay.',
        'Phù hợp với gia đình, đi bộ, nghỉ ngơi và các hoạt động ngoài trời có chi phí thấp.',
        'Hoàng Minh Giám, TP.HCM',
        10.8129980, 106.6760270, 75, 0, 0, FALSE, TRUE
    ),
    (
        'Công viên Văn hóa Đầm Sen', 'cong-vien-van-hoa-dam-sen',
        'Khu vui chơi giải trí lâu năm với nhiều hoạt động gia đình.',
        'Phù hợp cho lịch trình nửa ngày hoặc nhiều giờ, đặc biệt với nhóm gia đình; chi phí seed là khoảng ước tính cho vé và dịch vụ.',
        '3 Hòa Bình, TP.HCM',
        10.7677280, 106.6393510, 240, 150000, 350000, FALSE, TRUE
    ),
    (
        'Khu du lịch Suối Tiên', 'khu-du-lich-suoi-tien',
        'Khu vui chơi quy mô lớn ở phía đông TP.HCM.',
        'Phù hợp với gia đình và lịch trình dài; khoảng cách xa trung tâm giúp kiểm thử tốt yếu tố travel-time trong scheduler.',
        '120 Xa lộ Hà Nội, TP.HCM',
        10.8669020, 106.8022020, 300, 150000, 400000, FALSE, TRUE
    ),
    (
        'Cầu Ánh Sao', 'cau-anh-sao',
        'Cầu đi bộ nổi bật trong khu Hồ Bán Nguyệt.',
        'Phù hợp với trải nghiệm chiều tối, đi bộ và ngắm cảnh; hiệu ứng ánh sáng thường phù hợp hơn vào buổi tối.',
        'Khu Hồ Bán Nguyệt, TP.HCM',
        10.7293610, 106.7188390, 45, 0, 0, FALSE, TRUE
    ),
    (
        'Hồ Bán Nguyệt', 'ho-ban-nguyet',
        'Không gian mặt nước và đi bộ tại khu đô thị phía nam thành phố.',
        'Phù hợp với nghỉ ngơi, đi bộ, gia đình và kết hợp Cầu Ánh Sao trong cùng cụm hành trình.',
        'Khu Hồ Bán Nguyệt, TP.HCM',
        10.7299240, 106.7197610, 60, 0, 100000, FALSE, TRUE
    ),
    (
        'Khu du lịch Bình Quới 1', 'khu-du-lich-binh-quoi-1',
        'Không gian xanh ven sông mang phong cách làng quê Nam Bộ.',
        'Phù hợp với nghỉ ngơi, ăn uống, gia đình và trải nghiệm ngoài trời trong khu vực Thanh Đa.',
        '1147 Bình Quới, TP.HCM',
        10.8319610, 106.7275820, 150, 0, 500000, FALSE, TRUE
    ),
    (
        'Khu du lịch Văn Thánh', 'khu-du-lich-van-thanh',
        'Khu sinh thái và ẩm thực gần trung tâm thành phố.',
        'Phù hợp với nghỉ ngơi, ăn uống và trải nghiệm không gian xanh; chi phí phụ thuộc dịch vụ sử dụng.',
        '48/10 Điện Biên Phủ, TP.HCM',
        10.7962200, 106.7155540, 120, 0, 500000, FALSE, TRUE
    ),
    (
        'Bảo tàng Áo dài', 'bao-tang-ao-dai',
        'Không gian văn hóa giới thiệu áo dài Việt Nam trong khuôn viên xanh.',
        'Phù hợp với người quan tâm văn hóa, thời trang truyền thống, nhiếp ảnh và trải nghiệm kiến trúc - cảnh quan.',
        '206/19/30 Long Thuận, TP.HCM',
        10.8393820, 106.8223920, 150, 100000, 150000, FALSE, TRUE
    ),
    (
        'Artinus 3D Art Museum', 'artinus-3d-art-museum',
        'Không gian tranh 3D tương tác trong nhà.',
        'Phù hợp với nhóm bạn và gia đình muốn chụp ảnh, trải nghiệm nghệ thuật thị giác và hoạt động trong nhà.',
        '2 Đường số 9, TP.HCM',
        10.7419390, 106.6870450, 120, 150000, 250000, TRUE, TRUE
    ),
    (
        'Bảo tàng Y học cổ truyền Việt Nam (FITO)', 'bao-tang-y-hoc-co-truyen-viet-nam',
        'Bảo tàng tư nhân về lịch sử và tri thức y học cổ truyền Việt Nam.',
        'Không gian trong nhà phù hợp với người quan tâm lịch sử khoa học, dược liệu, y học truyền thống và kiến trúc trưng bày.',
        '41 Hoàng Dư Khương, TP.HCM',
        10.7749620, 106.6669490, 90, 120000, 200000, TRUE, TRUE
    )
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    short_description = EXCLUDED.short_description,
    full_description = EXCLUDED.full_description,
    address = EXCLUDED.address,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    estimated_visit_minutes = EXCLUDED.estimated_visit_minutes,
    min_cost = EXCLUDED.min_cost,
    max_cost = EXCLUDED.max_cost,
    indoor = EXCLUDED.indoor,
    active = EXCLUDED.active,
    updated_at = CURRENT_TIMESTAMP;

-- Rebuild category mappings for the curated places.
DELETE FROM place_categories
WHERE place_id IN (
    SELECT id
    FROM places
    WHERE slug IN (
        'dinh-doc-lap',
        'bao-tang-my-thuat-tphcm',
        'buu-dien-trung-tam-sai-gon',
        'thao-cam-vien-sai-gon',
        'cho-ben-thanh',
        'bao-tang-chung-tich-chien-tranh',
        'bao-tang-lich-su-tphcm',
        'bao-tang-thanh-pho-ho-chi-minh',
        'bao-tang-phu-nu-nam-bo',
        'bao-tang-ton-duc-thang',
        'ben-nha-rong-bao-tang-ho-chi-minh',
        'nha-tho-duc-ba-sai-gon',
        'nha-hat-thanh-pho-ho-chi-minh',
        'pho-di-bo-nguyen-hue',
        'cong-vien-ben-bach-dang',
        'ben-bach-dang',
        'landmark-81',
        'saigon-skydeck-bitexco',
        'duong-sach-nguyen-van-binh',
        'cong-vien-tao-dan',
        'ho-con-rua',
        'chua-ngoc-hoang',
        'chua-vinh-nghiem',
        'nha-tho-tan-dinh',
        'chua-giac-lam',
        'chua-ba-thien-hau',
        'nha-tho-cha-tam',
        'lang-ong-ba-chieu',
        'cho-binh-tay',
        'cho-ho-thi-ky',
        'cho-tan-dinh',
        'pho-di-bo-bui-vien',
        'cong-vien-gia-dinh',
        'cong-vien-van-hoa-dam-sen',
        'khu-du-lich-suoi-tien',
        'cau-anh-sao',
        'ho-ban-nguyet',
        'khu-du-lich-binh-quoi-1',
        'khu-du-lich-van-thanh',
        'bao-tang-ao-dai',
        'artinus-3d-art-museum',
        'bao-tang-y-hoc-co-truyen-viet-nam'
    )
);

INSERT INTO place_categories (place_id, category_id)
SELECT p.id, c.id
FROM (
    VALUES
        ('dinh-doc-lap', 'lich-su'),
        ('dinh-doc-lap', 'kien-truc'),
        ('dinh-doc-lap', 'van-hoa'),
        ('bao-tang-my-thuat-tphcm', 'nghe-thuat'),
        ('bao-tang-my-thuat-tphcm', 'van-hoa'),
        ('bao-tang-my-thuat-tphcm', 'kien-truc'),
        ('buu-dien-trung-tam-sai-gon', 'kien-truc'),
        ('buu-dien-trung-tam-sai-gon', 'lich-su'),
        ('buu-dien-trung-tam-sai-gon', 'van-hoa'),
        ('thao-cam-vien-sai-gon', 'thien-nhien'),
        ('thao-cam-vien-sai-gon', 'gia-dinh'),
        ('thao-cam-vien-sai-gon', 'khoa-hoc'),
        ('thao-cam-vien-sai-gon', 'ngoai-troi'),
        ('cho-ben-thanh', 'mua-sam'),
        ('cho-ben-thanh', 'am-thuc'),
        ('cho-ben-thanh', 'van-hoa'),
        ('bao-tang-chung-tich-chien-tranh', 'lich-su'),
        ('bao-tang-chung-tich-chien-tranh', 'van-hoa'),
        ('bao-tang-lich-su-tphcm', 'lich-su'),
        ('bao-tang-lich-su-tphcm', 'van-hoa'),
        ('bao-tang-thanh-pho-ho-chi-minh', 'lich-su'),
        ('bao-tang-thanh-pho-ho-chi-minh', 'van-hoa'),
        ('bao-tang-thanh-pho-ho-chi-minh', 'kien-truc'),
        ('bao-tang-phu-nu-nam-bo', 'lich-su'),
        ('bao-tang-phu-nu-nam-bo', 'van-hoa'),
        ('bao-tang-ton-duc-thang', 'lich-su'),
        ('bao-tang-ton-duc-thang', 'van-hoa'),
        ('ben-nha-rong-bao-tang-ho-chi-minh', 'lich-su'),
        ('ben-nha-rong-bao-tang-ho-chi-minh', 'van-hoa'),
        ('ben-nha-rong-bao-tang-ho-chi-minh', 'kien-truc'),
        ('nha-tho-duc-ba-sai-gon', 'kien-truc'),
        ('nha-tho-duc-ba-sai-gon', 'ton-giao'),
        ('nha-tho-duc-ba-sai-gon', 'lich-su'),
        ('nha-hat-thanh-pho-ho-chi-minh', 'kien-truc'),
        ('nha-hat-thanh-pho-ho-chi-minh', 'nghe-thuat'),
        ('nha-hat-thanh-pho-ho-chi-minh', 'van-hoa'),
        ('pho-di-bo-nguyen-hue', 'ngoai-troi'),
        ('pho-di-bo-nguyen-hue', 'van-hoa'),
        ('pho-di-bo-nguyen-hue', 'ngam-canh'),
        ('cong-vien-ben-bach-dang', 'ngoai-troi'),
        ('cong-vien-ben-bach-dang', 'ngam-canh'),
        ('cong-vien-ben-bach-dang', 'thien-nhien'),
        ('ben-bach-dang', 'ngoai-troi'),
        ('ben-bach-dang', 'ngam-canh'),
        ('ben-bach-dang', 'lich-su'),
        ('landmark-81', 'kien-truc'),
        ('landmark-81', 'mua-sam'),
        ('landmark-81', 'ngam-canh'),
        ('saigon-skydeck-bitexco', 'ngam-canh'),
        ('saigon-skydeck-bitexco', 'kien-truc'),
        ('duong-sach-nguyen-van-binh', 'van-hoa'),
        ('duong-sach-nguyen-van-binh', 'mua-sam'),
        ('duong-sach-nguyen-van-binh', 'ngoai-troi'),
        ('cong-vien-tao-dan', 'thien-nhien'),
        ('cong-vien-tao-dan', 'ngoai-troi'),
        ('cong-vien-tao-dan', 'gia-dinh'),
        ('ho-con-rua', 'ngoai-troi'),
        ('ho-con-rua', 'ngam-canh'),
        ('ho-con-rua', 'am-thuc'),
        ('chua-ngoc-hoang', 'ton-giao'),
        ('chua-ngoc-hoang', 'kien-truc'),
        ('chua-ngoc-hoang', 'van-hoa'),
        ('chua-vinh-nghiem', 'ton-giao'),
        ('chua-vinh-nghiem', 'kien-truc'),
        ('chua-vinh-nghiem', 'van-hoa'),
        ('nha-tho-tan-dinh', 'ton-giao'),
        ('nha-tho-tan-dinh', 'kien-truc'),
        ('nha-tho-tan-dinh', 'van-hoa'),
        ('chua-giac-lam', 'ton-giao'),
        ('chua-giac-lam', 'lich-su'),
        ('chua-giac-lam', 'kien-truc'),
        ('chua-ba-thien-hau', 'ton-giao'),
        ('chua-ba-thien-hau', 'van-hoa'),
        ('chua-ba-thien-hau', 'kien-truc'),
        ('nha-tho-cha-tam', 'ton-giao'),
        ('nha-tho-cha-tam', 'kien-truc'),
        ('nha-tho-cha-tam', 'van-hoa'),
        ('lang-ong-ba-chieu', 'lich-su'),
        ('lang-ong-ba-chieu', 'van-hoa'),
        ('lang-ong-ba-chieu', 'kien-truc'),
        ('cho-binh-tay', 'mua-sam'),
        ('cho-binh-tay', 'am-thuc'),
        ('cho-binh-tay', 'van-hoa'),
        ('cho-binh-tay', 'kien-truc'),
        ('cho-ho-thi-ky', 'mua-sam'),
        ('cho-ho-thi-ky', 'am-thuc'),
        ('cho-ho-thi-ky', 'van-hoa'),
        ('cho-tan-dinh', 'mua-sam'),
        ('cho-tan-dinh', 'am-thuc'),
        ('cho-tan-dinh', 'van-hoa'),
        ('pho-di-bo-bui-vien', 'am-thuc'),
        ('pho-di-bo-bui-vien', 'van-hoa'),
        ('pho-di-bo-bui-vien', 'ngoai-troi'),
        ('pho-di-bo-bui-vien', 'vui-choi'),
        ('cong-vien-gia-dinh', 'thien-nhien'),
        ('cong-vien-gia-dinh', 'ngoai-troi'),
        ('cong-vien-gia-dinh', 'gia-dinh'),
        ('cong-vien-van-hoa-dam-sen', 'vui-choi'),
        ('cong-vien-van-hoa-dam-sen', 'gia-dinh'),
        ('cong-vien-van-hoa-dam-sen', 'ngoai-troi'),
        ('khu-du-lich-suoi-tien', 'vui-choi'),
        ('khu-du-lich-suoi-tien', 'gia-dinh'),
        ('khu-du-lich-suoi-tien', 'ngoai-troi'),
        ('khu-du-lich-suoi-tien', 'van-hoa'),
        ('cau-anh-sao', 'ngoai-troi'),
        ('cau-anh-sao', 'ngam-canh'),
        ('ho-ban-nguyet', 'ngoai-troi'),
        ('ho-ban-nguyet', 'ngam-canh'),
        ('ho-ban-nguyet', 'gia-dinh'),
        ('khu-du-lich-binh-quoi-1', 'thien-nhien'),
        ('khu-du-lich-binh-quoi-1', 'gia-dinh'),
        ('khu-du-lich-binh-quoi-1', 'am-thuc'),
        ('khu-du-lich-binh-quoi-1', 'ngoai-troi'),
        ('khu-du-lich-van-thanh', 'thien-nhien'),
        ('khu-du-lich-van-thanh', 'gia-dinh'),
        ('khu-du-lich-van-thanh', 'am-thuc'),
        ('khu-du-lich-van-thanh', 'ngoai-troi'),
        ('bao-tang-ao-dai', 'van-hoa'),
        ('bao-tang-ao-dai', 'nghe-thuat'),
        ('bao-tang-ao-dai', 'kien-truc'),
        ('artinus-3d-art-museum', 'nghe-thuat'),
        ('artinus-3d-art-museum', 'gia-dinh'),
        ('artinus-3d-art-museum', 'vui-choi'),
        ('bao-tang-y-hoc-co-truyen-viet-nam', 'khoa-hoc'),
        ('bao-tang-y-hoc-co-truyen-viet-nam', 'lich-su'),
        ('bao-tang-y-hoc-co-truyen-viet-nam', 'van-hoa')
) AS mapping(place_slug, category_slug)
JOIN places p
    ON p.slug = mapping.place_slug
JOIN categories c
    ON c.slug = mapping.category_slug;

-- Rebuild complete 7-day opening-hour data for the curated places.
DELETE FROM opening_hours
WHERE place_id IN (
    SELECT id
    FROM places
    WHERE slug IN (
        'dinh-doc-lap',
        'bao-tang-my-thuat-tphcm',
        'buu-dien-trung-tam-sai-gon',
        'thao-cam-vien-sai-gon',
        'cho-ben-thanh',
        'bao-tang-chung-tich-chien-tranh',
        'bao-tang-lich-su-tphcm',
        'bao-tang-thanh-pho-ho-chi-minh',
        'bao-tang-phu-nu-nam-bo',
        'bao-tang-ton-duc-thang',
        'ben-nha-rong-bao-tang-ho-chi-minh',
        'nha-tho-duc-ba-sai-gon',
        'nha-hat-thanh-pho-ho-chi-minh',
        'pho-di-bo-nguyen-hue',
        'cong-vien-ben-bach-dang',
        'ben-bach-dang',
        'landmark-81',
        'saigon-skydeck-bitexco',
        'duong-sach-nguyen-van-binh',
        'cong-vien-tao-dan',
        'ho-con-rua',
        'chua-ngoc-hoang',
        'chua-vinh-nghiem',
        'nha-tho-tan-dinh',
        'chua-giac-lam',
        'chua-ba-thien-hau',
        'nha-tho-cha-tam',
        'lang-ong-ba-chieu',
        'cho-binh-tay',
        'cho-ho-thi-ky',
        'cho-tan-dinh',
        'pho-di-bo-bui-vien',
        'cong-vien-gia-dinh',
        'cong-vien-van-hoa-dam-sen',
        'khu-du-lich-suoi-tien',
        'cau-anh-sao',
        'ho-ban-nguyet',
        'khu-du-lich-binh-quoi-1',
        'khu-du-lich-van-thanh',
        'bao-tang-ao-dai',
        'artinus-3d-art-museum',
        'bao-tang-y-hoc-co-truyen-viet-nam'
    )
);

WITH schedule_profiles(profile, day_of_week, open_time, close_time, closed) AS (
    VALUES
        ('DAILY_0800_1700', 1, TIME '08:00', TIME '17:00', FALSE),
        ('DAILY_0800_1700', 2, TIME '08:00', TIME '17:00', FALSE),
        ('DAILY_0800_1700', 3, TIME '08:00', TIME '17:00', FALSE),
        ('DAILY_0800_1700', 4, TIME '08:00', TIME '17:00', FALSE),
        ('DAILY_0800_1700', 5, TIME '08:00', TIME '17:00', FALSE),
        ('DAILY_0800_1700', 6, TIME '08:00', TIME '17:00', FALSE),
        ('DAILY_0800_1700', 7, TIME '08:00', TIME '17:00', FALSE),
        ('DAILY_0730_1730', 1, TIME '07:30', TIME '17:30', FALSE),
        ('DAILY_0730_1730', 2, TIME '07:30', TIME '17:30', FALSE),
        ('DAILY_0730_1730', 3, TIME '07:30', TIME '17:30', FALSE),
        ('DAILY_0730_1730', 4, TIME '07:30', TIME '17:30', FALSE),
        ('DAILY_0730_1730', 5, TIME '07:30', TIME '17:30', FALSE),
        ('DAILY_0730_1730', 6, TIME '07:30', TIME '17:30', FALSE),
        ('DAILY_0730_1730', 7, TIME '07:30', TIME '17:30', FALSE),
        ('DAILY_0700_1900', 1, TIME '07:00', TIME '19:00', FALSE),
        ('DAILY_0700_1900', 2, TIME '07:00', TIME '19:00', FALSE),
        ('DAILY_0700_1900', 3, TIME '07:00', TIME '19:00', FALSE),
        ('DAILY_0700_1900', 4, TIME '07:00', TIME '19:00', FALSE),
        ('DAILY_0700_1900', 5, TIME '07:00', TIME '19:00', FALSE),
        ('DAILY_0700_1900', 6, TIME '07:00', TIME '19:00', FALSE),
        ('DAILY_0700_1900', 7, TIME '07:00', TIME '19:00', FALSE),
        ('DAILY_0600_2000', 1, TIME '06:00', TIME '20:00', FALSE),
        ('DAILY_0600_2000', 2, TIME '06:00', TIME '20:00', FALSE),
        ('DAILY_0600_2000', 3, TIME '06:00', TIME '20:00', FALSE),
        ('DAILY_0600_2000', 4, TIME '06:00', TIME '20:00', FALSE),
        ('DAILY_0600_2000', 5, TIME '06:00', TIME '20:00', FALSE),
        ('DAILY_0600_2000', 6, TIME '06:00', TIME '20:00', FALSE),
        ('DAILY_0600_2000', 7, TIME '06:00', TIME '20:00', FALSE),
        ('DAILY_0600_1900', 1, TIME '06:00', TIME '19:00', FALSE),
        ('DAILY_0600_1900', 2, TIME '06:00', TIME '19:00', FALSE),
        ('DAILY_0600_1900', 3, TIME '06:00', TIME '19:00', FALSE),
        ('DAILY_0600_1900', 4, TIME '06:00', TIME '19:00', FALSE),
        ('DAILY_0600_1900', 5, TIME '06:00', TIME '19:00', FALSE),
        ('DAILY_0600_1900', 6, TIME '06:00', TIME '19:00', FALSE),
        ('DAILY_0600_1900', 7, TIME '06:00', TIME '19:00', FALSE),
        ('DAILY_0700_2200', 1, TIME '07:00', TIME '22:00', FALSE),
        ('DAILY_0700_2200', 2, TIME '07:00', TIME '22:00', FALSE),
        ('DAILY_0700_2200', 3, TIME '07:00', TIME '22:00', FALSE),
        ('DAILY_0700_2200', 4, TIME '07:00', TIME '22:00', FALSE),
        ('DAILY_0700_2200', 5, TIME '07:00', TIME '22:00', FALSE),
        ('DAILY_0700_2200', 6, TIME '07:00', TIME '22:00', FALSE),
        ('DAILY_0700_2200', 7, TIME '07:00', TIME '22:00', FALSE),
        ('DAILY_0900_1800', 1, TIME '09:00', TIME '18:00', FALSE),
        ('DAILY_0900_1800', 2, TIME '09:00', TIME '18:00', FALSE),
        ('DAILY_0900_1800', 3, TIME '09:00', TIME '18:00', FALSE),
        ('DAILY_0900_1800', 4, TIME '09:00', TIME '18:00', FALSE),
        ('DAILY_0900_1800', 5, TIME '09:00', TIME '18:00', FALSE),
        ('DAILY_0900_1800', 6, TIME '09:00', TIME '18:00', FALSE),
        ('DAILY_0900_1800', 7, TIME '09:00', TIME '18:00', FALSE),
        ('DAILY_0830_1700', 1, TIME '08:30', TIME '17:00', FALSE),
        ('DAILY_0830_1700', 2, TIME '08:30', TIME '17:00', FALSE),
        ('DAILY_0830_1700', 3, TIME '08:30', TIME '17:00', FALSE),
        ('DAILY_0830_1700', 4, TIME '08:30', TIME '17:00', FALSE),
        ('DAILY_0830_1700', 5, TIME '08:30', TIME '17:00', FALSE),
        ('DAILY_0830_1700', 6, TIME '08:30', TIME '17:00', FALSE),
        ('DAILY_0830_1700', 7, TIME '08:30', TIME '17:00', FALSE),
        ('ZOO_DAILY', 1, TIME '07:00', TIME '17:30', FALSE),
        ('ZOO_DAILY', 2, TIME '07:00', TIME '17:30', FALSE),
        ('ZOO_DAILY', 3, TIME '07:00', TIME '17:30', FALSE),
        ('ZOO_DAILY', 4, TIME '07:00', TIME '17:30', FALSE),
        ('ZOO_DAILY', 5, TIME '07:00', TIME '17:30', FALSE),
        ('ZOO_DAILY', 6, TIME '07:00', TIME '17:30', FALSE),
        ('ZOO_DAILY', 7, TIME '07:00', TIME '17:30', FALSE),
        ('MARKET_DAILY', 1, TIME '06:00', TIME '18:00', FALSE),
        ('MARKET_DAILY', 2, TIME '06:00', TIME '18:00', FALSE),
        ('MARKET_DAILY', 3, TIME '06:00', TIME '18:00', FALSE),
        ('MARKET_DAILY', 4, TIME '06:00', TIME '18:00', FALSE),
        ('MARKET_DAILY', 5, TIME '06:00', TIME '18:00', FALSE),
        ('MARKET_DAILY', 6, TIME '06:00', TIME '18:00', FALSE),
        ('MARKET_DAILY', 7, TIME '06:00', TIME '18:00', FALSE),
        ('MARKET_LONG', 1, TIME '06:00', TIME '20:00', FALSE),
        ('MARKET_LONG', 2, TIME '06:00', TIME '20:00', FALSE),
        ('MARKET_LONG', 3, TIME '06:00', TIME '20:00', FALSE),
        ('MARKET_LONG', 4, TIME '06:00', TIME '20:00', FALSE),
        ('MARKET_LONG', 5, TIME '06:00', TIME '20:00', FALSE),
        ('MARKET_LONG', 6, TIME '06:00', TIME '20:00', FALSE),
        ('MARKET_LONG', 7, TIME '06:00', TIME '20:00', FALSE),
        ('MUSEUM_TUE_SUN', 1, CAST(NULL AS TIME), CAST(NULL AS TIME), TRUE),
        ('MUSEUM_TUE_SUN', 2, TIME '08:00', TIME '17:00', FALSE),
        ('MUSEUM_TUE_SUN', 3, TIME '08:00', TIME '17:00', FALSE),
        ('MUSEUM_TUE_SUN', 4, TIME '08:00', TIME '17:00', FALSE),
        ('MUSEUM_TUE_SUN', 5, TIME '08:00', TIME '17:00', FALSE),
        ('MUSEUM_TUE_SUN', 6, TIME '08:00', TIME '17:00', FALSE),
        ('MUSEUM_TUE_SUN', 7, TIME '08:00', TIME '17:00', FALSE),
        ('OUTDOOR_LONG', 1, TIME '05:00', TIME '23:00', FALSE),
        ('OUTDOOR_LONG', 2, TIME '05:00', TIME '23:00', FALSE),
        ('OUTDOOR_LONG', 3, TIME '05:00', TIME '23:00', FALSE),
        ('OUTDOOR_LONG', 4, TIME '05:00', TIME '23:00', FALSE),
        ('OUTDOOR_LONG', 5, TIME '05:00', TIME '23:00', FALSE),
        ('OUTDOOR_LONG', 6, TIME '05:00', TIME '23:00', FALSE),
        ('OUTDOOR_LONG', 7, TIME '05:00', TIME '23:00', FALSE),
        ('MALL_DAILY', 1, TIME '10:00', TIME '22:00', FALSE),
        ('MALL_DAILY', 2, TIME '10:00', TIME '22:00', FALSE),
        ('MALL_DAILY', 3, TIME '10:00', TIME '22:00', FALSE),
        ('MALL_DAILY', 4, TIME '10:00', TIME '22:00', FALSE),
        ('MALL_DAILY', 5, TIME '10:00', TIME '22:00', FALSE),
        ('MALL_DAILY', 6, TIME '10:00', TIME '22:00', FALSE),
        ('MALL_DAILY', 7, TIME '10:00', TIME '22:00', FALSE),
        ('SKYDECK_DAILY', 1, TIME '09:30', TIME '21:30', FALSE),
        ('SKYDECK_DAILY', 2, TIME '09:30', TIME '21:30', FALSE),
        ('SKYDECK_DAILY', 3, TIME '09:30', TIME '21:30', FALSE),
        ('SKYDECK_DAILY', 4, TIME '09:30', TIME '21:30', FALSE),
        ('SKYDECK_DAILY', 5, TIME '09:30', TIME '21:30', FALSE),
        ('SKYDECK_DAILY', 6, TIME '09:30', TIME '21:30', FALSE),
        ('SKYDECK_DAILY', 7, TIME '09:30', TIME '21:30', FALSE),
        ('BOOKSTREET_DAILY', 1, TIME '08:00', TIME '22:00', FALSE),
        ('BOOKSTREET_DAILY', 2, TIME '08:00', TIME '22:00', FALSE),
        ('BOOKSTREET_DAILY', 3, TIME '08:00', TIME '22:00', FALSE),
        ('BOOKSTREET_DAILY', 4, TIME '08:00', TIME '22:00', FALSE),
        ('BOOKSTREET_DAILY', 5, TIME '08:00', TIME '22:00', FALSE),
        ('BOOKSTREET_DAILY', 6, TIME '08:00', TIME '22:00', FALSE),
        ('BOOKSTREET_DAILY', 7, TIME '08:00', TIME '22:00', FALSE),
        ('RELIGIOUS_DAILY', 1, TIME '06:00', TIME '18:00', FALSE),
        ('RELIGIOUS_DAILY', 2, TIME '06:00', TIME '18:00', FALSE),
        ('RELIGIOUS_DAILY', 3, TIME '06:00', TIME '18:00', FALSE),
        ('RELIGIOUS_DAILY', 4, TIME '06:00', TIME '18:00', FALSE),
        ('RELIGIOUS_DAILY', 5, TIME '06:00', TIME '18:00', FALSE),
        ('RELIGIOUS_DAILY', 6, TIME '06:00', TIME '18:00', FALSE),
        ('RELIGIOUS_DAILY', 7, TIME '06:00', TIME '18:00', FALSE),
        ('NIGHT_DAILY', 1, TIME '17:00', TIME '23:59', FALSE),
        ('NIGHT_DAILY', 2, TIME '17:00', TIME '23:59', FALSE),
        ('NIGHT_DAILY', 3, TIME '17:00', TIME '23:59', FALSE),
        ('NIGHT_DAILY', 4, TIME '17:00', TIME '23:59', FALSE),
        ('NIGHT_DAILY', 5, TIME '17:00', TIME '23:59', FALSE),
        ('NIGHT_DAILY', 6, TIME '17:00', TIME '23:59', FALSE),
        ('NIGHT_DAILY', 7, TIME '17:00', TIME '23:59', FALSE),
        ('ATTRACTION_DAILY', 1, TIME '08:00', TIME '18:00', FALSE),
        ('ATTRACTION_DAILY', 2, TIME '08:00', TIME '18:00', FALSE),
        ('ATTRACTION_DAILY', 3, TIME '08:00', TIME '18:00', FALSE),
        ('ATTRACTION_DAILY', 4, TIME '08:00', TIME '18:00', FALSE),
        ('ATTRACTION_DAILY', 5, TIME '08:00', TIME '18:00', FALSE),
        ('ATTRACTION_DAILY', 6, TIME '08:00', TIME '18:00', FALSE),
        ('ATTRACTION_DAILY', 7, TIME '08:00', TIME '18:00', FALSE),
        ('EVENING_OUTDOOR', 1, TIME '16:00', TIME '22:00', FALSE),
        ('EVENING_OUTDOOR', 2, TIME '16:00', TIME '22:00', FALSE),
        ('EVENING_OUTDOOR', 3, TIME '16:00', TIME '22:00', FALSE),
        ('EVENING_OUTDOOR', 4, TIME '16:00', TIME '22:00', FALSE),
        ('EVENING_OUTDOOR', 5, TIME '16:00', TIME '22:00', FALSE),
        ('EVENING_OUTDOOR', 6, TIME '16:00', TIME '22:00', FALSE),
        ('EVENING_OUTDOOR', 7, TIME '16:00', TIME '22:00', FALSE),
        ('LEISURE_DAILY', 1, TIME '07:00', TIME '22:00', FALSE),
        ('LEISURE_DAILY', 2, TIME '07:00', TIME '22:00', FALSE),
        ('LEISURE_DAILY', 3, TIME '07:00', TIME '22:00', FALSE),
        ('LEISURE_DAILY', 4, TIME '07:00', TIME '22:00', FALSE),
        ('LEISURE_DAILY', 5, TIME '07:00', TIME '22:00', FALSE),
        ('LEISURE_DAILY', 6, TIME '07:00', TIME '22:00', FALSE),
        ('LEISURE_DAILY', 7, TIME '07:00', TIME '22:00', FALSE)
),
place_schedule(place_slug, profile) AS (
    VALUES
        ('dinh-doc-lap', 'DAILY_0800_1700'),
        ('bao-tang-my-thuat-tphcm', 'DAILY_0800_1700'),
        ('buu-dien-trung-tam-sai-gon', 'DAILY_0700_1900'),
        ('thao-cam-vien-sai-gon', 'ZOO_DAILY'),
        ('cho-ben-thanh', 'MARKET_DAILY'),
        ('bao-tang-chung-tich-chien-tranh', 'DAILY_0730_1730'),
        ('bao-tang-lich-su-tphcm', 'MUSEUM_TUE_SUN'),
        ('bao-tang-thanh-pho-ho-chi-minh', 'DAILY_0800_1700'),
        ('bao-tang-phu-nu-nam-bo', 'MUSEUM_TUE_SUN'),
        ('bao-tang-ton-duc-thang', 'MUSEUM_TUE_SUN'),
        ('ben-nha-rong-bao-tang-ho-chi-minh', 'MUSEUM_TUE_SUN'),
        ('nha-tho-duc-ba-sai-gon', 'DAILY_0600_2000'),
        ('nha-hat-thanh-pho-ho-chi-minh', 'DAILY_0700_2200'),
        ('pho-di-bo-nguyen-hue', 'OUTDOOR_LONG'),
        ('cong-vien-ben-bach-dang', 'OUTDOOR_LONG'),
        ('ben-bach-dang', 'OUTDOOR_LONG'),
        ('landmark-81', 'MALL_DAILY'),
        ('saigon-skydeck-bitexco', 'SKYDECK_DAILY'),
        ('duong-sach-nguyen-van-binh', 'BOOKSTREET_DAILY'),
        ('cong-vien-tao-dan', 'OUTDOOR_LONG'),
        ('ho-con-rua', 'OUTDOOR_LONG'),
        ('chua-ngoc-hoang', 'RELIGIOUS_DAILY'),
        ('chua-vinh-nghiem', 'RELIGIOUS_DAILY'),
        ('nha-tho-tan-dinh', 'DAILY_0600_2000'),
        ('chua-giac-lam', 'RELIGIOUS_DAILY'),
        ('chua-ba-thien-hau', 'RELIGIOUS_DAILY'),
        ('nha-tho-cha-tam', 'DAILY_0600_1900'),
        ('lang-ong-ba-chieu', 'RELIGIOUS_DAILY'),
        ('cho-binh-tay', 'MARKET_DAILY'),
        ('cho-ho-thi-ky', 'MARKET_LONG'),
        ('cho-tan-dinh', 'MARKET_DAILY'),
        ('pho-di-bo-bui-vien', 'NIGHT_DAILY'),
        ('cong-vien-gia-dinh', 'OUTDOOR_LONG'),
        ('cong-vien-van-hoa-dam-sen', 'ATTRACTION_DAILY'),
        ('khu-du-lich-suoi-tien', 'ATTRACTION_DAILY'),
        ('cau-anh-sao', 'EVENING_OUTDOOR'),
        ('ho-ban-nguyet', 'OUTDOOR_LONG'),
        ('khu-du-lich-binh-quoi-1', 'LEISURE_DAILY'),
        ('khu-du-lich-van-thanh', 'LEISURE_DAILY'),
        ('bao-tang-ao-dai', 'MUSEUM_TUE_SUN'),
        ('artinus-3d-art-museum', 'DAILY_0900_1800'),
        ('bao-tang-y-hoc-co-truyen-viet-nam', 'DAILY_0830_1700')
)
INSERT INTO opening_hours (
    place_id,
    day_of_week,
    open_time,
    close_time,
    closed
)
SELECT
    p.id,
    sp.day_of_week,
    sp.open_time,
    sp.close_time,
    sp.closed
FROM place_schedule ps
JOIN places p
    ON p.slug = ps.place_slug
JOIN schedule_profiles sp
    ON sp.profile = ps.profile
ORDER BY p.id, sp.day_of_week;

COMMIT;

-- Optional diagnostics after execution.
SELECT COUNT(*) AS active_curated_places
FROM places
WHERE active = TRUE
  AND slug IN (
      'dinh-doc-lap',
      'bao-tang-my-thuat-tphcm',
      'buu-dien-trung-tam-sai-gon',
      'thao-cam-vien-sai-gon',
      'cho-ben-thanh',
      'bao-tang-chung-tich-chien-tranh',
      'bao-tang-lich-su-tphcm',
      'bao-tang-thanh-pho-ho-chi-minh',
      'bao-tang-phu-nu-nam-bo',
      'bao-tang-ton-duc-thang',
      'ben-nha-rong-bao-tang-ho-chi-minh',
      'nha-tho-duc-ba-sai-gon',
      'nha-hat-thanh-pho-ho-chi-minh',
      'pho-di-bo-nguyen-hue',
      'cong-vien-ben-bach-dang',
      'ben-bach-dang',
      'landmark-81',
      'saigon-skydeck-bitexco',
      'duong-sach-nguyen-van-binh',
      'cong-vien-tao-dan',
      'ho-con-rua',
      'chua-ngoc-hoang',
      'chua-vinh-nghiem',
      'nha-tho-tan-dinh',
      'chua-giac-lam',
      'chua-ba-thien-hau',
      'nha-tho-cha-tam',
      'lang-ong-ba-chieu',
      'cho-binh-tay',
      'cho-ho-thi-ky',
      'cho-tan-dinh',
      'pho-di-bo-bui-vien',
      'cong-vien-gia-dinh',
      'cong-vien-van-hoa-dam-sen',
      'khu-du-lich-suoi-tien',
      'cau-anh-sao',
      'ho-ban-nguyet',
      'khu-du-lich-binh-quoi-1',
      'khu-du-lich-van-thanh',
      'bao-tang-ao-dai',
      'artinus-3d-art-museum',
      'bao-tang-y-hoc-co-truyen-viet-nam'
  );

SELECT COUNT(*) AS curated_opening_hour_rows
FROM opening_hours oh
JOIN places p ON p.id = oh.place_id
WHERE p.slug IN (
      'dinh-doc-lap',
      'bao-tang-my-thuat-tphcm',
      'buu-dien-trung-tam-sai-gon',
      'thao-cam-vien-sai-gon',
      'cho-ben-thanh',
      'bao-tang-chung-tich-chien-tranh',
      'bao-tang-lich-su-tphcm',
      'bao-tang-thanh-pho-ho-chi-minh',
      'bao-tang-phu-nu-nam-bo',
      'bao-tang-ton-duc-thang',
      'ben-nha-rong-bao-tang-ho-chi-minh',
      'nha-tho-duc-ba-sai-gon',
      'nha-hat-thanh-pho-ho-chi-minh',
      'pho-di-bo-nguyen-hue',
      'cong-vien-ben-bach-dang',
      'ben-bach-dang',
      'landmark-81',
      'saigon-skydeck-bitexco',
      'duong-sach-nguyen-van-binh',
      'cong-vien-tao-dan',
      'ho-con-rua',
      'chua-ngoc-hoang',
      'chua-vinh-nghiem',
      'nha-tho-tan-dinh',
      'chua-giac-lam',
      'chua-ba-thien-hau',
      'nha-tho-cha-tam',
      'lang-ong-ba-chieu',
      'cho-binh-tay',
      'cho-ho-thi-ky',
      'cho-tan-dinh',
      'pho-di-bo-bui-vien',
      'cong-vien-gia-dinh',
      'cong-vien-van-hoa-dam-sen',
      'khu-du-lich-suoi-tien',
      'cau-anh-sao',
      'ho-ban-nguyet',
      'khu-du-lich-binh-quoi-1',
      'khu-du-lich-van-thanh',
      'bao-tang-ao-dai',
      'artinus-3d-art-museum',
      'bao-tang-y-hoc-co-truyen-viet-nam'
  );

SELECT COUNT(*) AS curated_place_category_rows
FROM place_categories pc
JOIN places p ON p.id = pc.place_id
WHERE p.slug IN (
      'dinh-doc-lap',
      'bao-tang-my-thuat-tphcm',
      'buu-dien-trung-tam-sai-gon',
      'thao-cam-vien-sai-gon',
      'cho-ben-thanh',
      'bao-tang-chung-tich-chien-tranh',
      'bao-tang-lich-su-tphcm',
      'bao-tang-thanh-pho-ho-chi-minh',
      'bao-tang-phu-nu-nam-bo',
      'bao-tang-ton-duc-thang',
      'ben-nha-rong-bao-tang-ho-chi-minh',
      'nha-tho-duc-ba-sai-gon',
      'nha-hat-thanh-pho-ho-chi-minh',
      'pho-di-bo-nguyen-hue',
      'cong-vien-ben-bach-dang',
      'ben-bach-dang',
      'landmark-81',
      'saigon-skydeck-bitexco',
      'duong-sach-nguyen-van-binh',
      'cong-vien-tao-dan',
      'ho-con-rua',
      'chua-ngoc-hoang',
      'chua-vinh-nghiem',
      'nha-tho-tan-dinh',
      'chua-giac-lam',
      'chua-ba-thien-hau',
      'nha-tho-cha-tam',
      'lang-ong-ba-chieu',
      'cho-binh-tay',
      'cho-ho-thi-ky',
      'cho-tan-dinh',
      'pho-di-bo-bui-vien',
      'cong-vien-gia-dinh',
      'cong-vien-van-hoa-dam-sen',
      'khu-du-lich-suoi-tien',
      'cau-anh-sao',
      'ho-ban-nguyet',
      'khu-du-lich-binh-quoi-1',
      'khu-du-lich-van-thanh',
      'bao-tang-ao-dai',
      'artinus-3d-art-museum',
      'bao-tang-y-hoc-co-truyen-viet-nam'
  );
