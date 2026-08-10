# Kiến trúc tổng thể hệ thống

## Tổng quan

SaigonPlanTravel sử dụng kiến trúc client-server gồm frontend Next.js, backend
Spring Boot modular monolith và PostgreSQL.

## Thành phần

- Frontend Next.js hiển thị dữ liệu địa điểm, xác thực và Trip preferences.
- Backend Spring Boot sở hữu nghiệp vụ `auth`, `place` và `trip`.
- PostgreSQL lưu người dùng, địa điểm, danh mục, giờ mở cửa và Trip.
- Leaflet/OpenStreetMap hiển thị địa điểm và tọa độ xuất phát của Trip.

## Luồng chính

1. Người dùng duyệt địa điểm công khai hoặc đăng nhập.
2. Frontend gọi REST API của Spring Boot.
3. Backend xác thực, áp dụng nghiệp vụ và đọc/ghi PostgreSQL.
4. Backend trả DTO; frontend render danh sách, chi tiết, biểu mẫu hoặc bản đồ.

Hệ thống không có module tạo timeline hoặc kế hoạch di chuyển tự động.
