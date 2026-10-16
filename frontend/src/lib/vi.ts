export const roleLabels = { STUDENT: "học viên", TEACHER: "gia sư", ADMIN: "quản trị viên" } as const;
export const statusLabels = { PENDING: "Chờ xét duyệt", APPROVED: "Đã phê duyệt", REJECTED: "Bị từ chối" } as const;

// Display in the browser's timezone; keep the API timestamp unchanged.
export function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Không xác định" : date.toLocaleString("vi-VN", {
    year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", timeZoneName: "short",
  });
}
