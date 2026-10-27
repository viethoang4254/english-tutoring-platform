import { expect, test } from "@playwright/test";

test("landing page renders without overflow or browser errors", async ({ page }) => {
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));

  const response = await page.goto("/");
  expect(response?.status()).toBe(200);
  await expect(page.locator("html")).toHaveAttribute("lang", "vi");
  await expect(page.getByRole("heading", { name: "Học tiếng Anh. Có người đồng hành." })).toBeVisible();
  await expect(page.getByText("Khám phá giao diện danh sách khóa học với dữ liệu minh họa.", { exact: false })).toBeVisible();
  const fitsViewport = await page.evaluate(
    () => document.documentElement.scrollWidth <= window.innerWidth,
  );
  expect(fitsViewport).toBe(true);
  expect(pageErrors).toEqual([]);
  await expect(page.getByRole("heading", { name: "Đăng ký tài khoản học viên", exact: true })).toBeVisible();
  await expect(page.getByText("Tạo tài khoản học viên, sau đó đăng nhập bằng email và mật khẩu để bắt đầu.")).toBeVisible();
});


test("missing page is localized", async ({ page }) => {
  const response = await page.goto("/missing-localization-test");
  expect(response?.status()).toBe(404);
  await expect(page.getByRole("heading", { name: "Không tìm thấy trang" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Về trang chủ" })).toHaveAttribute("href", "/");
});

test("landing navigation and separate registration links use existing forms", async ({ page }) => {
  await page.goto("/");
  const mobile = page.getByRole("navigation", { name: "Điều hướng di động" });
  if (page.viewportSize()!.width < 761) {
    await page.getByText("Menu", { exact: false }).click();
    await expect(mobile).toBeVisible();
    await mobile.getByRole("link", { name: "Khóa học" }).click();
  } else {
    await page.getByRole("navigation", { name: "Điều hướng chính" }).getByRole("link", { name: "Khóa học" }).click();
  }
  await expect(page).toHaveURL(/\/courses$/);
  await page.getByRole("contentinfo").getByRole("link", { name: "Đăng ký học viên", exact: true }).click();
  await expect(page.getByRole("heading", { name: "Đăng ký học viên", exact: true })).toBeVisible();
  await expect(page.getByLabel("Chuyên môn")).toHaveCount(0);
  await page.goto("/");
  const footer = page.getByRole("contentinfo");
  await expect(footer.getByRole("heading", { name: "THÔNG TIN" })).toBeVisible();
  const teacherLink = footer.getByRole("link", { name: "Đăng ký giảng viên" });
  await teacherLink.scrollIntoViewIfNeeded();
  await expect(teacherLink).toBeInViewport();
  await expect(teacherLink).toHaveAttribute("href", "/become-a-teacher");
  await teacherLink.click();
  await expect(page).toHaveURL(/\/become-a-teacher$/);
  await page.reload();
  await expect(page.getByRole("heading", { name: "Đăng ký gia sư", exact: true })).toBeVisible();
  await expect(page.getByLabel("Chuyên môn")).toBeVisible();
  await page.goto("/account?view=unsupported");
  await expect(page.getByRole("heading", { name: "Đăng nhập", exact: true })).toBeVisible();
});
