import { test, expect, type Page } from "@playwright/test";

async function open(page: Page) {
  await page.route("**/api/v1/**", route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "fixture" } });
    if (path.endsWith("/login")) return route.fulfill({ json: { accessToken: "fixture", tokenType: "Bearer" } });
    if (path.endsWith("/users/me")) return route.fulfill({ json: { id: "1", role: "ADMIN", emailVerified: true } });
    return route.fulfill({ status: 404, json: { code: "NOT_FOUND", message: "Unavailable" } });
  });
  await page.goto("/login");
  await page.getByLabel("Email", { exact: true }).fill("admin@example.invalid");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("fixture-password");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page).toHaveURL(/\/admin\/dashboard$/);
  if (page.viewportSize()!.width < 900) await page.getByRole("button", { name: "Điều hướng quản trị", exact: true }).click();
  await page.getByRole("button", { name: "Quản lý danh mục", exact: true }).click();
}
const item = { id: "9007199254740993", name: "Giao tiếp", active: true, createdAt: "2026-10-22T00:00:00Z", updatedAt: "2026-10-22T00:00:00Z" };

test("Category create rename status pagination filter and responsive layout", async ({ page }, info) => {
  await open(page);
  let current = { ...item };
  const writes: unknown[] = [];
  await page.route("**/api/v1/admin/categories**", route => {
    const req = route.request(), url = new URL(req.url());
    expect(req.headers().authorization).toBe("Bearer fixture");
    if (req.method() !== "GET") {
      const body = req.postDataJSON(); writes.push(body); current = { ...current, ...body };
      if (req.method() === "PATCH") expect(url.pathname).toBe("/api/v1/admin/categories/" + item.id);
      return route.fulfill({ status: req.method() === "POST" ? 201 : 200, json: current });
    }
    const number = Number(url.searchParams.get("page"));
    return route.fulfill({ json: { items: number || url.searchParams.get("active") === "false" ? [] : [current], page: number, size: 20, hasNext: number === 0 } });
  });
  await page.getByRole("button", { name: "Tải lại danh sách" }).click();
  await expect(page.getByText("Giao tiếp", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Trang sau" }).click();
  await expect(page.getByText("Không có danh mục trên trang này.")).toBeVisible();
  await page.getByRole("button", { name: "Trang trước" }).click();
  await page.getByLabel("Tên danh mục", { exact: true }).fill("  Mới  ");
  await page.getByRole("button", { name: "Tạo danh mục" }).click();
  await expect(page.getByText("Mới", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Đổi tên" }).click();
  await page.getByLabel("Tên danh mục mới").fill("Đổi tên mới");
  await page.getByRole("button", { name: "Lưu tên" }).click();
  await expect(page.getByText("Đổi tên mới", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Ngừng hoạt động", exact: true }).click();
  await expect(page.getByRole("button", { name: "Kích hoạt" })).toBeVisible();
  await page.getByRole("button", { name: "Kích hoạt" }).click();
  await expect(page.getByRole("button", { name: "Ngừng hoạt động", exact: true })).toBeVisible();
  expect(writes).toEqual([{ name: "Mới" }, { name: "Đổi tên mới" }, { active: false }, { active: true }]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: info.outputPath("categories.png"), fullPage: true });
  await page.getByRole("combobox", { name: "Trạng thái" }).selectOption("false");
  await expect(page.getByText("Không có danh mục trên trang này.")).toBeVisible();
});

test("Category validation conflict and uncertain outcomes never replay writes", async ({ page }) => {
  await open(page);
  let writes = 0, mode = "conflict";
  await page.route("**/api/v1/admin/categories**", route => {
    if (route.request().method() === "GET") return route.fulfill({ json: { items: [], page: 0, size: 20, hasNext: false } });
    writes++;
    if (mode === "abort") return route.abort();
    return route.fulfill({ status: mode === "validation" ? 400 : 409, json: { code: mode === "validation" ? "INVALID_REQUEST" : "CATEGORY_NAME_CONFLICT", message: "Rejected" } });
  });
  await page.getByLabel("Tên danh mục", { exact: true }).fill(" ");
  await page.getByRole("button", { name: "Tạo danh mục" }).click();
  await expect(page.getByText(/Tên danh mục phải có/)).toBeVisible();
  expect(writes).toBe(0);
  await page.getByLabel("Tên danh mục", { exact: true }).fill("Duplicate");
  await page.getByRole("button", { name: "Tạo danh mục" }).click();
  await expect(page.getByText(/Tên danh mục đã được sử dụng/)).toBeVisible();
  mode = "validation";
  await page.getByRole("button", { name: "Tạo danh mục" }).click();
  await expect(page.getByText(/Thông tin chưa hợp lệ/)).toBeVisible();
  mode = "abort";
  await page.getByRole("button", { name: "Tạo danh mục" }).click();
  await expect(page.getByRole("button", { name: "Tạo danh mục" })).toBeDisabled();
  expect(writes).toBe(3);
  await page.getByRole("button", { name: "Tải lại danh sách" }).click();
  await expect(page.getByRole("button", { name: "Tạo danh mục" })).toBeEnabled();
  expect(writes).toBe(3);
});

for (const status of [401, 403, 503]) test(`Category handles HTTP ${status}`, async ({ page }) => {
  await open(page);
  await page.route("**/api/v1/admin/categories**", route => route.fulfill({ status, json: { code: status === 401 ? "AUTHENTICATION_REQUIRED" : status === 403 ? "ACCESS_DENIED" : "CATEGORY_UNAVAILABLE", message: "Unavailable" } }));
  await page.getByRole("button", { name: "Tải lại danh sách" }).click();
  if (status === 401) await expect(page).toHaveURL(/\/login\?next=\/admin\/dashboard$/);
  else {
    await expect(page.getByRole("alert")).toBeVisible();
    await expect(page.getByRole("button", { name: "Tạo danh mục" })).toBeDisabled();
  }
});

test("Category pending mutation prevents duplicate submissions", async ({ page }) => {
  await open(page);
  let writes = 0;
  let release!: () => void;
  const waiting = new Promise<void>(resolve => { release = resolve; });
  await page.route("**/api/v1/admin/categories**", async route => {
    if (route.request().method() === "GET") return route.fulfill({ json: { items: [item], page: 0, size: 20, hasNext: false } });
    writes++;
    await waiting;
    return route.fulfill({ status: 201, json: item });
  });
  await page.getByLabel("Tên danh mục", { exact: true }).fill(item.name);
  await page.getByRole("button", { name: "Tạo danh mục" }).click();
  await expect(page.getByRole("button", { name: "Tạo danh mục" })).toBeDisabled();
  await page.getByLabel("Tên danh mục", { exact: true }).evaluate(input => (input as HTMLInputElement).form!.requestSubmit());
  expect(writes).toBe(1);
  release();
  await expect(page.getByText(item.name, { exact: true })).toBeVisible();
  expect(writes).toBe(1);
});
