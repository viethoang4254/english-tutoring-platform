import {test,expect} from "@playwright/test";
import {teacherPanel} from "../support/teacher-navigation";
for(const mode of ["sessions","empty","error","denied"])test(`Calendar ${mode} month week and responsive navigation`,async({page})=>{
 const calls:string[]=[];let failed=mode==="error";
 await page.route("**/api/v1/**",async r=>{
  const u=new URL(r.request().url()),path=u.pathname;
  if(path.endsWith("/csrf"))return r.fulfill({json:{token:"fixture"}});
  if(path.endsWith("/login"))return r.fulfill({json:{accessToken:"fixture",tokenType:"Bearer"}});
  if(path.endsWith("/users/me"))return r.fulfill({json:{id:"1",role:"TEACHER",emailVerified:true}});
  if(path.endsWith("/onboarding"))return r.fulfill({json:{applicationId:"1",status:"APPROVED"}});
  if(path.endsWith("/profile"))return r.fulfill({json:{id:"1",fullName:"Fixture",email:"fixture@example.invalid",emailVerified:true}});
  if(path.endsWith("/calendar/sessions")){
   calls.push(u.search);expect(r.request().headers().authorization).toBe("Bearer fixture");expect(r.request().method()).toBe("GET");
   if(failed||mode==="denied")return r.fulfill({status:mode==="denied"?403:503,json:{code:"COURSE_UNAVAILABLE",message:"PRIVATE"}});
   const start=new Date(new Date(u.searchParams.get("from")!).getTime()+12*3600000).toISOString();
   return r.fulfill({json:{items:mode==="empty"?[]:[{sessionId:"9007199254740993",courseId:"42",courseTitle:"Owned Calendar Course",sessionNumber:1,sessionTitle:"Speaking",startsAt:start,endsAt:new Date(new Date(start).getTime()+3600000).toISOString(),courseTimezone:"Europe/Paris",status:"CANCELLED"}],page:Number(u.searchParams.get("page")),size:100,hasNext:u.searchParams.get("page")==="0"&&mode==="sessions"}});
  }
  if(path.endsWith("/courses"))return r.fulfill({json:{items:[],page:0,size:20,hasNext:false}});
  return r.fulfill({status:404,json:{code:"COURSE_NOT_FOUND"}});
 });
 await page.goto("/login");await page.getByLabel("Email",{exact:true}).fill("fixture@example.invalid");await page.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();
 await expect(page).toHaveURL(/\/teacher$/);await teacherPanel(page,"Lịch dạy");await expect(page).toHaveURL(/\/teacher\/calendar$/);
 const calendar=page.getByRole("region",{name:"Lịch dạy của tôi"});
 if(mode==="denied"){await expect(calendar.getByRole("alert")).toBeVisible();await expect(page.getByText("PRIVATE")).toHaveCount(0);return;}
 if(mode==="error"){await expect(calendar.getByRole("alert")).toBeVisible();failed=false;await calendar.getByRole("button",{name:"Tải lại"}).click();}
 await expect(calendar.locator('[aria-label="Lịch tháng"]')).toBeVisible();
 if(mode==="empty")await expect(calendar.getByRole("status")).toContainText("Không có buổi học");
 else {await expect(calendar.getByText("Owned Calendar Course")).toBeVisible();await expect(calendar.getByText("Đã hủy",{exact:true})).toBeVisible();await expect(calendar.getByText("Europe/Paris",{exact:true})).toBeVisible();}
 if(mode==="sessions"){await calendar.getByRole("button",{name:"Trang sau",exact:true}).click();await expect.poll(()=>calls.at(-1)).toContain("page=1");}
 await calendar.getByRole("button",{name:"Tuần",exact:true}).click();await expect(calendar.locator('[aria-label="Lịch tuần"]')).toBeVisible();await expect.poll(()=>calls.at(-1)).toContain("page=0");
 const count=calls.length;await calendar.getByRole("button",{name:"Kỳ sau",exact:true}).click();await expect.poll(()=>calls.length).toBeGreaterThan(count);
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.reload();await expect(page).toHaveURL(/\/login\?next=/);
});
