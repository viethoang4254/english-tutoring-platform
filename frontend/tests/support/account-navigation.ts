import type { Page } from "@playwright/test";

export async function accountPanel(page: Page, name: string) {
  const label = name === "Đổi mật khẩu" || name === "Hồ sơ cá nhân" ? "Cài đặt tài khoản" : name === "Xét duyệt gia sư" ? "Tài khoản & hồ sơ đăng ký" : name;
  const toggle = page.locator('button[aria-controls="dashboard-navigation"]');
  await toggle.waitFor({ state: "attached" });
  if (await toggle.isVisible() && await toggle.getAttribute("aria-expanded") === "false") await toggle.click();
  await page.getByRole("navigation", { name: "Điều hướng Dashboard", exact: true }).getByRole("link", { name: label, exact: true }).click();
}
