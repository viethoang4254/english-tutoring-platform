import {test,expect,type Locator} from "@playwright/test";
import {captureReview} from "../support/ui-review";
import {analyticsFixture} from "../support/admin-analytics";

async function containedTuitionKpi(panel:Locator) {
 const card=panel.locator("dt").filter({hasText:/^Học phí xác nhận trong kỳ$/}).locator("..");
 await expect(card.locator("dd")).toHaveText("1.000.000,25 ₫");
 const layout=await card.evaluate(element=>{
  const value=element.querySelector("dd span")!, content=element.querySelector("dd")!;
  const box=value.getBoundingClientRect(),bounds=content.getBoundingClientRect();
  const range=document.createRange();range.selectNodeContents(value);
  return {left:box.left,right:box.right,availableLeft:bounds.left,availableRight:bounds.right,
   lines:range.getClientRects().length,scroll:element.scrollWidth,width:element.clientWidth};
 });
 expect(layout.left).toBeGreaterThanOrEqual(layout.availableLeft);
 expect(layout.right).toBeLessThanOrEqual(layout.availableRight+1);
 expect(layout.scroll).toBeLessThanOrEqual(layout.width);
 expect(layout.lines).toBe(1);
}
async function readableTables(panel:Locator, expected:number) {
 const tables=panel.getByRole("table");await expect(tables).toHaveCount(expected);
 for(const table of await tables.all()) {
  const caption=table.locator("caption");await expect(caption).toBeVisible();
  const dimensions=await caption.evaluate(element=>{
   const box=element.getBoundingClientRect(), table=element.closest("table")!.getBoundingClientRect();
   const style=getComputedStyle(element);
   return {width:box.width,height:box.height,tableWidth:table.width,lineHeight:parseFloat(style.lineHeight)||parseFloat(style.fontSize)*1.5};
  });
  expect(dimensions.width).toBeGreaterThan(200);
  expect(dimensions.width).toBeGreaterThanOrEqual(dimensions.tableWidth-2);
  expect(dimensions.height).toBeLessThanOrEqual(dimensions.lineHeight*4);
  for(const value of await table.locator("tbody span").all()) {
   const box=await value.evaluate(element=>{const range=document.createRange();range.selectNodeContents(element);return {lines:range.getClientRects().length,width:element.getBoundingClientRect().width,parentWidth:element.parentElement!.getBoundingClientRect().width};});
   expect(box.lines).toBe(1);expect(box.width).toBeLessThanOrEqual(box.parentWidth);
  }
  await expect(table.locator("tbody tr")).toHaveCount(1);
  await expect(table.locator("tbody tr").first()).toContainText("1.000.000,25 ₫");
 }
}
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
   if(range==="7"){data.period={users:0,enrollments:0,courses:0,confirmedPayments:0,confirmedTuition:{amount:"0.00",currency:"VND"}};data.days=[];data.courseTuition=[];data.teacherTuition=[];for(const key of Object.keys(data.totals) as (keyof typeof data.totals)[]){if(key!=="confirmedTuition")data.totals[key]=0;}data.totals.confirmedTuition={amount:"0.00",currency:"VND"};}
   return route.fulfill({json:data});
  }
  return route.fulfill({status:404,json:{code:"NOT_FOUND"}});
 });
 await page.goto("/login");await page.getByLabel("Email",{exact:true}).fill("admin@example.invalid");await page.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();
 await expect(page).toHaveURL(/admin\/dashboard$/);await expect(page.getByRole("status",{name:"Đang tải thống kê"})).toBeVisible();release();
 let panel=page.getByRole("region",{name:"Thống kê hệ thống"});
 await expect(panel.getByRole("img",{name:"Trạng thái khóa học: 6",exact:true})).toBeVisible();
 await expect(panel.getByRole("region",{name:"Trạng thái khóa học",exact:true})).toContainText("66,7%");
 await expect(panel.getByRole("img",{name:"Trạng thái thanh toán: 13",exact:true})).toBeVisible();
 for(const name of ["Trạng thái khóa học: 6","Trạng thái thanh toán: 13"]){
  const box=await panel.getByRole("img",{name,exact:true}).boundingBox();expect(box!.width).toBe(info.project.name==="desktop"?180:160);
 }
 await expect(panel.getByRole("region",{name:"Trạng thái thanh toán",exact:true})).toContainText("61,5%");
 await panel.locator("summary").click();
 await expect(panel.locator("dt",{hasText:"Tài khoản đã đăng ký"}).locator("..").locator("dd")).toHaveText("12");
 await expect(panel.locator("dt",{hasText:"Tổng học phí đã xác nhận"}).locator("..").locator("dd")).toHaveText("1.000.000,25 ₫");
 await expect(panel.getByRole("table")).toContainText("18/11/2026");expect(reads).toEqual(["30"]);
 await panel.locator("summary").click();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await readableTables(panel,1);
 await containedTuitionKpi(panel);
 await expect(panel.getByRole("img",{name:/Xu hướng đăng ký mới/})).toBeVisible();
 await expect(panel.locator("figure svg circle")).toHaveCount(3);
 await expect(panel.locator("figure svg circle title").first()).toHaveText("2026-11-18: Đăng ký mới 3");
 await captureReview(page,info,"admin-dashboard");
 await panel.getByRole("button",{name:"7 ngày",exact:true}).click();
 await expect(panel.getByRole("img",{name:"Trạng thái khóa học: 0",exact:true})).toBeVisible();
 await expect(panel.getByText("Chưa có dữ liệu.", {exact:true})).toHaveCount(2);
 await expect(panel.getByRole("region",{name:"Trạng thái thanh toán",exact:true})).not.toContainText("NaN");
 const toggle=page.locator("[data-dashboard-toggle]");if(await toggle.isVisible())await toggle.click();
 await page.getByRole("link",{name:"Thống kê giao dịch",exact:true}).click();await expect(page).toHaveURL(/section=statistics$/);
 panel=page.getByRole("region",{name:"Thống kê học phí"});
 await expect(panel.getByRole("table",{name:"Khóa học và gia sư nhận học phí"})).toContainText("Gia sư kiểm thử");
 await expect(panel.getByRole("button",{name:/Xác nhận thanh toán|Chỉnh sửa|Xuất bản/})).toHaveCount(0);
 await expect(panel.locator('a[href*="meet.google.com"]')).toHaveCount(0);
 await readableTables(panel,3);
 await containedTuitionKpi(panel);
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);await captureReview(page,info,"admin-payment-statistics");
 await panel.getByRole("button",{name:"7 ngày",exact:true}).click();await expect(panel.getByText("Chưa có hoạt động trong khoảng thời gian này.")).toBeVisible();
 await expect(panel.getByText("Chưa có học phí được xác nhận trong kỳ.")).toBeVisible();
 fail=true;await panel.getByRole("button",{name:"90 ngày",exact:true}).click();await expect(panel.getByRole("alert")).toBeVisible();await expect(page.getByText("PRIVATE",{exact:true})).toHaveCount(0);
 fail=false;await panel.getByRole("button",{name:"Thử lại thống kê"}).click();await expect(panel.getByRole("table",{name:"Khóa học và gia sư nhận học phí"})).toBeVisible();
 await panel.getByRole("button",{name:"Toàn bộ",exact:true}).click();await expect(panel.getByText("Chọn 7, 30 hoặc 90 ngày để xem chi tiết hoạt động theo ngày.")).toBeVisible();
 expect(reads).toEqual(["30","7","30","7","90","90","all"]);
 for(const heading of await panel.getByRole("heading").all()){const box=await heading.boundingBox();expect(box!.width).toBeGreaterThan(200);}
});
