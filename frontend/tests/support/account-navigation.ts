import type { Page } from "@playwright/test";

export async function accountPanel(page: Page, name: string) {
  if(new URL(page.url()).pathname.startsWith("/admin")){
    const toggle=page.locator("[data-dashboard-toggle]");if(await toggle.isVisible())await toggle.click();
    await page.getByRole("navigation",{name:"Điều hướng quản trị",exact:true}).getByRole("link",{name,exact:true}).click();return;
  }
  const student=page.getByRole("navigation",{name:"Điều hướng học viên", includeHidden:true});
  if(await student.count()){
    const toggle=page.locator("[data-dashboard-toggle]");if(await toggle.isVisible())await toggle.click();
    await page.getByRole("navigation",{name:"Điều hướng học viên"}).getByRole("link",{name:"Hồ sơ và cài đặt",exact:true}).click();return;
  }
  const label = name === "Đổi mật khẩu" || name === "Hồ sơ cá nhân" ? "Cài đặt tài khoản" : name === "Xét duyệt gia sư" ? "Tài khoản & hồ sơ đăng ký" : name;
  const toggle = page.locator('[data-dashboard-toggle]');
  await toggle.waitFor({ state: "attached" });
  if (await toggle.isVisible() && await toggle.getAttribute("aria-expanded") === "false") await toggle.click();
  await page.getByRole("navigation", { name: "Điều hướng Dashboard", exact: true }).getByRole("link", { name: label, exact: true }).click();
}
