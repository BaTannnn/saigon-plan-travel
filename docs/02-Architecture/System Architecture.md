# Kiến trúc tổng thể hệ thống

![[assets/architecture/system-architecture.png]]

## Tổng quan

Hệ thống SaigonPlanTravel được xây dựng theo kiến trúc client-server,
kết hợp Spring Boot modular monolith và Python AI Service.

## Các thành phần

- [[Frontend - NextJS]]
- [[Backend - Spring Boot]]
- [[AI Service - FastAPI]]
- [[Database - PostgreSQL]]
- [[External Services]]

## Luồng xử lý

1. Người dùng nhập yêu cầu chuyến đi.
2. Frontend gửi yêu cầu đến Spring Boot.
3. Backend xử lý nghiệp vụ lập lịch.
4. AI Service hỗ trợ RAG và phân tích ngữ cảnh.
5. Hệ thống trả về lịch trình, bản đồ và giải thích.