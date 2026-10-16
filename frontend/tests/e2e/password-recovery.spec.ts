import { expect, test } from "@playwright/test";

for (const entry of ["modal", "page"]) test(`forgot password from ${entry} completes without automatic login`, async ({ page }) => {
  const calls: { path: string; body: unknown }[] = [];
  await page.route("**/api/v1/**", async route => {
    const request=route.request(); const path=new URL(request.url()).pathname;
    if(request.method()==="OPTIONS") {await route.fulfill({status:204});return;}
    calls.push({path,body:request.postData()?request.postDataJSON():null});
    if(path.endsWith("/csrf")) await route.fulfill({json:{token:"csrf-test"}});
    else if(path.endsWith("/forgot")) await route.fulfill({status:202});
    else if(path.endsWith("/reset")) await route.fulfill({status:204});
    else await route.abort();
  });
  await page.goto(entry==="modal"?"/":"/login");
  if(entry==="modal") await page.getByRole("button",{name:"Tài khoản",exact:true}).click();
  await page.getByRole("link",{name:"Quên mật khẩu?"}).click();
  await expect(page).toHaveURL(/\/forgot-password$/);await page.reload();
  await page.getByLabel("Email",{exact:true}).fill("student@example.test");
  await page.getByRole("button",{name:"Gửi hướng dẫn"}).click();
  await expect(page.getByRole("status")).toContainText("Nếu tài khoản đủ điều kiện");
  await page.getByRole("link",{name:"Nhập mã và đặt lại mật khẩu"}).click();await expect(page).toHaveURL(/\/reset-password$/);await page.reload();
  await page.getByLabel("Email",{exact:true}).fill("student@example.test");await page.getByLabel("Mã đặt lại mật khẩu",{exact:true}).fill("000123");
  await page.getByLabel("Mật khẩu",{exact:true}).fill(" new password ");
  await page.getByLabel("Xác nhận mật khẩu",{exact:true}).fill(" new password ");
  await page.getByRole("button",{name:"Lưu mật khẩu mới"}).evaluate((button:HTMLButtonElement)=>{button.click();button.click();});
  await expect(page).toHaveURL(/\/login\?reset=1$/);
  await expect(page.getByRole("status")).toContainText("Mật khẩu đã được cập nhật");
  expect(calls.filter(c=>c.path.endsWith("/reset"))).toEqual([{path:"/api/v1/auth/password/reset",body:{email:"student@example.test",code:"000123",password:" new password "}}]);
  expect(calls.some(c=>c.path.endsWith("/login")||c.path.endsWith("/refresh"))).toBe(false);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});

for (const status of [400,403,500,503]) test(`reset error ${status} stays safe and is never retried`, async ({page})=>{
  let requests=0;
  await page.route("**/api/v1/**",async route=>{
    if(route.request().method()==="OPTIONS") {await route.fulfill({status:204});return;}
    if(route.request().url().endsWith("/csrf")) await route.fulfill({json:{token:"csrf-test"}});
    else {requests++;await route.fulfill({status,json:{code:status===400?"INVALID_RESET_CREDENTIAL":"ERROR",message:"PRIVATE SQL"}});}
  });
  await page.goto("/reset-password");await page.getByLabel("Email",{exact:true}).fill("student@example.test");await page.getByLabel("Mã đặt lại mật khẩu",{exact:true}).fill("999999");
  await page.getByLabel("Mật khẩu",{exact:true}).fill("new password");await page.getByLabel("Xác nhận mật khẩu",{exact:true}).fill("different");
  await page.getByRole("button",{name:"Lưu mật khẩu mới"}).click();expect(requests).toBe(0);
  await expect(page.getByRole("status")).toContainText("không khớp");
  await page.getByLabel("Xác nhận mật khẩu",{exact:true}).fill("new password");await page.getByRole("button",{name:"Lưu mật khẩu mới"}).click();
  await expect(page.getByRole("status")).not.toBeEmpty();await expect(page.getByText("PRIVATE SQL")).toHaveCount(0);expect(requests).toBe(1);
  await expect(page).toHaveURL(/\/reset-password$/);
});
