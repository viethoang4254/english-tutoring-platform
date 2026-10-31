import { ApiRequestError } from "../../lib/api";
export type Enrollment = {
  id: string; courseId: string; courseTitle: string;
  status: "PENDING" | "ACTIVE" | "COMPLETED" | "CANCELLED";
  enrolledAt: string | null; seatReservedUntil: string | null; reservationExpired: boolean;
  cancelledAt: string | null; cancellationReason: string | null; createdAt: string; updatedAt: string;
};
export type EnrollmentPage = { items: Enrollment[]; page: number; size: number; hasNext: boolean };
export const statusLabel = { PENDING: "Chờ thanh toán", ACTIVE: "Đang tham gia", COMPLETED: "Đã hoàn thành", CANCELLED: "Đã hủy" };
export function remainingSeconds(row: Enrollment, now: number) {
  if (row.status !== "PENDING" || row.reservationExpired || !row.seatReservedUntil) return 0;
  return Math.max(0, Math.ceil((Date.parse(row.seatReservedUntil) - now) / 1000));
}
export function enrollmentError(error: unknown) {
  if (!(error instanceof ApiRequestError)) return "Chưa xác nhận được kết quả. Hãy tải lại dữ liệu trước khi tiếp tục.";
  const messages: Record<string, string> = {
    COURSE_FULL: "Khóa học đã hết chỗ. Không thể gia hạn giữ chỗ.",
    COURSE_NOT_ENROLLABLE: "Khóa học không còn nhận đăng ký hoặc đã bắt đầu.",
    ENROLLMENT_RENEWAL_REQUIRED: "Giữ chỗ đã hết hạn. Bạn cần chủ động gia hạn.",
    ENROLLMENT_STATE_CONFLICT: "Trạng thái đăng ký không cho phép thao tác này.",
    PAYMENT_COORDINATION_REQUIRED: "Đăng ký có giao dịch đang chờ xử lý. Chưa thể gia hạn hoặc hủy.",
  };
  return messages[error.code] ?? (error.status === 401 ? "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."
    : error.status === 403 ? "Bạn không có quyền truy cập đăng ký này."
    : error.status === 404 ? "Không tìm thấy đăng ký mà bạn được phép truy cập."
    : "Không thể tải hoặc cập nhật đăng ký. Vui lòng tải lại dữ liệu.");
}
