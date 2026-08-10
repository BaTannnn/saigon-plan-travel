# Domain Glossary

| Thuật ngữ | Nghĩa trong dự án | Ghi chú |
| --- | --- | --- |
| Place | Địa điểm hoặc trải nghiệm du lịch có thể được khám phá | Tương ứng entity `Place` |
| Category | Nhóm chủ đề công khai gắn với Place và sở thích Trip | Dùng slug ở request công khai |
| Opening Hour | Khoảng mở cửa hằng tuần của một Place | Thiếu dữ liệu có nghĩa là chưa biết |
| User | Tài khoản đã đăng ký và xác thực bằng JWT | Sở hữu Trip |
| Trip | Bản ghi sở thích chuyến đi một ngày của người dùng | Không chứa danh sách điểm dừng hoặc timeline |
| Travel Pace | Nhịp độ người dùng lưu cho Trip | `RELAXED`, `BALANCED`, `FAST` |
| Environment Preference | Sở thích không gian của Trip | `INDOOR`, `OUTDOOR`, `MIXED` |
| Public ID | UUID dùng trong API Trip | Không lộ khóa số nội bộ |
| Demo Data | Dữ liệu phục vụ phát triển chưa được xác minh đầy đủ | Không trình bày như dữ kiện thực tế đã kiểm chứng |
