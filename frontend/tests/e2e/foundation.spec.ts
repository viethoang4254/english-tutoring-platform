import { expect, test } from "@playwright/test";

test("neutral foundation renders without overflow or browser errors", async ({ page }) => {
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));

  const response = await page.goto("/");
  expect(response?.status()).toBe(200);
  await expect(page.getByRole("heading", { name: "Frontend foundation" })).toBeVisible();
  await expect(page.getByText("Technical startup page. Product UI/UX is not yet implemented.")).toBeVisible();
  const fitsViewport = await page.evaluate(
    () => document.documentElement.scrollWidth <= window.innerWidth,
  );
  expect(fitsViewport).toBe(true);
  expect(pageErrors).toEqual([]);
});
