import { expect, type Page } from "@playwright/test";

export async function openStudentNavigation(page: Page) {
  const toggle = page.locator("[data-dashboard-toggle]");
  await expect(toggle).toBeAttached();
  if (await toggle.isVisible() && await toggle.getAttribute("aria-expanded") === "false") await toggle.click();
  await expect(page.getByRole("navigation", {name:"Điều hướng học viên",exact:true})).toBeVisible();
}
