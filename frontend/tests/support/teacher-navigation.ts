import { expect, type Page } from "@playwright/test";

export async function openTeacher(page: Page) {
  await page.locator(".header-account-menu summary").click();
  await page.locator(".header-account-menu").getByRole("link", { name: "Không gian gia sư", exact: true }).click();
}

export async function teacherPanel(page: Page, name: string) {
  const toggle = page.locator('button[aria-controls="dashboard-navigation"]');
  await toggle.waitFor({ state: "attached" });
  if (await toggle.isVisible() && await toggle.getAttribute("aria-expanded") === "false") await toggle.click();
  const link = page.getByRole("navigation", { name: "Điều hướng Dashboard", exact: true }).getByRole("link", { name, exact: true });
  const href = await link.getAttribute("href");
  await link.click();
  await expect(page).toHaveURL(new URL(href!, page.url()).href);
  await expect(page.getByRole("heading", { level: 1, name, exact: true })).toBeVisible();
}
