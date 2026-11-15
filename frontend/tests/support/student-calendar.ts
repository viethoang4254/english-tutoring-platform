import { expect, type Page } from "@playwright/test";
import type { StudentCalendarSession } from "../../src/features/student/student-calendar";

// Synthetic API fixtures only; no production enrollment, payment or Meet URL.
export const calendarSessions: StudentCalendarSession[] = [
  { id: "101", courseId: "42", courseTitle: "Tiếng Anh trong công việc", teacherDisplayName: "Gia sư kiểm thử", sessionNumber: 1, title: "Giới thiệu bản thân", startsAt: "2026-10-18T19:00:00+07:00", endsAt: "2026-10-18T20:30:00+07:00", timezone: "Asia/Ho_Chi_Minh", status: "SCHEDULED", rescheduled: false },
  { id: "102", courseId: "42", courseTitle: "Tiếng Anh trong công việc", teacherDisplayName: "Gia sư kiểm thử", sessionNumber: 2, title: "Trao đổi trong cuộc họp", startsAt: "2026-10-20T18:30:00+07:00", endsAt: "2026-10-20T20:00:00+07:00", timezone: "Asia/Ho_Chi_Minh", status: "SCHEDULED", rescheduled: true },
  { id: "103", courseId: "42", courseTitle: "Tiếng Anh trong công việc", teacherDisplayName: "Gia sư kiểm thử", sessionNumber: 3, title: "Hội thoại theo tình huống", startsAt: "2026-10-20T20:15:00+07:00", endsAt: "2026-10-20T21:15:00+07:00", timezone: "Asia/Ho_Chi_Minh", status: "CANCELLED", rescheduled: false },
  { id: "104", courseId: "42", courseTitle: "Tiếng Anh trong công việc", teacherDisplayName: "Gia sư kiểm thử", sessionNumber: 4, title: "Thực hành cùng gia sư", startsAt: "2026-10-23T19:00:00+07:00", endsAt: "2026-10-23T20:30:00+07:00", timezone: "Asia/Ho_Chi_Minh", status: "CANCELLED", rescheduled: false },
];
export function calendarPage(url: URL, sessions = calendarSessions) {
  const from = Date.parse(url.searchParams.get("from")!); const to = Date.parse(url.searchParams.get("to")!);
  const page = Number(url.searchParams.get("page")); const size = Number(url.searchParams.get("size"));
  const rows = sessions.filter(s => Date.parse(s.startsAt) < to && Date.parse(s.endsAt) > from)
    .sort((a,b) => Date.parse(a.startsAt) - Date.parse(b.startsAt) || Number(a.id) - Number(b.id));
  return { items: rows.slice(page * size, (page + 1) * size), page, size, hasNext: rows.length > (page + 1) * size };
}
export async function calendarFixture(page: Page) {
  await page.clock.setFixedTime(new Date("2026-10-18T03:00:00Z"));
  const state = { sessions: [...calendarSessions], status: 200, requests: [] as URL[] };
  await page.route("**/api/v1/**", async route => {
    const request = route.request(); const url = new URL(request.url()); const path = url.pathname;
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "test-csrf" } });
    if (path.endsWith("/login")) return route.fulfill({ json: { accessToken: "test-access", tokenType: "Bearer" } });
    if (path.endsWith("/users/me")) return route.fulfill({ json: { id: "3", role: "STUDENT", emailVerified: false } });
    if (path.endsWith("/users/me/profile")) return route.fulfill({ json: { fullName: "Học viên kiểm thử", email: "student@example.test" } });
    if (path === "/api/v1/student/calendar/sessions") {
      expect(request.method()).toBe("GET");expect(request.headers().authorization).toBe("Bearer test-access");
      expect(url.searchParams.get("size")).toBe("100");state.requests.push(url);
      return route.fulfill({ status: state.status, json: state.status === 200 ? calendarPage(url, state.sessions) : { code: "UNAVAILABLE", message: "PRIVATE" } });
    }
    return route.abort();
  });
  return state;
}
export async function openStudentCalendar(page: Page) {
  await page.goto("/student/dashboard");
  await expect(page).toHaveURL(/\/login\?next=/);
  await page.getByLabel("Email", { exact: true }).fill("student@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("synthetic-password");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Lịch học của tôi", exact: true })).toBeVisible();
}
