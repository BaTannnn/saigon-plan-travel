package com.saigonplantravel.backend.scheduling.domain;

public enum ItineraryWarningCode {

    TRAVEL_TIME_ESTIMATED(
            "Thời gian di chuyển đang dùng ước tính đường thẳng, chưa phải dữ liệu tuyến đường thực tế."
    ),
    COST_USES_MINIMUM_ESTIMATE(
            "Chi phí lịch trình được ước tính từ mức chi phí tối thiểu của từng địa điểm."
    ),
    UNUSED_TIME_REMAINS(
            "Lịch trình còn thời gian nhưng các địa điểm còn lại không thể xếp theo các ràng buộc hiện tại."
    ),
    LIMITED_CATEGORY_COVERAGE(
            "Lịch trình chưa bao phủ tất cả danh mục ưu tiên."
    ),
    OPENING_HOURS_UNKNOWN(
            "Chưa có dữ liệu giờ mở cửa cho địa điểm này."
    );

    private final String message;

    ItineraryWarningCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
