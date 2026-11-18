import {test,expect} from "@playwright/test";
import {captureReview} from "../support/ui-review";
import {analyticsFixture} from "../support/admin-analytics";

test("@admin-analytics Automatic totals, period filters, payment monitoring and recovery",async({page},info)=>{
 let release!:()=>void;const initial=new Promise<void>(resolve=>{release=resolve;});let first=true,fail=false;const reads:string[]=[];
 await page.route("**/api/v1/**",async route=>{
  const u=new URL(route.request().url());
  if(u.pathname.endsWith("/csrf"))return route.fulfill({json:{token:"fixture-csrf"}});
  if(u.pathname.endsWith("/login"))return route.fulfill({json:{accessToken:"admin-token",tokenType:"Bearer"}});
  if(u.pathname.endsWith("/users/me"))return route.fulfill({json:{id:"1",role:"ADMIN",emailVerified:true}});
  if(u.pathname.endsWith("/admin/analytics/overview")){
   expect(route.request().method()).toBe("GET");expect(route.request().headers().authorization).toBe("Bearer admin-token");
   const range=u.searchParams.get("range")!;reads.push(range);if(first){first=false;await initial;}
   if(fail)return route.fulfill({status:503,json:{code:"ANALYTICS_UNAVAILABLE",message:"PRIVATE"}});
   const data=analyticsFixture(range);
   if(range==="7"){data.period={users:0,enrollments:0,courses:0,confirmedPayments:0,confirmedTuition:{amount:"0.00",currency:"VND"}};data.days=[];data.courseTuition=[];data.teacherTuition=[];}
   return route.fulfill({json:data});
  }
  return route.fulfill({status:404,json:{code:"NOT_FOUND"}});
 });
 await page.goto("/login");await page.getByLabel("Email",{exact:true}).fill("admin@example.invalid");await page.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();
 await expect(page).toHaveURL(/admin\/dashboard$/);await expect(page.getByRole("status",{name:"Đang tải thống kê"})).toBeVisible();release();
 let panel=page.getByRole("region",{name:"Thống kê hệ thống"});
 await expect(panel.locator("dt",{hasText:"Tài khoản đã đăng ký"}).locator("..").locator("dd")).toHaveText("12");
 await expect(panel.locator("dt",{hasText:"Tổng học phí đã xác nhận"}).locator("..").locator("dd")).toHaveText("1.000.000,25 ₫");
 await expect(panel.getByRole("table")).toContainText("18/11/2026");expect(reads).toEqual(["30"]);
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await captureReview(page,info,"admin-dashboard");
 const toggle=page.locator("[data-dashboard-toggle]");if(await toggle.isVisible())await toggle.click();
 await page.getByRole("link",{name:"Thống kê giao dịch",exact:true}).click();await expect(page).toHaveURL(/section=statistics$/);
 panel=page.getByRole("region",{name:"Thống kê học phí"});
 await expect(panel.getByRole("table",{name:"Khóa học và gia sư nhận học phí"})).toContainText("Gia sư kiểm thử");
 await expect(panel.getByRole("button",{name:/Xác nhận thanh toán|Chỉnh sửa|Xuất bản/})).toHaveCount(0);
 await expect(panel.locator('a[href*="meet.google.com"]')).toHaveCount(0);
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);await captureReview(page,info,"admin-payment-statistics");
 await panel.getByRole("combobox",{name:"Khoảng thời gian"}).selectOption("7");await expect(panel.getByText("Chưa có hoạt động trong khoảng thời gian này.")).toBeVisible();
 await expect(panel.getByText("Chưa có học phí được xác nhận trong kỳ.")).toBeVisible();
 fail=true;await panel.getByRole("combobox",{name:"Khoảng thời gian"}).selectOption("90");await expect(panel.getByRole("alert")).toBeVisible();await expect(page.getByText("PRIVATE",{exact:true})).toHaveCount(0);
 fail=false;await panel.getByRole("button",{name:"Thử lại thống kê"}).click();await expect(panel.getByRole("table",{name:"Khóa học và gia sư nhận học phí"})).toBeVisible();
 await panel.getByRole("combobox",{name:"Khoảng thời gian"}).selectOption("all");await expect(panel.getByText("Chọn 7, 30 hoặc 90 ngày để xem chi tiết hoạt động theo ngày.")).toBeVisible();
 expect(reads).toEqual(["30","30","7","90","90","all"]);
 for(const heading of await panel.getByRole("heading").all()){const box=await heading.boundingBox();expect(box!.width).toBeGreaterThan(200);}
});
