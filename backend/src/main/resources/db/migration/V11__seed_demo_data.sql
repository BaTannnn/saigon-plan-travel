-- DEMO DATA ONLY - NOT VERIFIED REAL-WORLD DATA.
-- This seed exists for development, UI work and automated testing.

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
) VALUES
    (
        'Demo Art Space',
        'demo-art-space',
        'Dữ liệu minh họa, chưa được xác minh',
        'Địa điểm mô phỏng chỉ phục vụ phát triển và kiểm thử.',
        'Địa chỉ demo 1',
        10.7750000,
        106.7000000,
        90,
        50000,
        150000,
        TRUE,
        TRUE
    ),
    (
        'Demo City Garden',
        'demo-city-garden',
        'Dữ liệu minh họa, chưa được xác minh',
        'Địa điểm mô phỏng chỉ phục vụ phát triển và kiểm thử.',
        'Địa chỉ demo 2',
        10.7800000,
        106.6900000,
        60,
        0,
        50000,
        FALSE,
        TRUE
    ),
    (
        'Demo History Hall',
        'demo-history-hall',
        NULL,
        'Dữ liệu minh họa, chưa được xác minh; chỉ phục vụ phát triển và kiểm thử.',
        'Địa chỉ demo 3',
        10.7700000,
        106.6950000,
        120,
        30000,
        100000,
        TRUE,
        TRUE
    ),
    (
        'Demo Riverside Walk',
        'demo-riverside-walk',
        'Dữ liệu minh họa, chưa được xác minh',
        'Địa điểm mô phỏng chỉ phục vụ phát triển và kiểm thử.',
        'Địa chỉ demo 4',
        10.7900000,
        106.7100000,
        75,
        0,
        80000,
        FALSE,
        TRUE
    ),
    (
        'Demo Science Center',
        'demo-science-center',
        'Dữ liệu minh họa, chưa được xác minh',
        'Địa điểm mô phỏng chỉ phục vụ phát triển và kiểm thử.',
        'Địa chỉ demo 5',
        10.7550000,
        106.6650000,
        100,
        40000,
        120000,
        TRUE,
        TRUE
    ),
    (
        'Demo Temporarily Hidden Place',
        'demo-temporarily-hidden-place',
        'Dữ liệu minh họa, chưa được xác minh',
        'Địa điểm mô phỏng inactive chỉ phục vụ kiểm thử.',
        'Địa chỉ demo 6',
        10.7700000,
        106.6700000,
        45,
        0,
        30000,
        TRUE,
        FALSE
    );

INSERT INTO categories (name, slug, description)
VALUES
    ('Văn hóa', 'van-hoa', 'Danh mục demo, chưa được xác minh.'),
    ('Lịch sử', 'lich-su', 'Danh mục demo, chưa được xác minh.'),
    ('Nghệ thuật', 'nghe-thuat', 'Danh mục demo, chưa được xác minh.'),
    ('Khoa học', 'khoa-hoc', 'Danh mục demo, chưa được xác minh.'),
    ('Ngoài trời', 'ngoai-troi', 'Danh mục demo, chưa được xác minh.');

INSERT INTO place_categories (place_id, category_id)
SELECT place.id, category.id
FROM (
    VALUES
        ('demo-art-space', 'nghe-thuat'),
        ('demo-art-space', 'van-hoa'),
        ('demo-city-garden', 'ngoai-troi'),
        ('demo-history-hall', 'lich-su'),
        ('demo-history-hall', 'van-hoa'),
        ('demo-riverside-walk', 'ngoai-troi'),
        ('demo-science-center', 'khoa-hoc'),
        ('demo-temporarily-hidden-place', 'van-hoa')
) AS mapping(place_slug, category_slug)
JOIN places place ON place.slug = mapping.place_slug
JOIN categories category ON category.slug = mapping.category_slug;

INSERT INTO opening_hours (
    place_id,
    day_of_week,
    open_time,
    close_time,
    closed
)
SELECT
    place.id,
    schedule.day_of_week,
    schedule.open_time,
    schedule.close_time,
    schedule.closed
FROM (
    VALUES
        ('demo-art-space', 1, TIME '09:00', TIME '17:00', FALSE),
        ('demo-art-space', 2, CAST(NULL AS TIME), CAST(NULL AS TIME), TRUE),
        ('demo-art-space', 3, TIME '09:00', TIME '17:00', FALSE),
        ('demo-art-space', 4, TIME '09:00', TIME '17:00', FALSE),
        ('demo-art-space', 5, TIME '09:00', TIME '17:00', FALSE),
        ('demo-art-space', 6, TIME '09:00', TIME '17:00', FALSE),
        ('demo-art-space', 7, TIME '09:00', TIME '17:00', FALSE),

        ('demo-city-garden', 1, TIME '06:00', TIME '18:00', FALSE),
        ('demo-city-garden', 2, TIME '06:00', TIME '18:00', FALSE),
        ('demo-city-garden', 3, TIME '06:00', TIME '18:00', FALSE),
        ('demo-city-garden', 4, TIME '06:00', TIME '18:00', FALSE),
        ('demo-city-garden', 5, TIME '06:00', TIME '18:00', FALSE),
        ('demo-city-garden', 6, TIME '06:00', TIME '18:00', FALSE),

        ('demo-history-hall', 2, TIME '08:30', TIME '16:30', FALSE),
        ('demo-history-hall', 3, TIME '08:30', TIME '16:30', FALSE),
        ('demo-history-hall', 4, TIME '08:30', TIME '16:30', FALSE),
        ('demo-history-hall', 5, TIME '08:30', TIME '16:30', FALSE),
        ('demo-history-hall', 6, TIME '08:30', TIME '16:30', FALSE),
        ('demo-history-hall', 7, TIME '08:30', TIME '16:30', FALSE),

        ('demo-riverside-walk', 1, TIME '06:00', TIME '21:00', FALSE),
        ('demo-riverside-walk', 2, TIME '06:00', TIME '21:00', FALSE),
        ('demo-riverside-walk', 3, TIME '06:00', TIME '21:00', FALSE),
        ('demo-riverside-walk', 4, TIME '06:00', TIME '21:00', FALSE),
        ('demo-riverside-walk', 5, TIME '06:00', TIME '21:00', FALSE),
        ('demo-riverside-walk', 6, TIME '06:00', TIME '21:00', FALSE),
        ('demo-riverside-walk', 7, TIME '06:00', TIME '21:00', FALSE),

        ('demo-science-center', 1, TIME '08:00', TIME '17:00', FALSE),
        ('demo-science-center', 2, TIME '08:00', TIME '17:00', FALSE),
        ('demo-science-center', 3, TIME '08:00', TIME '17:00', FALSE),
        ('demo-science-center', 4, TIME '08:00', TIME '17:00', FALSE),
        ('demo-science-center', 5, TIME '08:00', TIME '17:00', FALSE),
        ('demo-science-center', 6, TIME '08:00', TIME '12:00', FALSE),
        ('demo-science-center', 7, CAST(NULL AS TIME), CAST(NULL AS TIME), TRUE)
) AS schedule(place_slug, day_of_week, open_time, close_time, closed)
JOIN places place ON place.slug = schedule.place_slug;
