// UI-only fixtures, never an API response or evidence of a real Enrollment.
export type PreviewCourse = {
  id: string;
  title: string;
  teacher: string;
  category: string;
  sessionCount: number;
  tone: "indigo" | "cyan";
};
export type PreviewSession = {
  id: string;
  courseId: string;
  title: string;
  startsAt: string;
  endsAt: string;
  status: "SCHEDULED" | "CANCELLED";
};
export const dashboardTimeZone = "Asia/Ho_Chi_Minh";
const dayFormat = new Intl.DateTimeFormat("en-CA", {
  timeZone: dashboardTimeZone, year: "numeric", month: "2-digit", day: "2-digit",
});
export function dayKey(instant: Date | string) {
  const parts = dayFormat.formatToParts(new Date(instant));
  return ["year", "month", "day"].map(type => parts.find(part => part.type === type)!.value).join("-");
}
export function shiftDay(day: string, offset: number) {
  const date = new Date(day + "T12:00:00Z");
  date.setUTCDate(date.getUTCDate() + offset);
  return date.toISOString().slice(0, 10);
}
export function monthDays(month: string) {
  const first = new Date(month + "-01T12:00:00Z");
  const leading = (first.getUTCDay() + 6) % 7;
  const count = new Date(Date.UTC(first.getUTCFullYear(), first.getUTCMonth() + 1, 0)).getUTCDate();
  return Array.from({ length: Math.ceil((leading + count) / 7) * 7 }, (_, index) =>
    shiftDay(month + "-01", index - leading));
}
export function shiftMonth(month: string, offset: number) {
  const date = new Date(month + "-01T12:00:00Z");
  date.setUTCMonth(date.getUTCMonth() + offset);
  return date.toISOString().slice(0, 7);
}
export const previewCourses: PreviewCourse[] = [
  { id: "demo-conversation", title: "Tiếng Anh giao tiếp hằng ngày", teacher: "Gia sư minh họa A", category: "Giao tiếp", sessionCount: 12, tone: "indigo" },
  { id: "demo-work", title: "Tiếng Anh trong công việc", teacher: "Gia sư minh họa B", category: "Công việc", sessionCount: 10, tone: "cyan" },
];
export function previewSessions(today: string): PreviewSession[] {
  return [
    { offset: 0, courseId: "demo-conversation", title: "Giới thiệu bản thân", hour: "19:00", end: "20:30", status: "SCHEDULED" },
    { offset: 2, courseId: "demo-work", title: "Trao đổi trong cuộc họp", hour: "18:30", end: "20:00", status: "SCHEDULED" },
    { offset: 2, courseId: "demo-conversation", title: "Hội thoại theo tình huống", hour: "20:15", end: "21:15", status: "SCHEDULED" },
    { offset: 5, courseId: "demo-conversation", title: "Thực hành cùng gia sư", hour: "19:00", end: "20:30", status: "CANCELLED" },
    { offset: 7, courseId: "demo-work", title: "Trình bày ý tưởng", hour: "18:30", end: "20:00", status: "SCHEDULED" },
  ].map((entry, index) => ({
    id: "demo-session-" + index, courseId: entry.courseId, title: entry.title,
    startsAt: shiftDay(today, entry.offset) + "T" + entry.hour + ":00+07:00",
    endsAt: shiftDay(today, entry.offset) + "T" + entry.end + ":00+07:00",
    status: entry.status as PreviewSession["status"],
  }));
}
