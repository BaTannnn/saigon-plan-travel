import { ApiError } from "@/lib/api/api-client";

export type TripFormFeedback = {
  message: string;
  fieldErrors: Record<string, string>;
};

function normalizeFieldName(field: string) {
  if (field.startsWith("startLocation.")) return field;
  return field;
}

export function getTripFormFeedback(error: unknown): TripFormFeedback {
  if (!(error instanceof ApiError)) {
    return {
      message: "Không thể lưu chuyến đi. Hãy thử lại.",
      fieldErrors: {},
    };
  }

  if (error.status === 0) {
    return {
      message:
        "Không thể kết nối đến backend. Hãy kiểm tra kết nối và thử lại.",
      fieldErrors: {},
    };
  }

  const fieldErrors = Object.fromEntries(
    (error.problem?.fieldErrors ?? []).map((item) => [
      normalizeFieldName(item.field),
      item.message,
    ]),
  );

  return {
    message:
      Object.keys(fieldErrors).length > 0
        ? "Hãy kiểm tra lại các trường được đánh dấu."
        : (error.problem?.detail ?? "Không thể lưu chuyến đi. Hãy thử lại."),
    fieldErrors,
  };
}

export function getTripLoadErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) {
    return "Không thể tải chuyến đi. Hãy thử lại.";
  }
  if (error.status === 0) {
    return "Không thể kết nối đến backend. Hãy kiểm tra kết nối và thử lại.";
  }
  if (error.status === 404) {
    return "Không tìm thấy chuyến đi này hoặc bạn không có quyền truy cập.";
  }
  if (error.status === 400) {
    return "Mã chuyến đi trong đường dẫn không hợp lệ.";
  }
  return error.problem?.detail ?? "Không thể tải chuyến đi. Hãy thử lại.";
}

export function getTripsLoadErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) {
    return "Không thể tải danh sách chuyến đi. Hãy thử lại.";
  }
  if (error.status === 0) {
    return "Không thể kết nối đến backend. Hãy kiểm tra kết nối và thử lại.";
  }
  return (
    error.problem?.detail ?? "Không thể tải danh sách chuyến đi. Hãy thử lại."
  );
}

export function getTripDeleteErrorMessage(error: unknown) {
  if (!(error instanceof ApiError)) {
    return "Không thể xóa chuyến đi. Hãy thử lại.";
  }
  if (error.status === 0) {
    return "Không thể kết nối đến backend. Hãy kiểm tra kết nối và thử lại.";
  }
  if (error.status === 404) {
    return "Không tìm thấy chuyến đi này hoặc bạn không có quyền xóa.";
  }
  return error.problem?.detail ?? "Không thể xóa chuyến đi. Hãy thử lại.";
}
