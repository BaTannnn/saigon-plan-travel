# Domain Glossary — SaigonPlanTravel

> Chỉ thêm hoặc sửa thuật ngữ sau khi đã được xác nhận trong phiên `$grill-with-docs`.

| Thuật ngữ | Định nghĩa chuẩn | Ghi chú |
|---|---|---|
| Place | Một địa điểm du lịch hoặc trải nghiệm có thể được đưa vào lịch trình | Tương ứng entity `Place` |
| Category | Nhãn phân loại dùng chung để mô tả loại trải nghiệm hoặc đặc trưng của Place | Không có trạng thái `active` trong MVP; một Place có thể thuộc nhiều Category |
| Opening Hour | Một trạng thái lịch mở cửa hằng tuần của Place cho đúng một ngày ISO | Tối đa một interval/ngày; `1 = Monday`, `7 = Sunday` |
| Opening Hours Unknown | Không có Opening Hour row cho một ngày, nghĩa là dữ liệu chưa biết/chưa xác minh | Không được suy diễn thành đóng cửa |
| Closed Day | Có Opening Hour row với `closed=true` và hai time bằng `null` | Khác với Opening Hours Unknown |
| Trip Request | Yêu cầu chuyến đi do người dùng nhập | Bao gồm thời gian, ngân sách, vị trí, sở thích |
| Trip Draft | Bản nháp một ngày đã được validate và lưu trước khi sinh itinerary | FEAT-004 dùng public UUID; anonymous MVP không có ownership hoặc PII |
| Travel Pace | Nhịp trải nghiệm người dùng mong muốn | `RELAXED`, `BALANCED` hoặc `FAST`; chưa tự quyết định số điểm dừng trong FEAT-004 |
| Environment Preference | Môi trường trải nghiệm người dùng mong muốn | `INDOOR`, `OUTDOOR` hoặc `MIXED`; chưa tự lọc Place trong FEAT-004 |
| Candidate Place | Địa điểm ứng viên trước khi scheduling | Chưa chắc được đưa vào lịch trình cuối |
| Itinerary | Lịch trình đã được hệ thống tạo | Gồm nhiều itinerary item |
| Itinerary Item | Một điểm dừng hoặc hoạt động trong timeline | Có thời gian bắt đầu/kết thúc |
| Context Data | Dữ liệu bối cảnh động | Thời tiết, giao thông, đông đúc |
| Re-planning | Điều chỉnh lịch trình sau khi bối cảnh thay đổi | Có thể thay, đổi thứ tự hoặc bỏ địa điểm |
| RAG | Retrieval-Augmented Generation | Truy xuất dữ liệu trước khi LLM trả lời |
| Demo Data | Dữ liệu chỉ dùng để phát triển/test, chưa xác minh đầy đủ | Không trình bày như dữ liệu thật |
