import {test,expect,type Page} from "@playwright/test";

async function signIn(page:Page,role="TEACHER",status="APPROVED") {
  let eligibilityReads=0;
  await page.route("**/api/v1/**",route=>{
    const path=new URL(route.request().url()).pathname;
    if(path.endsWith("/csrf"))return route.fulfill({json:{token:"fixture"}});
    if(path.endsWith("/login"))return route.fulfill({json:{accessToken:"fixture",tokenType:"Bearer"}});
    if(path.endsWith("/logout"))return route.fulfill({status:204});
    if(path.endsWith("/users/me"))return route.fulfill({json:{id:"1",role,emailVerified:true}});
    if(path.endsWith("/teacher/onboarding")){eligibilityReads++;return route.fulfill({json:{applicationId:"1",status,submittedAt:"2026-01-01T00:00:00Z"}});}
    if(path.endsWith("/users/me/profile"))return route.fulfill({json:{id:"1",fullName:"Fixture",username:null,email:"fixture@example.invalid",emailVerified:true,phone:null}});
    if(path.endsWith("/teacher/courses"))return route.fulfill({json:{items:[],page:0,size:20,hasNext:false}});
    return route.fulfill({status:403,json:{code:"ACCESS_DENIED",message:"Denied"}});
  });
  await page.goto("/account");
  await page.getByLabel("Email",{exact:true}).fill("fixture@example.invalid");
  await page.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");
  await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();
  await expect(page.locator("[data-dashboard-layout]")).toBeVisible();
  return ()=>eligibilityReads;
}
async function menu(page:Page){const toggle=page.locator("[data-dashboard-toggle]");if(await toggle.isVisible()&&await toggle.getAttribute("aria-expanded")==="false")await toggle.click();}
async function navigate(page:Page,label:string,nav="Điều hướng Dashboard"){
 await menu(page);await page.getByRole("navigation",{name:nav,exact:true}).getByRole("link",{name:label,exact:true}).click();
 await expect(page.getByRole("heading",{level:1,name:label,exact:true})).toBeVisible();
}
test("Teacher dashboard excludes public shell and retains navigation and history",async({page},info)=>{
 const reads=await signIn(page);await navigate(page,"Tổng quan");await expect(page.getByRole("contentinfo")).toHaveCount(0);await expect(page.getByRole("navigation",{name:"Điều hướng chính",exact:true})).toHaveCount(0);
 await expect(page.locator('aside nav a[href="/teacher/courses"]')).toHaveCount(1);
 await expect(page.getByRole("heading",{name:"Lịch dạy trong 7 ngày tới",exact:true})).toBeVisible();
 const initialReads=reads();
 for(const name of ["Quản lý khóa học","Lịch dạy","Buổi học","Tài khoản nhận tiền","Hồ sơ gia sư","Cài đặt tài khoản"])await navigate(page,name);
 expect(reads()).toBe(initialReads);
 await page.goBack();await expect(page.getByRole("heading",{level:1,name:"Hồ sơ gia sư"})).toBeVisible();
 await page.goForward();await expect(page.getByRole("heading",{level:1,name:"Cài đặt tài khoản"})).toBeVisible();
 const toggle=page.locator("[data-dashboard-toggle]");if(await toggle.isVisible()){await toggle.click();await expect(page.getByRole("dialog")).toBeVisible();await page.keyboard.press("Escape");await expect(toggle).toBeFocused();await expect(toggle).toHaveAttribute("aria-expanded","false");}
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.screenshot({path:info.outputPath("unified-dashboard.png"),fullPage:true});
 await page.reload();await expect(page).toHaveURL(/\/login\?next=/);
});
for(const status of ["PENDING","REJECTED"])test(`${status} Teacher retains onboarding without teaching links`,async({page})=>{
 await signIn(page,"TEACHER",status);await menu(page);
 await expect(page.getByRole("navigation",{name:"Điều hướng Dashboard",exact:true}).getByRole("link",{name:"Quản lý khóa học"})).toHaveCount(0);
 await page.getByRole("navigation",{name:"Điều hướng Dashboard",exact:true}).getByRole("link",{name:"Trạng thái hồ sơ đăng ký"}).click();
 await expect(page.getByRole("heading",{level:1,name:"Trạng thái hồ sơ đăng ký"})).toBeVisible();
 await navigate(page,"Cài đặt tài khoản");await expect(page.locator("#change-password summary")).toBeVisible();
 await page.goto("/teacher/courses");await expect(page).toHaveURL(/\/login\?next=/);
});
test("logout clears session and anonymous Teacher routes remain protected",async({page})=>{
 await signIn(page);await menu(page);await page.getByRole("button",{name:"Đăng xuất",exact:true}).filter({visible:true}).click();
 await expect(page).toHaveURL(/\/account$/);await page.goto("/teacher/courses");await expect(page).toHaveURL(/\/login\?next=/);
});
test("Student has its own shell for enrollment and profile, without Teacher menus",async({page})=>{
 await signIn(page,"STUDENT");await menu(page);
 const nav=page.getByRole("navigation",{name:"Điều hướng học viên",exact:true});
 await expect(nav.getByRole("link")).toHaveText(["Tổng quan","Khóa học của tôi","Khám phá khóa học","Lịch học","Hồ sơ và cài đặt"]);
 await nav.getByRole("link",{name:"Khóa học của tôi",exact:true}).click();await expect(page).toHaveURL(/\/student\/enrollments$/);
 await expect(page.getByRole("heading",{level:1,name:"Khóa học của tôi"})).toBeVisible();await expect(page.getByRole("contentinfo")).toHaveCount(0);
 await navigate(page,"Hồ sơ và cài đặt","Điều hướng học viên");await expect(page.locator("#change-password summary")).toBeVisible();
});
