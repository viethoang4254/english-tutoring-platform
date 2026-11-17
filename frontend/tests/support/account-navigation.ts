import { expect, type Page } from "@playwright/test";
import { openStudentNavigation } from "./student-navigation";

export async function accountPanel(page: Page, name: string) {
  // Profile loading temporarily unmounts navigation; absence is not a Teacher role.
  await expect(page.getByRole("navigation", {
    name: /^(Điều hướng học viên|Điều hướng Dashboard|Điều hướng quản trị)$/,
    includeHidden: true,
  }).first()).toBeAttached();
  if(new URL(page.url()).pathname.startsWith("/admin")){
    const toggle=page.locator("[data-dashboard-toggle]");
    if(await toggle.isVisible() && await toggle.getAttribute("aria-expanded")==="false")await toggle.click();
    await page.getByRole("navigation",{name:"Điều hướng quản trị",exact:true}).getByRole("link",{name,exact:true}).click();return;
  }
  const student=page.getByRole("navigation",{name:"Điều hướng học viên",exact:true,includeHidden:true});
  if(await student.count()){
    if(new URL(page.url()).pathname!=="/account"){
      await openStudentNavigation(page);
      await page.getByRole("navigation",{name:"Điều hướng học viên",exact:true})
        .getByRole("link",{name:"Hồ sơ và cài đặt",exact:true}).click();
    }
    await expect(page).toHaveURL(/\/account(?:[?#]|$)/);
    await expect(page.getByRole("heading",{name:"Hồ sơ và cài đặt",exact:true})).toBeVisible();
    return;
  }
  const label = name === "Đổi mật khẩu" || name === "Hồ sơ cá nhân" ? "Cài đặt tài khoản" : name === "Xét duyệt gia sư" ? "Tài khoản & hồ sơ đăng ký" : name;
  const toggle = page.locator("[data-dashboard-toggle]");
  if (await toggle.isVisible() && await toggle.getAttribute("aria-expanded") === "false") await toggle.click();
  const link=page.getByRole("navigation", { name: "Điều hướng Dashboard", exact: true }).getByRole("link", { name: label, exact: true });
  const href=await link.getAttribute("href");
  expect(href).toBeTruthy();
  await link.click();
  await expect(page).toHaveURL(new URL(href!,page.url()).href);
}
