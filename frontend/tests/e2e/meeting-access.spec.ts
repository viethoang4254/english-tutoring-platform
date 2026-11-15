import { expect, test } from "@playwright/test";
import { calendarFixture, openStudentCalendar } from "../support/student-calendar";
import { captureReview } from "../support/ui-review";
import { teacherPanel, openTeacher } from "../support/teacher-navigation";

const room = "https://meet.google.com/abc-defg-hij";
const join = "**/api/v1/student/sessions/*/join";

test("@meeting-review student resolves only on click and opens the authorized room", async ({ page }, info) => {
  await calendarFixture(page); let calls = 0;
  await page.route(join, route => {
    calls++; expect(route.request().method()).toBe("GET");
    expect(route.request().headers().authorization).toBe("Bearer test-access");
    expect(new URL(route.request().url()).pathname).toBe("/api/v1/student/sessions/101/join");
    return route.fulfill({ json: { meetUrl: room } });
  });
  await page.route(room, route => route.fulfill({ contentType: "text/html", body: "<h1>Synthetic meeting destination</h1>" }));
  await openStudentCalendar(page);
  const button = page.getByRole("button", { name: "Tham gia Google Meet", exact: true });
  await expect(button).toBeVisible(); expect(calls).toBe(0);
  await expect(page.locator('a[href*="meet.google.com"]')).toHaveCount(0);
  await captureReview(page, info, "student-meeting");
  await button.click(); await expect(page).toHaveURL(room); expect(calls).toBe(1);
});

test("rescheduled session uses its own ID and cancelled session cannot join", async ({ page }) => {
  await calendarFixture(page); let calls = 0;
  await page.route(join, route => {
    calls++; expect(new URL(route.request().url()).pathname).toBe("/api/v1/student/sessions/102/join");
    return route.fulfill({ status: 403, json: { code: "ACCESS_DENIED", message: "PRIVATE" } });
  });
  await openStudentCalendar(page);
  await page.locator("#lich-hoc").getByRole("button", { name: /20 tháng 10.*2 buổi học/ }).click();
  const scheduled = page.locator("#lich-hoc article").filter({ hasText: "Trao đổi trong cuộc họp" });
  await expect(scheduled.getByText("Đã đổi lịch", { exact: true })).toBeVisible();
  await expect(scheduled.getByText("18:30", { exact: true })).toBeVisible();
  const cancelled = page.locator("#lich-hoc article").filter({ hasText: "Hội thoại theo tình huống" });
  await expect(cancelled.getByRole("button", { name: "Buổi học đã hủy" })).toBeDisabled();
  await expect(cancelled.getByRole("button", { name: "Tham gia Google Meet" })).toHaveCount(0);
  expect(calls).toBe(0);
  await scheduled.getByRole("button", { name: "Tham gia Google Meet" }).click();
  await expect(scheduled.getByRole("alert")).toContainText("không có quyền"); expect(calls).toBe(1);
  await expect(scheduled.getByRole("link", { name: "Xem khóa học" })).toHaveAttribute("href", "/student/dashboard/courses/42");
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

for (const status of [401, 403, 503]) test(`student join handles ${status} without exposing private data`, async ({ page }) => {
  await calendarFixture(page); let calls = 0;
  await page.route(join, route => { calls++; return route.fulfill({ status, json: { code: "UNAVAILABLE", message: "PRIVATE" } }); });
  await openStudentCalendar(page);
  await page.getByRole("button", { name: "Tham gia Google Meet" }).click();
  if (status === 401) await expect(page).toHaveURL(/\/login\?next=/);
  else {
    await expect(page.getByLabel("Truy cập Google Meet").getByRole("alert")).toBeVisible();
    await expect(page.getByText("PRIVATE", { exact: true })).toHaveCount(0);
    await expect(page.locator('a[href*="meet.google.com"]')).toHaveCount(0);
    expect(calls).toBe(1);
    await page.getByRole("button", { name: "Tham gia Google Meet" }).click();
    await expect.poll(() => calls).toBe(2);
  }
});

test("obsolete join response cannot navigate after leaving the selected day", async ({ page }) => {
  await calendarFixture(page);
  let release!: () => void;
  const wait = new Promise<void>(resolve => { release = resolve; });
  await page.route(join, async route => { await wait; await route.fulfill({ json: { meetUrl: room } }); });
  await openStudentCalendar(page);
  await page.getByRole("button", { name: "Tham gia Google Meet" }).click();
  await expect(page.getByRole("button", { name: "Đang kiểm tra quyền truy cập…" })).toBeDisabled();
  await page.getByRole("button", { name: "Tháng sau", exact: true }).click();
  await expect(page.getByRole("button", { name: "Đang kiểm tra quyền truy cập…" })).toHaveCount(0);
  const response = page.waitForResponse(r => r.url().endsWith("/sessions/101/join"));
  release(); await response;
  await expect(page).toHaveURL(/\/student\/dashboard$/);
  await expect(page.locator('a[href*="meet.google.com"]')).toHaveCount(0);
});

test("@meeting-review approved unverified Teacher reads own room and rechecks before opening", async ({ page }, info) => {
  let calls = 0, status = 200; let configured = false;
  const course = { id: "42", name: "Khóa học kiểm thử", status: "PUBLISHED", category: { id: "1", name: "Tiếng Anh", active: true }, description: "Nội dung", tuition: { amount: "100000", currency: "VND" }, minStudents: 1, maxStudents: 10, sessionCount: 2, plannedStartDate: "2030-01-01", timezone: "Asia/Ho_Chi_Minh", discount: null };
  await page.route("**/api/v1/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "fixture" } });
    if (path.endsWith("/login")) return route.fulfill({ json: { accessToken: "fixture", tokenType: "Bearer" } });
    if (path.endsWith("/users/me")) return route.fulfill({ json: { id: "1", role: "TEACHER", emailVerified: false } });
    if (path.endsWith("/teacher/onboarding")) return route.fulfill({ json: { applicationId: "1", status: "APPROVED", submittedAt: "2026-01-01T00:00:00Z", reviewedAt: null, rejectionReason: null } });
    if (path === "/api/v1/teacher/courses/42/meeting") {
      calls++; expect(route.request().method()).toBe("GET"); expect(route.request().headers().authorization).toBe("Bearer fixture");
      return route.fulfill({ status, json: status === 200 ? configured ? { meetUrl: room } : {} : { code: "COURSE_NOT_FOUND", message: "PRIVATE" } });
    }
    if (path === "/api/v1/teacher/courses/42") return route.fulfill({ json: course });
    if (path === "/api/v1/teacher/courses") return route.fulfill({ json: { items: [course], page: 0, size: 20, hasNext: false } });
    return route.fulfill({ status: 404, json: { code: "NOT_FOUND" } });
  });
  await page.goto("/account");
  await page.getByLabel("Email", { exact: true }).fill("teacher@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("synthetic-password");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await openTeacher(page); await teacherPanel(page, "Khóa học của tôi");
  await page.getByRole("button", { name: "Tải lại khóa học", exact: true }).click();
  await page.getByRole("button", { name: "Xem chi tiết", exact: true }).click();
  await expect(page.getByRole("heading", { name: course.name, exact: true })).toBeVisible(); expect(calls).toBe(0);
  await page.getByRole("button", { name: "Xem phòng Google Meet" }).click();
  await expect(page.getByLabel("Truy cập Google Meet").getByRole("alert")).toContainText("Chưa có liên kết");
  configured = true;
  await page.getByRole("button", { name: "Xem phòng Google Meet" }).click();
  await expect(page.getByRole("link", { name: "Mở Google Meet", exact: true })).toHaveAttribute("href", room);
  await captureReview(page, info, "teacher-meeting");
  status = 404;
  await page.getByRole("link", { name: "Mở Google Meet", exact: true }).click();
  await expect(page.getByLabel("Truy cập Google Meet").getByRole("alert")).toContainText("không có quyền");
  await expect(page.getByRole("link", { name: "Mở Google Meet", exact: true })).toHaveCount(0);
  await expect(page).toHaveURL(/\/teacher\/courses\/42$/); expect(calls).toBe(3);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
