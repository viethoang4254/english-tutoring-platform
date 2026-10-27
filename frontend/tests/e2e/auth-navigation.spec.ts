import { expect, test } from "@playwright/test";

test("modal dismisses with Escape/backdrop, traps focus and links to registration", async ({ page }) => {
  await page.goto("/");
  const account = page.getByRole("button", { name: "Tài khoản", exact: true });
  await account.click();
  const dialog = page.getByRole("dialog", { name: "Đăng nhập" });
  await expect(dialog).toBeVisible();
  await expect(page).toHaveURL(/\/$/);
  await page.keyboard.press("Shift+Tab");
  expect(await dialog.evaluate(el => el.contains(document.activeElement))).toBe(true);
  await page.keyboard.press("Escape");
  await expect(dialog).toHaveCount(0);
  await expect(account).toBeFocused();
  await account.click();
  await page.mouse.click(2, 2);
  await expect(dialog).toHaveCount(0);
  await account.click();
  await page.getByRole("link", { name: "Đăng ký ngay" }).click();
  await expect(page).toHaveURL(/\/register$/);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Đăng ký học viên" })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test("confirmation mismatch prevents registration and visibility toggle preserves password", async ({ page }) => {
  let calls = 0;
  await page.route("**/api/v1/**", route => { calls++; return route.abort(); });
  await page.goto("/register");
  await expect(page.locator("#username-rules, #phone-rules, #password-rules")).toHaveCount(0);
  await expect(page.locator("[role=status]")).toBeEmpty();
  for (const label of ["Mật khẩu", "Xác nhận mật khẩu"]) {
    const field = page.getByLabel(label, { exact: true });
    await expect(field).toHaveAttribute("type", "password");
    const show = page.getByRole("button", { name: `Hiện ${label.toLowerCase()}`, exact: true });
    await expect(show.locator("svg")).toHaveCount(1);
    await expect(show).toHaveText("");
    await show.focus(); await page.keyboard.press("Enter");
    await expect(field).toHaveAttribute("type", "text");
    await page.getByRole("button", { name: `Ẩn ${label.toLowerCase()}`, exact: true }).click();
    await expect(field).toHaveAttribute("type", "password");
  }
  expect(calls).toBe(0);
  await page.getByLabel("Họ và tên").fill("Học viên");
  await page.getByLabel("Tên người dùng", { exact: true }).fill("student_01");
  await page.getByLabel("Số điện thoại", { exact: true }).fill("0912345678");
  await page.getByLabel("Email", { exact: true }).fill("student@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(" password ");
  await page.getByLabel("Xác nhận mật khẩu", { exact: true }).fill("different");
  await page.getByRole("button", { name: "Hiện mật khẩu", exact: true }).click();
  await expect(page.getByLabel("Mật khẩu", { exact: true })).toHaveAttribute("type", "text");
  await expect(page.getByLabel("Mật khẩu", { exact: true })).toHaveValue(" password ");
  await page.getByRole("button", { name: "Tạo tài khoản học viên" }).click();
  await expect(page.getByRole("status")).toContainText("Mật khẩu xác nhận không khớp.");
  expect(calls).toBe(0);
  await page.goto("/verify-email");
  await page.reload();
  await expect(page.getByLabel("Mã xác minh")).toBeVisible();
  await expect(page.getByLabel("Địa chỉ email nhận lại mã")).toBeVisible();
});

test("header login shares session across navigation and logout clears it without silent recovery", async ({ page }) => {
  let refreshes = 0;
  await page.route("**/api/v1/**", async route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "csrf-test" } });
    if (path.endsWith("/login")) return route.fulfill({ json: { accessToken: "test-token", tokenType: "Bearer" } });
    if (path.endsWith("/users/me")) return route.fulfill({ json: { id: "1", role: "STUDENT", emailVerified: true } });
    if (path.endsWith("/refresh")) refreshes++;
    return route.fulfill({ status: 204 });
  });
  await page.goto("/");
  await page.getByRole("button", { name: "Tài khoản", exact: true }).click();
  await page.getByLabel("Email", { exact: true }).fill("student@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(" password ");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.locator(".header-account-menu summary").click();
  await expect(page.getByRole("link", { name: "Tài khoản của tôi" })).toBeVisible();
  await page.getByRole("link", { name: "Tài khoản của tôi" }).click();
  await expect(page.getByText("Đã đăng nhập với vai trò học viên.")).toBeVisible();
  await page.goto("/");
  expect(refreshes).toBe(0);
  await page.getByRole("button", { name: "Tài khoản", exact: true }).click();
  await page.getByLabel("Email", { exact: true }).fill("student@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(" password ");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await page.locator(".header-account-menu summary").click();
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await expect(page.getByRole("button", { name: "Tài khoản", exact: true })).toBeVisible();
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});


test("dedicated login and registration share shell and navigate independently of the modal", async ({ page }) => {
  let logins = 0; let refreshes = 0;
  await page.route("**/api/v1/**", async route => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "csrf-test" } });
    if (path.endsWith("/refresh")) refreshes++;
    if (path.endsWith("/login")) { logins++; return route.fulfill({ json: { accessToken: "test", tokenType: "Bearer" } }); }
    return route.fulfill({ json: { id: "1", role: "STUDENT", emailVerified: true } });
  });
  await page.goto("/login"); await page.reload();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByRole("banner")).toBeVisible();
  await expect(page.getByRole("contentinfo")).toBeVisible();
  await expect(page.getByRole("navigation", { name: "Đường dẫn" })).toContainText("Đăng nhập");
  await expect(page.getByRole("link", { name: "Xác minh email để đăng nhập" })).toHaveCount(0);
  await expect(page.getByText("Đã có phiên đăng nhập?", { exact: true })).toHaveCount(0);
  await page.getByRole("link", { name: "Đăng ký ngay" }).click();
  await expect(page).toHaveURL(/\/register$/); await page.reload();
  await expect(page.getByLabel("Tên người dùng", { exact: true })).toBeVisible();
  await expect(page.getByLabel("Số điện thoại", { exact: true })).toBeVisible();
  const fields = page.locator(".registration-grid");
  const columns = await fields.evaluate(el => getComputedStyle(el).gridTemplateColumns.split(" ").length);
  expect(columns).toBe(page.viewportSize()!.width > 600 ? 2 : 1);
  await page.locator("main").getByRole("link", { name: "Đăng nhập", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.getByLabel("Email", { exact: true }).fill("student@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill(" password ");
  await page.getByRole("button", { name: "Hiện mật khẩu", exact: true }).click();
  await expect(page.getByLabel("Mật khẩu", { exact: true })).toHaveAttribute("type", "text");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page).toHaveURL(/\/student\/dashboard$/);
  expect(logins).toBe(1); expect(refreshes).toBe(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
