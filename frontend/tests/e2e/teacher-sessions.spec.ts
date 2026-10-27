import { teacherPanel, openTeacher } from "../support/teacher-navigation";
import {test,expect,type Page} from "@playwright/test";
const course={id:"9007199254740993",name:"Course fixture",status:"DRAFT",category:{id:"1",name:"Category",active:true},description:"Text",tuition:{amount:"0.00",currency:"VND"},plannedStartDate:"2030-01-01",timezone:"Asia/Ho_Chi_Minh",sessionCount:1,minStudents:1,maxStudents:2};
async function fixture(page:Page,generated=true,error=0){
  let savedCourse={...course};
  let lesson={id:"9007199254740994",courseId:course.id,sessionNumber:1,title:"Title",content:"<b>plain text</b>",status:"SCHEDULED",scheduledStartAt:"2030-01-01T12:00:00Z",scheduledEndAt:"2030-01-01T13:00:00Z",updatedAt:"2029-01-01T00:00:00Z",cancellationReason:null as string|null};
  let rules=[{dayOfWeek:2,startTime:"19:00",endTime:"20:00"}],writes=0;
  const bodies:Record<string,unknown>[]=[];
  await page.route("**/api/v1/**",async route=>{
    const req=route.request(),url=new URL(req.url()),path=url.pathname;
    const send=(json:unknown)=>route.fulfill({json});
    if(path.endsWith("/csrf"))return send({token:"fixture"});if(path.endsWith("/login"))return send({accessToken:"fixture",tokenType:"Bearer"});
    if(path.endsWith("/users/me"))return send({id:"1",role:"TEACHER",emailVerified:true});
    if(path.endsWith("/teacher/onboarding"))return send({applicationId:"1",status:"APPROVED"});
    if(path.includes("/teacher/courses")){
      expect(req.headers().authorization).toBe("Bearer fixture");
      if(req.method()!=="GET"){
        writes++;if(error)return error===1?route.abort():route.fulfill({status:error,json:{code:error===409?"SESSION_CHANGED":"ACCESS_DENIED",message:"Safe"}});
        const body=req.postData()?req.postDataJSON():{};bodies.push(body);
        if(path.endsWith(course.id)){savedCourse={...savedCourse,...body};return send(savedCourse);}
        if(path.endsWith("/schedule"))rules=body.rules;
        else if(path.endsWith("/generate")){expect(req.postData()).toBeNull();generated=true;return send({courseId:course.id,generatedSessionCount:1});}
        else if(path.endsWith("/reschedule")){expect(body.expectedStartAt).toBe(lesson.scheduledStartAt);expect(body.expectedEndAt).toBe(lesson.scheduledEndAt);lesson={...lesson,scheduledStartAt:body.scheduledStartAt,scheduledEndAt:body.scheduledEndAt,updatedAt:"2029-02-01T00:00:00Z"};}
        else if(path.endsWith("/cancel"))lesson={...lesson,status:"CANCELLED",cancellationReason:body.reason,updatedAt:"2029-03-01T00:00:00Z"};
        else lesson={...lesson,...body,updatedAt:"2029-01-02T00:00:00Z"};
      }
      if(path.endsWith("/schedule"))return send({courseId:course.id,plannedStartDate:course.plannedStartDate,timezone:course.timezone,sessionCount:1,generatedSessionCount:generated?1:0,rules});
      if(path.endsWith("/schedule-changes")){const n=Number(url.searchParams.get("page"));return send({items:n?[]:[{id:"901",sessionId:lesson.id,oldStartAt:"2030-01-01T12:00:00Z",oldEndAt:"2030-01-01T13:00:00Z",newStartAt:lesson.scheduledStartAt,newEndAt:lesson.scheduledEndAt,changedAt:"2029-02-01T00:00:00Z"}],page:n,size:20,hasNext:n===0});}
      if(path.endsWith("/sessions")){const n=Number(url.searchParams.get("page")),status=url.searchParams.get("status");return send({items:generated&&!n&&(!status||status===lesson.status)?[lesson]:[],page:n,size:20,hasNext:n===0});}
      if(path.includes("/sessions/"))return send(lesson);
      if(path.endsWith(course.id))return send(savedCourse);
      return send({items:[course],page:0,size:20,hasNext:false});
    }
    return route.fulfill({status:404,json:{code:"NOT_FOUND"}});
  });
  await page.goto("/account");await page.getByLabel("Email",{exact:true}).fill("teacher@example.invalid");await page.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();
  await openTeacher(page); await teacherPanel(page,"Khóa học của tôi");
  await page.getByRole("button",{name:"Tải lại khóa học",exact:true}).click();await page.getByRole("button",{name:"Xem chi tiết",exact:true}).click();await page.getByRole("button",{name:"Quản lý lịch & buổi học"}).click();await page.getByRole("button",{name:"Tải lại lịch và buổi học"}).click();
  return {writes:()=>writes,bodies};
}
test("weekly validation and confirmed initial generation",async({page})=>{
  const f=await fixture(page,false);await page.getByRole("button",{name:"Thêm quy tắc"}).click();
  await page.getByRole("group",{name:"Quy tắc lịch tuần",exact:true}).getByRole("combobox").nth(1).selectOption("2");await page.getByRole("button",{name:"Lưu lịch tuần"}).click();await expect(page.getByRole("region",{name:"Lịch và buổi học",exact:true}).getByRole("alert")).toContainText("chồng lấn");expect(f.writes()).toBe(0);
  await page.getByLabel("Bắt đầu 2",{exact:true}).fill("20:00");await page.getByLabel("Kết thúc 2",{exact:true}).fill("21:00");await page.getByRole("button",{name:"Lưu lịch tuần"}).click();await expect(page.getByRole("region",{name:"Lịch và buổi học",exact:true}).getByRole("alert")).toContainText("Đã lưu");
  await page.getByRole("button",{name:"Tạo các buổi học"}).click();await page.getByRole("button",{name:"Quay lại"}).click();expect(f.writes()).toBe(1);
  await page.getByRole("button",{name:"Tạo các buổi học"}).click();await page.getByRole("button",{name:"Xác nhận",exact:true}).click();await expect(page.getByRole("region",{name:"Lịch và buổi học",exact:true}).getByRole("alert")).toContainText("Đã tạo 1");await expect(page.getByRole("button",{name:"Lưu lịch tuần"})).toBeDisabled();expect(f.writes()).toBe(2);
});
test("detail null omission reschedule history cancellation and responsive layout",async({page},info)=>{
  const f=await fixture(page);await page.getByRole("button",{name:"Buổi học: trang sau"}).click();await expect(page.getByText("Không có buổi học trên trang này.")).toBeVisible();await page.getByRole("button",{name:"Buổi học: trang trước"}).click();
  await page.getByLabel("Trạng thái buổi học").selectOption("CANCELLED");await expect(page.getByText("Không có buổi học trên trang này.")).toBeVisible();await page.getByLabel("Trạng thái buổi học").selectOption("");
  await page.getByRole("button",{name:"Xem buổi 1"}).click();await expect(page.getByRole("paragraph").filter({hasText:"<b>plain text</b>"})).toBeVisible();
  await page.getByLabel("Tiêu đề buổi học",{exact:true}).fill("");await page.getByRole("button",{name:"Lưu nội dung"}).click();await expect(page.getByRole("region",{name:"Lịch và buổi học",exact:true}).getByRole("alert")).toContainText("Đã cập nhật");expect(f.bodies[0]).toEqual({title:null});
  await page.getByLabel("Bắt đầu mới").fill("2030-01-02T19:00");await page.getByLabel("Kết thúc mới").fill("2030-01-02T20:00");await page.getByRole("button",{name:"Đổi lịch buổi học"}).click();await expect(page.getByLabel("Bắt đầu mới")).toHaveValue("2030-01-02T19:00");expect(f.bodies[1].scheduledStartAt).toBe("2030-01-02T12:00:00.000Z");
  await page.getByRole("button",{name:"Xem lịch sử đổi lịch"}).click();await expect(page.getByText("Cũ:",{exact:false})).toBeVisible();await page.getByRole("button",{name:"Lịch sử: trang sau"}).click();await expect(page.getByText("Chưa có thay đổi lịch.")).toBeVisible();
  await page.screenshot({path:info.outputPath("teacher-sessions.png"),fullPage:true});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.getByLabel("Lý do hủy",{exact:false}).fill("Unavailable");await page.getByRole("button",{name:"Hủy buổi học",exact:true}).click();await expect(page.getByRole("dialog")).toContainText("không tạo buổi học thay thế");await page.getByRole("button",{name:"Xác nhận",exact:true}).click();await expect(page.getByRole("button",{name:"Hủy buổi học",exact:true})).toBeDisabled();await expect(page.getByText("Lý do hủy: Unavailable")).toBeVisible();
});
for(const error of [1,409,403,401])test(`mutation ${error} blocks unsafe subsequent writes`,async({page})=>{
  const f=await fixture(page,true,error);await page.getByRole("button",{name:"Xem buổi 1"}).click();await page.getByLabel("Tiêu đề buổi học",{exact:true}).fill("Changed");await page.getByRole("button",{name:"Lưu nội dung"}).click();
  if(error===1||error===409){await expect(page.getByRole("region",{name:"Lịch và buổi học",exact:true}).getByRole("alert")).toContainText(error===1?"Chưa xác nhận":"đã thay đổi");await expect(page.getByRole("button",{name:"Lưu nội dung"})).toBeDisabled();await page.getByRole("button",{name:"Tải lại lịch và buổi học"}).click();await expect(page.getByRole("button",{name:"Lưu nội dung"})).toBeEnabled();}
  else await expect(page.getByRole("heading",{name:"Chi tiết buổi 1"})).toHaveCount(0);
  expect(f.writes()).toBe(1);
});

test("sidebar preserves selected Course and unsaved Session content",async({page},info)=>{
  await fixture(page);
  await page.getByRole("button",{name:"Xem buổi 1"}).click();
  await page.getByLabel("Tiêu đề buổi học",{exact:true}).fill("Unsaved title");
  await teacherPanel(page,"Tổng quan");
  await expect(page.getByLabel("Tiêu đề buổi học",{exact:true})).toBeHidden();
  await expect(page).toHaveURL(/\/teacher$/);
  await teacherPanel(page,"Khóa học của tôi"); await page.getByRole("button",{name:"Tiếp tục khóa học đã chọn"}).click();
  await expect(page.getByLabel("Tiêu đề buổi học",{exact:true})).toHaveValue("Unsaved title");
  await expect(page.getByRole("button",{name:"Tải lại khóa học",exact:true})).toBeHidden();
  await page.screenshot({path:info.outputPath("account-dashboard.png"),fullPage:true});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});

test("saved Teacher preview preserves unsaved curriculum and survives Course edits",async({page},info)=>{
  const f=await fixture(page);
  await page.route("**/api/v1/teacher/categories**",r=>r.fulfill({json:{items:[course.category],page:0,size:100,hasNext:false}}));
  await page.getByRole("button",{name:"Xem buổi 1",exact:true}).click();
  await page.getByLabel("Tiêu đề buổi học",{exact:true}).fill("Unsaved curriculum title");
  await page.getByRole("button",{name:"Xem trước khóa học",exact:true}).click();
  const preview=page.getByRole("region",{name:"Xem trước dành cho gia sư"});
  await expect(preview.getByText("Buổi 1: Title",{exact:true})).toBeVisible();
  await expect(preview.getByText("Unsaved curriculum title")).toHaveCount(0);
  await preview.getByRole("button",{name:"Đọc nội dung buổi 1"}).click();
  await expect(preview.getByText("<b>plain text</b>",{exact:true})).toBeVisible();
  expect(await preview.locator("form,input,textarea").count()).toBe(0);
  expect(f.writes()).toBe(0);
  await preview.getByRole("button",{name:"Xem trước: trang sau"}).click();
  await expect(preview.getByText("Chưa có buổi học trên trang này.")).toBeVisible();
  await preview.getByRole("button",{name:"Xem trước: trang trước"}).click();
  await page.getByRole("button",{name:"Lịch giảng dạy & nội dung",exact:true}).click();
  await expect(page.getByLabel("Tiêu đề buổi học",{exact:true})).toHaveValue("Unsaved curriculum title");
  await page.getByRole("textbox",{name:"Nội dung buổi học",exact:true}).fill("Saved independent curriculum");
  await page.getByRole("button",{name:"Lưu nội dung",exact:true}).click();
  await expect(page.getByRole("region",{name:"Lịch và buổi học",exact:true}).getByRole("alert")).toContainText("Đã cập nhật");
  expect(f.bodies[0]).toEqual({title:"Unsaved curriculum title",content:"Saved independent curriculum"});
  await page.getByRole("button",{name:"Thông tin & học phí",exact:true}).click();
  await page.getByRole("button",{name:"Chỉnh sửa bản nháp",exact:true}).click();
  await expect(page.getByRole("heading",{name:"1. Thông tin cơ bản"})).toBeVisible();
  await expect(page.getByRole("heading",{name:"2. Học phí & sĩ số"})).toBeVisible();
  await page.getByLabel("Tên khóa học",{exact:true}).fill("Updated Course only");
  await page.getByRole("button",{name:"Lưu bản nháp",exact:true}).click();
  await expect(page.getByText("Đã lưu bản nháp khóa học.")).toBeVisible();
  await page.getByRole("button",{name:"Xem trước khóa học",exact:true}).click();
  await expect(preview.getByRole("heading",{name:"Updated Course only"})).toBeVisible();
  await preview.getByRole("button",{name:"Đọc nội dung buổi 1"}).click();
  await expect(preview.getByText("Saved independent curriculum",{exact:true})).toBeVisible();
  expect(f.writes()).toBe(2);
  await page.screenshot({path:info.outputPath("teacher-curriculum-preview.png"),fullPage:true});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});

test("preview clears private content on authorization denial without mutation",async({page})=>{
  const f=await fixture(page);
  await page.getByRole("button",{name:"Xem trước khóa học",exact:true}).click();
  const preview=page.getByRole("region",{name:"Xem trước dành cho gia sư"});
  await expect(preview.getByText("Buổi 1: Title",{exact:true})).toBeVisible();
  await page.route(`**/api/v1/teacher/courses/${course.id}/sessions/*`,r=>r.fulfill({status:403,json:{code:"ACCESS_DENIED",message:"Denied"}}));
  await preview.getByRole("button",{name:"Đọc nội dung buổi 1"}).click();
  await expect(preview.getByRole("alert")).toBeVisible();
  await expect(preview.getByText("Buổi 1: Title",{exact:true})).toHaveCount(0);
  expect(f.writes()).toBe(0);
});
