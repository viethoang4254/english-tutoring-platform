import { expect, test, type Page } from "@playwright/test";

async function login(page: Page, role = "ADMIN", modal = false) {
  await page.route("**/api/v1/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "test-csrf" } });
    if (path.endsWith("/login")) return route.fulfill({ json: { accessToken: "test-access", tokenType: "Bearer" } });
    if (path.endsWith("/users/me")) return route.fulfill({ json: { id: "1", role, emailVerified: true } });
    if (path.endsWith("/profile")) return route.fulfill({ json: { fullName: "Người dùng thử nghiệm", email: "test@example.invalid" } });
    if (path.includes("/pending")) return route.fulfill({ json: { items: [], page: 0, size: 20, hasNext: false } });
    if (path.endsWith("/onboarding")) return route.fulfill({ json: { applicationId: "1", status: "PENDING" } });
    return route.fulfill({ status: 204 });
  });
  await page.goto(modal ? "/" : "/login");
  if (modal) await page.getByRole("button", { name: "Tài khoản", exact: true }).click();
  await page.getByLabel("Email", { exact: true }).fill("test@example.invalid");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("password");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
}
async function menu(page: Page) {
  if (page.viewportSize()!.width < 900) await page.getByRole("button", { name: "Điều hướng quản trị", exact: true }).click();
}
for (const modal of [false, true]) test(`Admin login and protected dashboard (${modal ? "modal" : "page"})`, async ({ page }, info) => {
  await login(page, "ADMIN", modal);
  await expect(page).toHaveURL(/\/admin\/dashboard$/);
  await expect(page.getByRole("heading", { name: "Tổng quan", exact: true })).toBeVisible();
  await expect(page.getByText("Chưa có số liệu tổng hợp", { exact: true })).toHaveCount(5);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath("admin-dashboard.png"), fullPage: true });
  await page.getByRole("contentinfo").getByRole("link", { name: "Đăng nhập", exact: true }).click();
  await expect(page).toHaveURL(/\/admin\/dashboard$/);
  await page.getByRole("button", { name: "Mở danh sách chờ xét duyệt" }).click();
  await page.getByRole("button", { name: "Cập nhật danh sách chờ xét duyệt" }).click();
  await expect(page.getByText("Không có hồ sơ chờ xét duyệt trên trang này.")).toBeVisible();
  await menu(page);
  await page.getByRole("button", { name: "Quản lý danh mục", exact: true }).click();
  await expect(page.getByRole("button", { name: "Tạo danh mục", exact: true })).toBeVisible();
  await menu(page);
  await page.getByRole("navigation", { name: "Điều hướng quản trị", exact: true }).getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await expect(page).toHaveURL(/\/login\?next=\/admin\/dashboard$/);
});
test("Anonymous access redirects without protected requests", async ({ page }) => {
  await page.route("**/api/v1/admin/**", () => { throw new Error("Anonymous Admin request"); });
  await page.goto("/admin/dashboard");
  await expect(page).toHaveURL(/\/login\?next=\/admin\/dashboard$/);
});
test("Review load failure is visible and can be retried explicitly", async ({ page }) => {
  await login(page);
  await expect(page).toHaveURL(/\/admin\/dashboard$/);
  await page.route("**/admin/teacher-applications/pending?*", route => route.fulfill({ status: 503, json: { code: "UNAVAILABLE" } }));
  await page.getByRole("button", { name: "Mở danh sách chờ xét duyệt" }).click();
  await page.getByRole("button", { name: "Cập nhật danh sách chờ xét duyệt" }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("Dịch vụ tạm thời không khả dụng");
  await expect(page.getByRole("button", { name: "Cập nhật danh sách chờ xét duyệt" })).toBeEnabled();
});
for (const role of ["STUDENT", "TEACHER"]) test(`${role} routing is preserved and Admin access denied`, async ({ page }) => {
  await login(page, role);
  await expect(page).toHaveURL(role === "STUDENT" ? /\/student\/dashboard$/ : /\/login$/);
  if (role === "TEACHER") await expect(page.getByText("Đã đăng nhập với vai trò gia sư.")).toBeVisible();
  // Use the installed Next.js client router to retain the real in-memory provider.
  await page.evaluate(() => (window as unknown as { next: { router: { push: (path: string) => void } } }).next.router.push("/admin/dashboard"));
  await expect(page.getByRole("heading", { name: "Không có quyền truy cập" })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Điều hướng quản trị", exact: true })).toHaveCount(0);
});
