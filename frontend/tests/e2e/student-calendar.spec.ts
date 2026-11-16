import { expect, test } from "@playwright/test";
import { calendarFixture, calendarPage, calendarSessions, openStudentCalendar } from "../support/student-calendar";
import { captureReview } from "../support/ui-review";
import { dayKey, occursOn } from "../../src/features/student/calendar-dates";

const calendarPath = "**/api/v1/student/calendar/sessions?*";

test("@student-calendar-review authenticated sessions and schedule changes render without Meet links", async ({ page }, info) => {
  await calendarFixture(page);await openStudentCalendar(page);
  const calendar = page.locator("#lich-hoc");
  await expect(calendar.getByRole("heading", { name: "Giới thiệu bản thân", exact: true })).toBeVisible();
  await expect(calendar.getByText("Tiếng Anh trong công việc", { exact: true })).toBeVisible();
  await expect(calendar.getByText("Gia sư kiểm thử · Buổi 1", { exact: true })).toBeVisible();
  await expect(calendar.getByText("Múi giờ khóa học: Asia/Ho_Chi_Minh")).toBeVisible();
  await expect(page.getByRole("complementary", { name: "Sắp diễn ra" }).getByText("Trao đổi trong cuộc họp", { exact: true })).toBeVisible();
  await expect(page.locator('a[href*="meet.google.com"]')).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Vào lớp" })).toHaveCount(0);
  await captureReview(page, info, "student-calendar");
  await calendar.getByRole("button", { name: /20 tháng 10.*2 buổi học/ }).click();
  await expect(calendar.getByText("Đã đổi lịch", { exact: true })).toBeVisible();
  await expect(calendar.getByText("Đã hủy", { exact: true })).toHaveCount(2); // Legend and cancelled session.
  await expect(page.getByRole("complementary", { name: "Sắp diễn ra" }).getByText("Hội thoại theo tình huống", { exact: true })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await captureReview(page, info, "student-calendar-changes");
});

test("month navigation requests the complete visible grid and preserves selection", async ({ page }) => {
  const fixture = await calendarFixture(page);await openStudentCalendar(page);
  await expect(page.locator("#lich-hoc").getByRole("heading", { name: "Giới thiệu bản thân" })).toBeVisible();
  expect(fixture.requests[0].searchParams.get("from")).toBe("2026-09-28T00:00:00+07:00");
  expect(fixture.requests[0].searchParams.get("to")).toBe("2026-11-02T00:00:00+07:00");
  await page.getByRole("button", { name: "Tháng sau", exact: true }).click();
  await expect(page.getByText("Chưa có buổi học trong kỳ lịch này.", { exact: false })).toBeVisible();
  expect(fixture.requests.at(-1)?.searchParams.get("from")).toBe("2026-10-26T00:00:00+07:00");
  expect(fixture.requests.at(-1)?.searchParams.get("to")).toBe("2026-12-07T00:00:00+07:00");
  await page.getByRole("button", { name: "Hôm nay", exact: true }).click();
  await expect(page.locator("#lich-hoc").getByRole("heading", { name: "Giới thiệu bản thân" })).toBeVisible();
  await expect(page.locator("#lich-hoc").getByRole("button", { pressed: true })).toHaveAttribute("aria-current", "date");
});

test("all server pages are read before displaying a complete month", async ({ page }) => {
  const fixture = await calendarFixture(page);
  fixture.sessions = Array.from({ length: 101 }, (_, i) => ({ ...calendarSessions[0], id: String(i + 1000), title: `Buổi kiểm thử ${i + 1}` }));
  await openStudentCalendar(page);
  await expect(page.locator("#lich-hoc").getByRole("heading", { name: "Buổi kiểm thử 101", exact: true })).toBeVisible();
  expect(fixture.requests.map(url => url.searchParams.get("page"))).toEqual(["0", "1"]);
  await expect(page.locator("#lich-hoc article")).toHaveCount(101);
});

test("loading is explicit and does not display a false empty calendar", async ({ page }) => {
  await calendarFixture(page);
  let release!: () => void;const gate = new Promise<void>(resolve => { release = resolve; });
  await page.route(calendarPath, async route => { await gate;await route.fulfill({ json: calendarPage(new URL(route.request().url())) }); });
  await openStudentCalendar(page);
  await expect(page.getByText("Đang tải lịch học…", { exact: true })).toBeVisible();
  await expect(page.getByText("Không có buổi học trong ngày này", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Tải lại lịch học" })).toBeDisabled();
  release();
  await expect(page.locator("#lich-hoc").getByRole("heading", { name: "Giới thiệu bản thân" })).toBeVisible();
});

test("empty enrolled calendar never falls back to demonstrations", async ({ page }) => {
  const fixture = await calendarFixture(page);fixture.sessions = [];await openStudentCalendar(page);
  await expect(page.getByText("Chưa có buổi học trong kỳ lịch này.", { exact: false })).toBeVisible();
  await expect(page.getByText("Không có buổi học sắp tới trong kỳ lịch đang xem.", { exact: true })).toBeVisible();
  await expect(page.getByText("Không có buổi học trong ngày này", { exact: true })).toBeVisible();
  await expect(page.getByText("Bản xem trước · Dữ liệu minh họa")).toHaveCount(0);
  await expect(page.locator("#lich-hoc article")).toHaveCount(0);
  await expect(page.getByRole("link", { name: "Xem khóa học của tôi", exact: true })).toHaveAttribute("href", "/student/enrollments");
});

test("calendar failure has an explicit safe retry independent of profile", async ({ page }) => {
  const fixture = await calendarFixture(page);fixture.status = 503;await openStudentCalendar(page);
  const region = page.getByRole("region", { name: "Lịch học đã đăng ký" });
  await expect(region.getByRole("alert")).toContainText("Không thể tải lịch học. Vui lòng thử lại.");
  await expect(page.getByText("PRIVATE")).toHaveCount(0);expect(fixture.requests).toHaveLength(1);
  await expect(page.getByRole("heading", { name: "Lịch học", exact: true })).toBeVisible();
  fixture.status = 200;await page.getByRole("button", { name: "Thử lại lịch học", exact: true }).click();
  await expect(page.locator("#lich-hoc").getByRole("heading", { name: "Giới thiệu bản thân" })).toBeVisible();
  expect(fixture.requests).toHaveLength(2);
});

for (const status of [401, 403]) test(`calendar authorization ${status} fails closed`, async ({ page }) => {
  const fixture = await calendarFixture(page);
  // Let the dashboard mount before provoking an authorization change.
  await openStudentCalendar(page);
  await expect(page.locator("#lich-hoc article")).toHaveCount(1);
  fixture.status = status;await page.getByRole("button", { name: "Tải lại lịch học" }).click();
  if (status === 401) await expect(page.getByRole("heading", { name: "Đăng nhập", exact: true })).toBeVisible();
  else await expect(page.getByRole("region", { name: "Lịch học đã đăng ký" }).getByRole("alert")).toContainText("Tài khoản không có quyền xem lịch học.");
  await expect(page.locator("#lich-hoc article")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "Thử lại lịch học", exact: true })).toHaveCount(0);
  expect(fixture.requests).toHaveLength(2);
});

test("late previous-month responses cannot overwrite the current month", async ({ page }) => {
  await calendarFixture(page);
  let release!: () => void;const gate = new Promise<void>(resolve => { release = resolve; });
  await page.route(calendarPath, async route => {
    const url = new URL(route.request().url());
    if (url.searchParams.get("from")?.startsWith("2026-09-28")) { await gate;return route.fulfill({ json: calendarPage(url) }); }
    return route.fallback();
  });
  await openStudentCalendar(page);await expect(page.getByText("Đang tải lịch học…", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Tháng sau", exact: true }).click();
  await expect(page.getByText("Chưa có buổi học trong kỳ lịch này.", { exact: false })).toBeVisible();
  const oldResponse = page.waitForResponse(response => new URL(response.url()).searchParams.get("from")?.startsWith("2026-09-28") === true);
  release();await (await oldResponse).finished();
  await expect(page.locator("#lich-hoc").getByRole("heading", { name: "tháng 11 năm 2026", exact: true })).toBeVisible();
  await expect(page.locator("#lich-hoc article")).toHaveCount(0);
});

test("course navigation uses the session course and returning reloads eligibility", async ({ page }) => {
  const fixture = await calendarFixture(page);await openStudentCalendar(page);
  await page.locator("#lich-hoc").getByRole("link", { name: "Xem khóa học", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/dashboard\/courses\/42$/);
  fixture.sessions = [];
  await page.goBack();
  await expect(page.getByText("Chưa có buổi học trong kỳ lịch này.", { exact: false })).toBeVisible();
  expect(fixture.requests.length).toBeGreaterThanOrEqual(2);
});

test("day overlap respects midnight exclusivity and offset instants", () => {
  const overnight = { startsAt: "2026-10-18T23:30:00+07:00", endsAt: "2026-10-19T00:30:00+07:00" };
  expect(occursOn(overnight, "2026-10-18")).toBe(true);expect(occursOn(overnight, "2026-10-19")).toBe(true);
  expect(occursOn({ ...overnight, endsAt: "2026-10-19T00:00:00+07:00" }, "2026-10-19")).toBe(false);
  expect(dayKey("2030-03-10T01:30:00-05:00")).toBe("2030-03-10");
});

test("inconsistent pagination fails safely instead of showing duplicate sessions", async ({ page }) => {
  await calendarFixture(page);
  await page.route(calendarPath, route => route.fulfill({ json: { items: [calendarSessions[0], calendarSessions[0]], page: 0, size: 100, hasNext: false } }));
  await openStudentCalendar(page);
  await expect(page.getByRole("region", { name: "Lịch học đã đăng ký" }).getByRole("alert")).toContainText("Không thể tải lịch học.");
  await expect(page.locator("#lich-hoc article")).toHaveCount(0);
});
