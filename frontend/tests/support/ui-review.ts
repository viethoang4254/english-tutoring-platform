import { join } from "node:path";
import type { Page, TestInfo } from "@playwright/test";

export async function captureReview(page: Page, info: TestInfo, name: string) {
  const directory = process.env.TASK009_UI_IMAGES;
  if (!directory) return;
  await page.evaluate(() => document.fonts.ready.then(() => undefined));
  await page.screenshot({path: join(directory, name + "-" + info.project.name + ".png"), fullPage: true, animations: "disabled"});
}
