-- DEMO DATA - NOT VERIFIED.
-- These categories and weekly opening hours exist only for development/testing.
-- They must not be presented as verified real-world place information.

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
