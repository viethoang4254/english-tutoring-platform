import { expect, test } from "@playwright/test";
import { googleFixture, openGoogle } from "../support/google-calendar";
import { captureReview } from "../support/ui-review";

const cases = [
  ["STUDENT","SYNCED","Đã đồng bộ Google Calendar"],
  ["STUDENT","PENDING","Đang đồng bộ lịch học"],
  ["STUDENT","FAILED","Đồng bộ thất bại, vui lòng thử lại"],
  ["TEACHER","SYNCED","Đã đồng bộ Google Calendar"],
] as const;
for (const [role,status,message] of cases) {
  test(`@calendar-sync-review ${role} ${status}`,async ({page},info)=>{
    const fixture=await googleFixture(page,role);
    fixture.status="CONNECTED";
    fixture.sync={state:status,pending:Number(status==="PENDING"),failed:Number(status==="FAILED"),
      lastSuccess:status==="SYNCED"?"2026-10-18T01:30:00Z":null};
    const panel=await openGoogle(page,role);
    await expect(panel.getByText(message,{exact:true})).toBeVisible();
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    await captureReview(page,info,`${role.toLowerCase()}-sync-${status.toLowerCase()}`);
    if(status==="FAILED"){
      await panel.getByRole("button",{name:"Thử đồng bộ lại"}).click();
      await expect(panel.getByText("Đang đồng bộ lịch học",{exact:true})).toBeVisible();
      expect(fixture.syncRetries).toBe(1);
    }
  });
}

test("connection backfill is pending until the server reports success",async ({page})=>{
  const fixture=await googleFixture(page);
  const panel=await openGoogle(page);
  await expect(panel.getByText("Chưa kết nối",{exact:true})).toBeVisible();
  fixture.sync={state:"PENDING",pending:2,failed:0,lastSuccess:null};
  await panel.getByRole("button",{name:"Kết nối Google Calendar",exact:true}).click();
  await expect(page).toHaveURL(/\/account\?googleCalendar=CONNECTED$/);
  await page.locator("summary").filter({hasText:"Kết nối Google Calendar"}).click();
  const account=page.getByRole("region",{name:"Kết nối Google Calendar",exact:true});
  await expect(account.getByText("Đang đồng bộ lịch học",{exact:true})).toBeVisible();
  await expect(account.getByText("Đã đồng bộ Google Calendar",{exact:true})).toHaveCount(0);
  fixture.sync={state:"SYNCED",pending:0,failed:0,lastSuccess:"2026-10-18T02:00:00Z"};
  await account.getByRole("button",{name:"Tải lại kết nối"}).click();
  await expect(account.getByText("Đã đồng bộ Google Calendar",{exact:true})).toBeVisible();
});
test("revoked authorization requires reconnect and cannot offer sync retry",async ({page})=>{
  const fixture=await googleFixture(page);fixture.status="RECONNECT_REQUIRED";
  const panel=await openGoogle(page);
  await expect(panel.getByText("Cần kết nối lại",{exact:true})).toBeVisible();
  await expect(panel.getByRole("button",{name:"Kết nối lại Google Calendar"})).toBeEnabled();
  await expect(panel.getByRole("button",{name:"Thử đồng bộ lại"})).toHaveCount(0);
  expect(fixture.syncRetries).toBe(0);
});
test("ambiguous calendar provisioning cannot silently create another calendar",async ({page})=>{
  const fixture=await googleFixture(page);fixture.status="CONNECTED";
  fixture.sync={state:"REVIEW_REQUIRED",pending:0,failed:0,lastSuccess:null};
  const panel=await openGoogle(page);
  await expect(panel).toContainText("Chưa xác định được lịch Google.");
  await expect(panel.getByRole("button",{name:"Thử đồng bộ lại"})).toHaveCount(0);
  expect(fixture.syncRetries).toBe(0);
});
test("sync retry failure remains visible and does not claim success",async ({page})=>{
  const fixture=await googleFixture(page);fixture.status="CONNECTED";
  fixture.sync={state:"FAILED",pending:0,failed:1,lastSuccess:null};
  const panel=await openGoogle(page);
  await expect(panel.getByText("Đồng bộ thất bại, vui lòng thử lại",{exact:true})).toBeVisible();
  await page.route("**/api/v1/integrations/google-calendar/sync",route=>route.fulfill({status:503,json:{code:"UNAVAILABLE",message:"PRIVATE"}}));
  await panel.getByRole("button",{name:"Thử đồng bộ lại"}).click();
  await expect(panel.getByRole("alert")).toHaveText("Không thể yêu cầu đồng bộ. Vui lòng thử lại.");
  await expect(panel.getByText("Đã đồng bộ Google Calendar",{exact:true})).toHaveCount(0);
});
