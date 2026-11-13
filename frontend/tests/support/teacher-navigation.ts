import { expect, type Page } from "@playwright/test";
export async function openTeacher(page: Page) {
 if(new URL(page.url()).pathname.startsWith("/teacher"))return;
 const toggle=page.locator("[data-dashboard-toggle]");
 await expect(toggle).toBeAttached();
 if(await toggle.isVisible())await toggle.click();
 await page.getByRole("navigation",{name:"Điều hướng Dashboard",exact:true}).getByRole("link",{name:"Tổng quan",exact:true}).click();
 await expect(page.locator("[data-dashboard-layout]")).toBeVisible();
}
export async function teacherPanel(page:Page,name:string){
 const toggle=page.locator("[data-dashboard-toggle]");
 await expect(toggle).toBeAttached();
 if(await toggle.isVisible()&&await toggle.getAttribute("aria-expanded")==="false")await toggle.click();
 const label=name==="Khóa học của tôi"?"Quản lý khóa học":name==="Tài khoản & hồ sơ đăng ký"?"Trạng thái hồ sơ đăng ký":name;
 const link=page.getByRole("navigation",{name:"Điều hướng Dashboard",exact:true}).getByRole("link",{name:label,exact:true});
 const href=await link.getAttribute("href");await link.click();await expect(page).toHaveURL(new URL(href!,page.url()).href);
 await expect(page.getByRole("heading",{level:1,name:label,exact:true})).toBeVisible();
}
