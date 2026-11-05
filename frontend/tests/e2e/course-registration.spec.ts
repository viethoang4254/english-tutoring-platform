import { expect, test, type Page } from "@playwright/test";
import type { Enrollment } from "../../src/features/enrollment/enrollment";
import type { PublicCourseDetail } from "../../src/features/course/public-courses";

const courseId = "9007199254740993";
const detail: PublicCourseDetail = { course: { id: courseId, name: "Khóa học đăng ký", description: "Thông tin công khai", tuition: { amount: "1800000.00", currency: "VND" }, category: { id: "11", name: "Giao tiếp" }, teacher: { id: "21", fullName: "Gia sư An" }, plannedStartDate: "2030-01-08", timezone: "Asia/Ho_Chi_Minh", sessionCount: 12 }, rules: [] };
const row: Enrollment = { id: "9007199254740994", courseId, courseTitle: detail.course.name, status: "PENDING", enrolledAt: null, seatReservedUntil: "2030-01-01T00:15:00Z", reservationExpired: false, cancelledAt: null, cancellationReason: null, createdAt: "2030-01-01T00:00:00Z", updatedAt: "2030-01-01T00:00:00Z" };
async function setup(page: Page, options: { role?: "STUDENT" | "TEACHER" | "ADMIN"; responseStatus?: number; code?: string; network?: boolean; active?: boolean; gate?: Promise<void> } = {}) {
  const mutations: string[] = [];
  await page.route("**/api/v1/**", async route => {
    const request = route.request(), path = new URL(request.url()).pathname;
    if (request.method() === "POST") mutations.push(path);
    if (path === `/api/v1/public/courses/${courseId}`) return route.fulfill({ json: detail });
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "fixture-csrf" } });
    if (path.endsWith("/login")) {
      expect(request.headers()["x-xsrf-token"]).toBe("fixture-csrf");
      return route.fulfill({ json: { accessToken: "fixture-token", tokenType: "Bearer" } });
    }
    if (path.endsWith("/users/me")) return route.fulfill({ json: { id: "1", role: options.role ?? "STUDENT", emailVerified: false } });
    if (path === `/api/v1/student/courses/${courseId}/enrollment`) {
      expect(request.method()).toBe("POST");
      expect(request.headers().authorization).toBe("Bearer fixture-token");
      expect(request.postData()).toBeNull();
      expect(request.headers().cookie).toBeUndefined();
      if (options.gate) await options.gate;
      if (options.network && mutations.filter(p => p.endsWith("/enrollment")).length === 1) return route.abort();
      return route.fulfill({ status: options.responseStatus ?? 201, json: options.code ? { code: options.code, message: "private diagnostic" } : options.active ? { ...row, status: "ACTIVE" } : row });
    }
    return route.fulfill({ status: 404, json: { code: "NOT_FOUND", message: "Not found" } });
  });
  await page.goto(`/courses/${courseId}`);
  await expect(page.getByRole("heading", { name: detail.course.name, exact: true })).toBeVisible();
  return mutations;
}
async function login(page: Page) {
  await page.getByRole("button", { name: "Đăng nhập để đăng ký", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "Đăng nhập", exact: true });
  await dialog.getByLabel("Email", { exact: true }).fill("fixture@example.invalid");
  await dialog.getByLabel("Mật khẩu", { exact: true }).fill("fixture-password");
  await dialog.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(dialog).not.toBeVisible();
  await expect(page.locator(".header-account-menu summary")).toBeVisible();
  await expect(page).toHaveURL(new RegExp(`/courses/${courseId}$`));
}
const reserve = (page: Page) => page.getByRole("button", { name: "Đăng ký giữ chỗ", exact: true });
const registration = (page: Page) => page.getByRole("region", { name: "Đăng ký khóa học", exact: true });
for (const responseStatus of [201, 200]) test(`Student reservation ${responseStatus} preserves PENDING and real IDs`, async ({ page }) => {
  const calls = await setup(page, { responseStatus }); await login(page);
  await reserve(page).click();
  await expect(registration(page).getByRole("status")).toHaveText("Đã xác nhận đăng ký giữ chỗ: Chờ thanh toán.");
  await expect(registration(page).getByRole("link", { name: "Đăng ký của tôi" })).toHaveAttribute("href", `/student/enrollments?id=${row.id}`);
  await expect(reserve(page)).toHaveCount(0);
  await expect(registration(page).getByText(/Thanh toán chưa được tích hợp/)).toBeVisible();
  await expect(page.locator('a[href*="meet.google"]')).toHaveCount(0);
  expect(calls).toEqual(["/api/v1/auth/login", `/api/v1/student/courses/${courseId}/enrollment`]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
for (const role of ["TEACHER", "ADMIN"] as const) test(`${role} cannot reserve`, async ({ page }) => {
  const calls = await setup(page, { role }); await login(page);
  await expect(registration(page).getByText("Chỉ tài khoản học viên được đăng ký khóa học.")).toBeVisible();
  await expect(reserve(page)).toHaveCount(0);
  expect(calls).toEqual(["/api/v1/auth/login"]);
});
for (const [responseStatus, code, message] of [
  [409, "COURSE_FULL", "Khóa học đã hết chỗ."],
  [409, "COURSE_NOT_ENROLLABLE", "Khóa học không còn nhận đăng ký hoặc đã bắt đầu."],
  [409, "ENROLLMENT_RENEWAL_REQUIRED", "Giữ chỗ đã hết hạn. Bạn cần chủ động gia hạn."],
  [409, "ENROLLMENT_STATE_CONFLICT", "Trạng thái đăng ký không cho phép thao tác này."],
  [404, "COURSE_NOT_FOUND", "Khóa học không tồn tại hoặc không còn khả dụng."],
  [403, "ACCESS_DENIED", "Bạn không có quyền truy cập đăng ký này."],
  [401, "UNAUTHORIZED", ""],
  [503, "ENROLLMENT_UNAVAILABLE", "Chưa xác nhận được kết quả. Kiểm tra Đăng ký của tôi hoặc thử lại; yêu cầu trùng không kéo dài thời hạn giữ chỗ."],
] as const) test(`reservation handles ${code} without granting access`, async ({ page }) => {
  const calls = await setup(page, { responseStatus, code }); await login(page); await reserve(page).click();
  if (responseStatus === 401) {
    await expect(page.getByRole("button", { name: "Đăng nhập để đăng ký", exact: true })).toBeVisible();
    await expect(page.locator(".header-account-menu")).toHaveCount(0);
  } else {
    await expect(registration(page).getByRole("alert")).toHaveText(message);
    await expect(registration(page).getByRole("link", { name: "Đăng ký của tôi" })).toHaveAttribute("href", "/student/enrollments");
    if (responseStatus === 403) await expect(reserve(page)).toBeDisabled();
  }
  expect(calls).toEqual(["/api/v1/auth/login", `/api/v1/student/courses/${courseId}/enrollment`]);
  await expect(page.getByText("private diagnostic")).toHaveCount(0);
  await expect(page.locator('a[href*="meet.google"]')).toHaveCount(0);
});
test("pending submission prevents repeated mutations", async ({ page }) => {
  let release!: () => void; const gate = new Promise<void>(resolve => { release = resolve; });
  const calls = await setup(page, { gate }); await login(page);
  try {
    await reserve(page).click();
    const busy = page.getByRole("button", { name: "Đang gửi đăng ký…", exact: true });
    await expect(busy).toBeDisabled();
    await busy.evaluate(button => { (button as HTMLButtonElement).click(); (button as HTMLButtonElement).click(); });
    await expect.poll(() => calls.filter(p => p.endsWith("/enrollment")).length).toBe(1);
  } finally { release(); }
  await expect(registration(page).getByRole("status")).toContainText("Chờ thanh toán");
  expect(calls.filter(p => p.endsWith("/enrollment"))).toHaveLength(1);
});
test("uncertain network outcome allows explicit idempotent retry only", async ({ page }) => {
  const calls = await setup(page, { network: true, responseStatus: 200 }); await login(page); await reserve(page).click();
  await expect(registration(page).getByRole("alert")).toContainText("Chưa xác nhận được kết quả.");
  expect(calls.filter(p => p.endsWith("/enrollment"))).toHaveLength(1);
  await reserve(page).click();
  await expect(registration(page).getByRole("status")).toContainText("Chờ thanh toán");
  expect(calls.filter(p => p.endsWith("/enrollment"))).toHaveLength(2);
});
test("existing ACTIVE relationship navigates without claiming a new reservation", async ({ page }) => {
  await setup(page, { active: true, responseStatus: 200 }); await login(page); await reserve(page).click();
  await expect(registration(page).getByRole("status")).toHaveText("Bạn đã có đăng ký: Đang tham gia.");
  await expect(registration(page).getByRole("link", { name: "Đăng ký của tôi" })).toHaveAttribute("href", `/student/enrollments?id=${row.id}`);
  await expect(page.locator('a[href*="meet.google"]')).toHaveCount(0);
});
test("anonymous direct entry and refresh never create a reservation; login dialog is keyboard accessible", async ({ page }) => {
  const calls = await setup(page); await page.reload();
  const trigger = page.getByRole("button", { name: "Đăng nhập để đăng ký", exact: true });
  await trigger.click(); await expect(page.getByRole("dialog", { name: "Đăng nhập" })).toBeVisible();
  await page.keyboard.press("Escape"); await expect(trigger).toBeFocused();
  expect(calls).toEqual([]);
  await login(page); await expect(reserve(page)).toBeVisible();
  expect(calls).toEqual(["/api/v1/auth/login"]);
});
