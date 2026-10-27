import { teacherPanel, openTeacher } from "../support/teacher-navigation";
import { test, expect, type Page } from "@playwright/test";
const id="9007199254740993";
const course={id,name:"Khóa học mẫu kiểm thử",status:"DRAFT",category:{id:"9",name:"Danh mục cũ",active:false},description:"Nội dung kiểm thử",tuition:{amount:"9999999999999.99",currency:"VND"},minStudents:1,maxStudents:150,sessionCount:200,plannedStartDate:"2026-12-01",timezone:"Asia/Ho_Chi_Minh",discount:null};
async function open(page:Page) {
  await page.route("**/api/v1/**",route=>{
    const path=new URL(route.request().url()).pathname;
    if(path.endsWith("/csrf"))return route.fulfill({json:{token:"fixture"}});
    if(path.endsWith("/login"))return route.fulfill({json:{accessToken:"fixture",tokenType:"Bearer"}});
    if(path.endsWith("/users/me"))return route.fulfill({json:{id:"1",role:"TEACHER",emailVerified:true}});
    if(path.endsWith("/teacher/onboarding"))return route.fulfill({json:{applicationId:"1",status:"APPROVED",submittedAt:"2026-01-01T00:00:00Z",reviewedAt:null,rejectionReason:null}});
    return route.fulfill({status:404,json:{code:"NOT_FOUND",message:"Unavailable"}});
  });
  await page.goto("/account");
  await page.getByLabel("Email",{exact:true}).fill("teacher@example.invalid");
  await page.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");
  await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();
  await openTeacher(page); await teacherPanel(page,"Khóa học của tôi");
  await expect(page.getByRole("heading",{name:"Khóa học của tôi"}).last()).toBeVisible();
}
test("owned list filtering pagination detail edit inactive retention and responsive layout",async({page},info)=>{
  await open(page);let saved={...course};
  await page.route("**/api/v1/teacher/categories**",r=>r.fulfill({json:{items:[{id:"10",name:"Đang hoạt động",active:true}],page:0,size:100,hasNext:false}}));
  await page.route("**/api/v1/teacher/courses**",route=>{
    const req=route.request(),url=new URL(req.url());expect(req.headers().authorization).toBe("Bearer fixture");
    if(req.method()==="PATCH") {const body=req.postDataJSON();expect(body.categoryId).toBe("9");expect(body.tuition.amount).toBe("9999999999999.99");expect(body.teacherId).toBeUndefined();saved={...saved,...body,category:course.category};return route.fulfill({json:saved});}
    if(url.pathname.endsWith(id))return route.fulfill({json:saved});
    const number=Number(url.searchParams.get("page"));return route.fulfill({json:{items:number||url.searchParams.get("status")==="ARCHIVED"?[]:[saved],page:number,size:20,hasNext:number===0}});
  });
  await page.getByRole("button",{name:"Tải lại khóa học",exact:true}).click();
  await page.getByRole("button",{name:"Trang sau"}).click();await expect(page.getByText("Không có khóa học trên trang này.")).toBeVisible();
  await page.getByRole("button",{name:"Trang trước"}).click();
  await page.getByLabel("Trạng thái khóa học").selectOption("ARCHIVED");await expect(page.getByText("Không có khóa học trên trang này.")).toBeVisible();
  await page.getByLabel("Trạng thái khóa học").selectOption("DRAFT");
  await page.getByRole("button",{name:"Xem chi tiết"}).click();
  await page.getByRole("button",{name:"Chỉnh sửa bản nháp",exact:true}).click();
  await expect(page.getByRole("combobox",{name:"Danh mục",exact:true})).toHaveValue("9");
  await page.getByLabel("Tên khóa học",{exact:true}).fill("Đã đổi tên");
  await page.screenshot({path:info.outputPath("teacher-draft.png"),fullPage:true});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await page.getByRole("button",{name:"Lưu bản nháp"}).click();await expect(page.getByText("Đã lưu bản nháp khóa học.")).toBeVisible();
  await expect(page.getByRole("heading",{name:"Đã đổi tên"})).toBeVisible();
});
test("create with paginated categories and decimal discount",async({page})=>{
  await open(page);let writes=0;
  await page.route("**/api/v1/teacher/categories**",route=>{const p=Number(new URL(route.request().url()).searchParams.get("page"));return route.fulfill({json:{items:[{id:p?"11":"10",name:p?"Trang hai":"Trang một",active:true}],page:p,size:100,hasNext:p===0}});});
  await page.route("**/api/v1/teacher/courses",async route=>{
    writes++;const body=route.request().postDataJSON();expect(body.categoryId).toBe("11");expect(body.maxStudents).toBe(150);expect(body.sessionCount).toBe(200);expect(body.discount.percent).toBe("12.34");
    await new Promise(resolve=>setTimeout(resolve,150));return route.fulfill({status:201,json:{...course,...body,category:{id:"11",name:"Trang hai",active:true}}});
  });
  await page.getByRole("button",{name:"Tạo bản nháp"}).click();await page.getByRole("button",{name:"Thêm danh mục"}).click();
  await page.getByRole("combobox",{name:"Danh mục",exact:true}).selectOption("11");await page.getByLabel("Tên khóa học",{exact:true}).fill("Mới");await page.getByLabel("Mô tả",{exact:true}).fill("Nội dung");
  await page.getByLabel("Ngày bắt đầu dự kiến",{exact:true}).fill("2026-12-01");await page.getByLabel("Số học viên tối đa").fill("150");await page.getByLabel("Số buổi dự kiến").fill("200");
  await page.getByLabel("Giảm giá (%)",{exact:false}).fill("12.34");await page.getByLabel("Bắt đầu giảm giá").fill("2026-11-01T00:00:00Z");await page.getByLabel("Kết thúc giảm giá").fill("2026-11-30T00:00:00Z");
  await page.getByRole("button",{name:"Lưu bản nháp"}).click();await expect(page.getByRole("button",{name:"Lưu bản nháp"})).toBeDisabled();
  await expect(page.getByText("Đã lưu bản nháp khóa học.")).toBeVisible();expect(writes).toBe(1);
});
for(const response of [400,409,503,0])test(`mutation ${response} is not retried and uncertain outcomes block writes`,async({page})=>{
  await open(page);let writes=0;
  await page.route("**/api/v1/teacher/categories**",r=>r.fulfill({json:{items:[],page:0,size:100,hasNext:false}}));
  await page.route("**/api/v1/teacher/courses**",route=>{
    if(route.request().method()==="PATCH"){writes++;return response?route.fulfill({status:response,json:{code:response===409?"CATEGORY_INACTIVE":response===400?"INVALID_REQUEST":"COURSE_UNAVAILABLE",message:"Failure"}}):route.abort();}
    return route.fulfill({json:new URL(route.request().url()).pathname.endsWith(id)?course:{items:[course],page:0,size:20,hasNext:false}});
  });
  await page.getByRole("button",{name:"Tải lại khóa học",exact:true}).click();await page.getByRole("button",{name:"Xem chi tiết"}).click();await page.getByRole("button",{name:"Chỉnh sửa bản nháp",exact:true}).click();
  await page.getByRole("button",{name:"Lưu bản nháp"}).click();await expect(page.getByRole("alert").last()).toBeVisible();expect(writes).toBe(1);
  if(response===503||response===0){await expect(page.getByRole("button",{name:"Lưu bản nháp"})).toBeDisabled();await page.getByRole("button",{name:"Tải lại khóa học",exact:true}).click();await expect(page.getByRole("button",{name:"Chỉnh sửa bản nháp",exact:true})).toBeEnabled();}
});
test("published course is read-only and authorization denial clears private content",async({page})=>{
  await open(page);let denied=false;
  await page.route("**/api/v1/teacher/courses**",r=>denied?r.fulfill({status:403,json:{code:"ACCESS_DENIED",message:"Denied"}}):r.fulfill({json:new URL(r.request().url()).pathname.endsWith(id)?{...course,status:"PUBLISHED"}:{items:[course],page:0,size:20,hasNext:false}}));
  await page.getByRole("button",{name:"Tải lại khóa học",exact:true}).click();await page.getByRole("button",{name:"Xem chi tiết"}).click();await expect(page).toHaveURL(new RegExp(`/teacher/courses/${id}$`));await expect(page.getByRole("heading",{name:course.name,exact:true})).toBeVisible();await expect(page.getByRole("button",{name:"Chỉnh sửa bản nháp",exact:true})).toHaveCount(0);
  denied=true;await page.getByRole("button",{name:"Tải lại khóa học",exact:true}).click();await expect(page.getByRole("button",{name:"Chỉnh sửa bản nháp",exact:true})).toHaveCount(0);await expect(page.getByRole("heading",{name:course.name})).toHaveCount(0);
});
test("expired authentication removes course workspace without refresh retry",async({page})=>{
  await open(page);let calls=0;
  await page.route("**/api/v1/teacher/courses**",r=>{calls++;return r.fulfill({status:401,json:{code:"AUTHENTICATION_REQUIRED",message:"Denied"}});});
  await page.getByRole("button",{name:"Tải lại khóa học",exact:true}).click();
  await expect(page.getByRole("heading",{name:"Khóa học của tôi"}).last()).toHaveCount(0);
  await expect(page.getByRole("button",{name:"Đăng nhập",exact:true})).toBeVisible();expect(calls).toBe(1);
});
