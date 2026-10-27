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
  if(role==="STUDENT"){await page.locator(".header-account-menu summary").click();await page.locator(".header-account-menu a[href=\"/account\"]").click();}
  await expect(page.getByRole("heading",{level:1,name:"Tài khoản & hồ sơ đăng ký"})).toBeVisible();
  return ()=>eligibilityReads;
}
async function navigate(page:Page,label:string) {
  const toggle=page.locator('button[aria-controls="dashboard-navigation"]');
  if(await toggle.isVisible()&&await toggle.getAttribute("aria-expanded")==="false")await toggle.click();
  await page.getByRole("navigation",{name:"Điều hướng Dashboard"}).getByRole("link",{name:label,exact:true}).click();
  await expect(page.getByRole("heading",{level:1,name:label,exact:true})).toBeVisible();
}
test("one persistent vertical sidebar and header across Account and Teacher routes",async({page},info)=>{
  const reads=await signIn(page);
  const nav=page.locator("#dashboard-navigation");
  await expect(nav.locator("a,button")).toHaveText(["Tổng quan","Khóa học của tôi","Lịch dạy","Buổi học","Hồ sơ gia sư","Tài khoản & hồ sơ đăng ký","Cài đặt tài khoản","Đăng xuất"]);
  await page.evaluate(()=>{document.querySelector("#dashboard-navigation")?.setAttribute("data-persistent","yes");document.querySelector("header")?.setAttribute("data-persistent","yes");});
  const initialReads=reads();
  for(const label of ["Tổng quan","Khóa học của tôi","Lịch dạy","Buổi học","Hồ sơ gia sư","Tài khoản & hồ sơ đăng ký","Cài đặt tài khoản"])await navigate(page,label);
  await expect(nav).toHaveAttribute("data-persistent","yes");
  await expect(page.locator("header").first()).toHaveAttribute("data-persistent","yes");
  expect(reads()).toBe(initialReads);
  await expect(page.getByRole("status").filter({hasText:"Đang kiểm tra quyền"})).toHaveCount(0);
  await page.goBack();await expect(page.getByRole("heading",{level:1,name:"Tài khoản & hồ sơ đăng ký"})).toBeVisible();
  await page.goForward();await expect(page.getByRole("heading",{level:1,name:"Cài đặt tài khoản"})).toBeVisible();
  const toggle=page.locator('button[aria-controls="dashboard-navigation"]');
  if(await toggle.isVisible()){await toggle.click();await nav.getByRole("link").first().focus();await page.keyboard.press("Escape");await expect(toggle).toBeFocused();await expect(toggle).toHaveAttribute("aria-expanded","false");}
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.screenshot({path:info.outputPath("unified-dashboard.png"),fullPage:true});
  await page.reload();await expect(page).toHaveURL(/\/login\?next=/);
});
for(const status of ["PENDING","REJECTED"])test(`${status} Teacher retains account panels without management links`,async({page})=>{
  await signIn(page,"TEACHER",status);
  await expect(page.locator('#dashboard-navigation a[href="/teacher/courses"]')).toHaveCount(0);
  await navigate(page,"Cài đặt tài khoản");await expect(page.locator("#change-password summary")).toBeVisible();
  await page.goto("/teacher/courses");await expect(page).toHaveURL(/\/login\?next=/);
});
test("logout clears session and anonymous Teacher routes remain protected",async({page})=>{
  await signIn(page);const toggle=page.locator('button[aria-controls="dashboard-navigation"]');
  if(await toggle.isVisible())await toggle.click();
  await page.getByRole("navigation",{name:"Điều hướng Dashboard"}).getByRole("button",{name:"Đăng xuất"}).click();
  await expect(page).toHaveURL(/\/account$/);
  await page.goto("/teacher/courses");await expect(page).toHaveURL(/\/login\?next=/);
});

test("Student sees only shared account navigation",async({page})=>{
 await signIn(page,"STUDENT");
 await expect(page.locator("#dashboard-navigation a")).toHaveText(["Tổng quan","Tài khoản & hồ sơ đăng ký","Cài đặt tài khoản"]);
 await navigate(page,"Cài đặt tài khoản");
 await expect(page.locator("#change-password summary")).toBeVisible();
});
