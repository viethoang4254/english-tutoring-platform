import {test,expect,type Page} from "@playwright/test";
import {captureReview} from "../support/ui-review";
async function login(page:Page){
 await page.route("**/api/v1/**",r=>{const p=new URL(r.request().url()).pathname;
  if(p.endsWith("/csrf"))return r.fulfill({json:{token:"admin-csrf"}});
  if(p.endsWith("/login"))return r.fulfill({json:{accessToken:"admin-token",tokenType:"Bearer"}});
  if(p.endsWith("/users/me"))return r.fulfill({json:{id:"1",role:"ADMIN",emailVerified:true}});
  return r.fulfill({status:404,json:{code:"NOT_FOUND"}});
 });
 await page.goto("/login");await page.getByLabel("Email",{exact:true}).fill("admin@example.invalid");await page.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();await expect(page).toHaveURL(/\/admin\/dashboard$/);
}
async function navigate(page:Page,name:string){await expect(page.getByRole("heading",{level:1})).toBeVisible();const toggle=page.locator("[data-dashboard-toggle]");if(await toggle.isVisible())await toggle.click();await page.getByRole("link",{name,exact:true}).click();}
test("@admin-management User directory filters, pages and confirms lock/unlock",async({page},info)=>{
 await login(page);let locked=false,fail=false,writes=0,deleted=false,conflict=true,created=0,fullName="Học viên kiểm thử";
 const user=()=>({id:"2",fullName,email:"student@example.invalid",role:"STUDENT",emailVerified:false,locked});
 await page.route("**/admin/users**",r=>{const u=new URL(r.request().url());expect(r.request().headers().authorization).toBe("Bearer admin-token");
  if(r.request().method()==="POST"){expect(r.request().headers()["x-xsrf-token"]).toBe("admin-csrf");expect(r.request().postDataJSON().role).toBe("STUDENT");created++;return r.fulfill({status:201,json:{verificationRequired:false}});}
  if(r.request().method()==="DELETE"){expect(r.request().headers()["x-xsrf-token"]).toBe("admin-csrf");if(conflict)return r.fulfill({status:409,json:{code:"USER_DEPENDENCIES_EXIST",message:"PRIVATE"}});deleted=true;return r.fulfill({status:204});}
  if(u.pathname.endsWith("/profile")){expect(r.request().headers()["x-xsrf-token"]).toBe("admin-csrf");expect(Object.keys(r.request().postDataJSON())).toEqual(["fullName"]);fullName=r.request().postDataJSON().fullName;return r.fulfill({json:user()});}
  if(r.request().method()==="PATCH"){expect(r.request().headers()["x-xsrf-token"]).toBe("admin-csrf");writes++;locked=r.request().postDataJSON().locked;return r.fulfill({json:user()});}
  if(u.pathname.endsWith("/2"))return r.fulfill({json:user()});
  if(fail)return r.fulfill({status:503,json:{code:"UNAVAILABLE",message:"PRIVATE"}});
  const p=Number(u.searchParams.get("page"));return r.fulfill({json:{items:p||deleted?[]:[user()],page:p,size:20,hasNext:p===0&&!deleted}});
 });
 await navigate(page,"Quản lý người dùng");const panel=page.getByRole("region",{name:"Danh sách người dùng",exact:true});
 await expect(panel.getByText("student@example.invalid",{exact:true})).toBeVisible();
 await captureReview(page,info,"admin-users");expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await panel.getByRole("button",{name:"Chi tiết",exact:true}).click();await expect(page.getByRole("region",{name:"Chi tiết người dùng"})).toContainText("Email chưa xác minh");await page.getByRole("button",{name:"Đóng chi tiết"}).click();
 for(const name of ["Khóa","Mở khóa"]){await panel.getByRole("button",{name,exact:true}).click();expect(writes).toBe(name==="Khóa"?0:1);await panel.getByRole("button",{name:"Xác nhận",exact:true}).click();await expect(panel.getByText(name==="Khóa"?"Đã khóa tài khoản.":"Đã mở khóa tài khoản.",{exact:true})).toBeVisible();}
 expect(writes).toBe(2);
 await panel.getByRole("button",{name:"Thêm người dùng",exact:true}).click();const create=page.getByRole("dialog",{name:"Thêm người dùng"});
 await expect(create.getByRole("option",{name:"ADMIN",exact:true})).toHaveCount(0);
 await create.getByLabel("Họ và tên",{exact:true}).fill("Người dùng mới");await create.getByLabel("Email",{exact:true}).fill("new@example.invalid");await create.getByLabel("Tên người dùng",{exact:true}).fill("Student_new");await create.getByLabel("Số điện thoại",{exact:true}).fill("0912345678");await create.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");await create.getByLabel("Xác nhận mật khẩu",{exact:true}).fill("fixture-password");await create.getByRole("button",{name:"Tạo tài khoản",exact:true}).click();await expect(create).toHaveCount(0);expect(created).toBe(1);
 await panel.getByRole("button",{name:"Chỉnh sửa",exact:true}).click();const edit=page.getByRole("dialog",{name:"Chỉnh sửa người dùng"});await edit.getByLabel("Họ và tên").fill("Tên đã cập nhật");await edit.getByRole("button",{name:"Lưu thay đổi"}).click();await expect(edit).toHaveCount(0);await expect(panel.getByText("Tên đã cập nhật",{exact:true})).toBeVisible();

 await panel.getByLabel("Tìm theo tên hoặc email").fill("student");await panel.getByLabel("Vai trò",{exact:true}).selectOption("STUDENT");await panel.getByLabel("Trạng thái tài khoản").selectOption("false");
 const filtered=page.waitForRequest(r=>r.url().includes("/admin/users?")&&new URL(r.url()).searchParams.get("search")==="student");await panel.getByRole("button",{name:"Tìm kiếm",exact:true}).click();const url=new URL((await filtered).url());expect(url.searchParams.get("role")).toBe("STUDENT");expect(url.searchParams.get("locked")).toBe("false");
 await panel.getByRole("button",{name:"Trang sau",exact:true}).click();await expect(panel.getByText("Không tìm thấy người dùng.")).toBeVisible();
 fail=true;await panel.getByRole("button",{name:"Tải lại người dùng"}).click();await expect(panel.getByRole("alert")).toBeVisible();await expect(page.getByText("PRIVATE",{exact:true})).toHaveCount(0);
 fail=false;await panel.getByRole("button",{name:"Tải lại người dùng"}).click();await expect(panel.getByText("Không tìm thấy người dùng.")).toBeVisible();
 await panel.getByRole("button",{name:"Trang trước",exact:true}).click();await expect(panel.getByText("Tên đã cập nhật",{exact:true})).toBeVisible();
 await panel.getByRole("button",{name:"Xóa vĩnh viễn",exact:true}).click();const remove=page.getByRole("dialog",{name:"Xóa vĩnh viễn người dùng"});expect(deleted).toBe(false);await remove.getByRole("button",{name:"Xác nhận xóa vĩnh viễn"}).click();await expect(remove.getByRole("alert")).toContainText("không thể xóa");expect(deleted).toBe(false);conflict=false;await remove.getByRole("button",{name:"Xác nhận xóa vĩnh viễn"}).click();await expect(remove).toHaveCount(0);await expect(panel.getByText("Không tìm thấy người dùng.")).toBeVisible();expect(deleted).toBe(true);
});
test("@admin-management Teacher history and applicant details",async({page},info)=>{
 await login(page);
 await page.route("**/admin/teacher-applications**",r=>{const u=new URL(r.request().url()),status=u.searchParams.get("status")??"PENDING";return r.fulfill({json:u.pathname.endsWith("/2")?{id:"2",teacherId:"3",status:"APPROVED",specialization:"Tiếng Anh",teachingExperience:"3 năm",introduction:"Gia sư kiểm thử",submittedAt:"2026-01-01T00:00:00Z",reviewedBy:"1",reviewedAt:"2026-01-02T00:00:00Z",rejectionReason:null}:{items:status==="REJECTED"?[]:[{id:"2",teacherId:"3",status,submittedAt:"2026-01-01T00:00:00Z"}],page:0,size:20,hasNext:false}});});
 await page.route("**/admin/users/3",r=>r.fulfill({json:{id:"3",fullName:"Gia sư kiểm thử",email:"teacher@example.invalid",role:"TEACHER",locked:false,emailVerified:false}}));
 await navigate(page,"Xét duyệt gia sư");await expect(page.getByRole("button",{name:"Xem hồ sơ 2",exact:true})).toBeVisible();await page.getByRole("button",{name:"Đã duyệt",exact:true}).click();await page.getByRole("button",{name:"Xem hồ sơ 2",exact:true}).click();await page.getByRole("button",{name:"Xem tài khoản ứng viên"}).click();await expect(page.getByText(/teacher@example.invalid/)).toBeVisible();await expect(page.getByRole("button",{name:"Phê duyệt hồ sơ",exact:true})).toHaveCount(0);
 await captureReview(page,info,"admin-teacher-history");expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.getByRole("button",{name:"Đã từ chối",exact:true}).click();await expect(page.getByText("Không có hồ sơ trên trang này.")).toBeVisible();
});


test("@admin-management Bank approval loads pending and masked history",async({page},info)=>{
 await login(page);let decision="PENDING",writes=0;
 const account=()=>({id:"9",bankName:"Ngân hàng kiểm thử",holderName:"Gia sư kiểm thử",accountNumber:"****5678",reviewStatus:decision,selectionStatus:decision==="APPROVED"?"ACTIVE":"INACTIVE",hasQrImage:false,createdAt:"2026-01-01T00:00:00Z"});
 await page.route("**/admin/payment-accounts**",r=>{const u=new URL(r.request().url());
 if(r.request().method()==="POST"){expect(r.request().headers()["x-xsrf-token"]).toBe("admin-csrf");writes++;decision="APPROVED";return r.fulfill({status:204});}
 if(u.pathname.endsWith("/9"))return r.fulfill({json:{teacherId:"3",account:account(),reviewedBy:decision==="APPROVED"?"1":null,reviewedAt:decision==="APPROVED"?"2026-01-02T00:00:00Z":null,rejectionReason:null}});
 const status=u.searchParams.get("status")??"PENDING";return r.fulfill({json:{items:status===decision?[{...account(),teacherId:"3",submittedAt:"2026-01-01T00:00:00Z"}]:[],page:0,size:20,hasNext:false}});
 });
 await navigate(page,"Xét duyệt tài khoản nhận tiền");await expect(page.getByText("Gia sư kiểm thử · ****5678",{exact:true})).toBeVisible();await expect(page.getByText("0012345678")).toHaveCount(0);
 await captureReview(page,info,"admin-bank-approval");expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.getByRole("link",{name:"Xem hồ sơ 9"}).click();await page.getByRole("button",{name:"Phê duyệt",exact:true}).click();expect(writes).toBe(0);await page.getByRole("button",{name:"Xác nhận phê duyệt",exact:true}).click();await expect(page.getByText("Đã ghi nhận quyết định xét duyệt.")).toBeVisible();expect(writes).toBe(1);await expect(page.getByRole("button",{name:"Phê duyệt",exact:true})).toHaveCount(0);
 await page.getByRole("link",{name:"Danh sách chờ",exact:true}).click();await expect(page.getByText("Không có hồ sơ chờ trên trang này.")).toBeVisible();await page.getByRole("button",{name:"Đã duyệt",exact:true}).click();await expect(page.getByRole("link",{name:"Xem hồ sơ 9"})).toBeVisible();await page.getByRole("button",{name:"Đã từ chối",exact:true}).click();await expect(page.getByText("Không có hồ sơ trên trang này.")).toBeVisible();
});
