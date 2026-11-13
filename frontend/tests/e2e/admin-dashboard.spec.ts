import { expect, test, type Page } from "@playwright/test";

async function login(page: Page, role = "ADMIN", modal = false) {
  let remembered = false;
  await page.route("**/api/v1/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "test-csrf" } });
    if (path.endsWith("/refresh") && remembered) { remembered = false; return route.fulfill({ json: { accessToken: "test-access", tokenType: "Bearer" } }); }
    if (path.endsWith("/login")) { remembered = !!route.request().postDataJSON()?.rememberMe; return route.fulfill({ json: { accessToken: "test-access", tokenType: "Bearer" } }); }
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
  await page.getByLabel("Ghi nhớ đăng nhập").check();
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
}
async function menu(page: Page) {
  if (page.viewportSize()!.width < 900) await page.locator("[data-dashboard-toggle]").click();
}
for (const modal of [false, true]) test(`Admin login and protected dashboard (${modal ? "modal" : "page"})`, async ({ page }, info) => {
  await login(page, "ADMIN", modal);
  await expect(page).toHaveURL(/\/admin\/dashboard$/);
  await expect(page.getByRole("heading", { name: "Tổng quan", exact: true })).toBeVisible();
  await expect(page.getByRole("contentinfo")).toHaveCount(0);
  await expect(page.getByRole("navigation",{name:"Điều hướng chính",exact:true})).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath("admin-dashboard.png"), fullPage: true });

  await page.getByRole("button", { name: "Mở danh sách chờ xét duyệt" }).click();
  await page.getByRole("button", { name: "Cập nhật danh sách chờ xét duyệt" }).click();
  await expect(page.getByText("Không có hồ sơ chờ xét duyệt trên trang này.")).toBeVisible();
  await menu(page);
  await page.getByRole("link", { name: "Quản lý danh mục", exact: true }).click();
  await expect(page).toHaveURL(/section=categories$/);
  await expect(page.getByRole("button", { name: "Tạo danh mục", exact: true })).toBeVisible();
  await page.goBack();await expect(page).toHaveURL(/section=reviews$/);
  await expect(page.getByRole("heading",{level:1,name:"Xét duyệt gia sư"})).toBeVisible();
  await page.goForward();await expect(page).toHaveURL(/section=categories$/);
  await expect(page.getByRole("heading",{level:1,name:"Quản lý danh mục"})).toBeVisible();
  await menu(page);
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).filter({visible:true}).click();
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
  await expect(page).toHaveURL(role === "STUDENT" ? /\/student\/dashboard$/ : /\/teacher$/);
  if (role === "TEACHER") await expect(page.getByText("Đã đăng nhập với vai trò gia sư.")).toBeVisible();
  await page.goto("/admin/dashboard");
  await expect(page.getByRole("heading", { name: "Không có quyền truy cập" })).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Điều hướng quản trị", exact: true })).toHaveCount(0);
});
