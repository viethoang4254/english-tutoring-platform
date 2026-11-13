import {expect,test,type Page,type Route} from "@playwright/test";
import {teacherPanel} from "../support/teacher-navigation";

const account={id:"9",bankBin:"970436",bankCode:"VCB",bankName:"Fixture Bank",holderName:"Gia sư",accountNumber:"00001234",reviewStatus:"PENDING",selectionStatus:"INACTIVE",hasQrImage:true,createdAt:"2026-11-01T00:00:00Z"};
const png=Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGNgYGAAAAAEAAH2FzhVAAAAAElFTkSuQmCC","base64");
const pageOf=<T,>(items:T[])=>({items,page:0,size:20,hasNext:false});

async function login(page:Page,role:"ADMIN"|"TEACHER"|"STUDENT",bank:(route:Route,path:string)=>Promise<void>,onboarding="APPROVED"){
  let remembered = false;
  await page.route("**/api/v1/**",async route=>{
    const path=new URL(route.request().url()).pathname.replace("/api/v1","");
    if(path.includes("/payment-accounts")){
      expect(route.request().headers().authorization).toBe("Bearer fixture");
      return bank(route,path);
    }
    if(path==="/auth/csrf")return route.fulfill({json:{token:"csrf-fixture"}});
    if (path.endsWith("/refresh") && remembered) { remembered = false; return route.fulfill({json:{accessToken:"fixture",tokenType:"Bearer"}}); }
    if(path==="/auth/login"){ remembered = !!route.request().postDataJSON()?.rememberMe; return route.fulfill({json:{accessToken:"fixture",tokenType:"Bearer"}}); }
    if(path==="/users/me")return route.fulfill({json:{id:"1",role,emailVerified:true}});
    if(path==="/teacher/onboarding")return route.fulfill({json:{applicationId:"1",status:onboarding}});
    if(path==="/users/me/profile")return route.fulfill({json:{id:"1",fullName:"Fixture",email:"fixture@example.invalid"}});
    return route.fulfill({status:404,json:{code:"NOT_FOUND",message:"Fixture"}});
  });
  await page.goto("/login");await page.getByLabel("Email",{exact:true}).fill("fixture@example.invalid");
  await page.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");
  await page.getByLabel("Ghi nhớ đăng nhập").check();
  await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();
  await expect(page).toHaveURL(role==="ADMIN"?/\/admin\/dashboard$/:role==="STUDENT"?/\/student\/dashboard$/:/\/teacher$/);
}
async function adminQueue(page:Page){
  const toggle=page.locator('[data-dashboard-toggle]');
  await toggle.waitFor({state:"attached"});
  if(await toggle.isVisible() && await toggle.getAttribute("aria-expanded")==="false")await toggle.click();
  await page.getByRole("link",{name:"Xét duyệt tài khoản nhận tiền",exact:true}).click();
}
const denied=(route:Route,status:number)=>route.fulfill({status,json:{code:status===409?"RECEIVING_ACCOUNT_ALREADY_REVIEWED":"ACCESS_DENIED",message:"Fixture"}});
async function fill(page:Page){
  await page.getByRole("combobox",{name:"Ngân hàng",exact:true}).selectOption("970436");
  await page.getByLabel("Tên chủ tài khoản").fill(account.holderName);await page.getByLabel("Số tài khoản").fill(account.accountNumber);
  await page.getByLabel("Ảnh QR ngân hàng").setInputFiles({name:"fixture.png",mimeType:"image/png",buffer:png});
}
test("Teacher uploads multipart evidence once and sees the selected account and submission statuses",async({page})=>{
  let submitted=false,posts=0;
  await login(page,"TEACHER",async(route)=>{
    if(route.request().method()==="POST"){
      posts++;expect(route.request().headers()["idempotency-key"]).toMatch(/^[0-9a-f-]{36}$/);
      expect(route.request().headers()["content-type"]).toContain("multipart/form-data; boundary=");
      const body=route.request().postDataBuffer()!.toString("latin1");
      for(const name of ["qrImage","accountNumber","holderName","bankBin"])expect(body).toContain(`name="${name}"`);
      expect(body).toContain("00001234");submitted=true;return route.fulfill({status:201,json:account});
    }
    return route.fulfill({json:pageOf([...(submitted?[{...account,accountNumber:"****1234"}]:[]),
      {...account,id:"7",reviewStatus:"APPROVED",selectionStatus:"ACTIVE",accountNumber:"****4567"},
      {...account,id:"6",reviewStatus:"REJECTED",accountNumber:"****2222"}])});
  });
  await teacherPanel(page,"Tài khoản nhận tiền");
  await expect(page.getByText("Đã phê duyệt · Đang sử dụng")).toBeVisible();
  await expect(page.getByText("Đã từ chối · Không được chọn")).toBeVisible();
  await fill(page);
  await page.getByRole("button",{name:"Gửi hồ sơ"}).evaluate(button=>{if(!(button instanceof HTMLButtonElement))throw new Error("Expected button");button.click();button.click();});
  await expect(page.getByText("Chờ xét duyệt · Không được chọn")).toBeVisible();expect(posts).toBe(1);
  await expect(page.getByRole("button",{name:"Gửi hồ sơ"})).toBeDisabled();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});
test("Teacher rejects invalid QR and retries uncertain submission with the same key",async({page})=>{
  const keys:string[]=[];
  await login(page,"TEACHER",async route=>{
    if(route.request().method()==="POST"){keys.push(route.request().headers()["idempotency-key"]);return keys.length===1?route.abort():route.fulfill({json:account});}
    return route.fulfill({json:pageOf([])});
  });
  await teacherPanel(page,"Tài khoản nhận tiền");await fill(page);
  await page.getByLabel("Ảnh QR ngân hàng").setInputFiles({name:"bad.png",mimeType:"image/png",buffer:Buffer.from("bad")});
  await page.getByRole("button",{name:"Gửi hồ sơ"}).click();
  await expect(page.getByRole("alert").filter({hasText:"Nội dung ảnh"})).toBeVisible();expect(keys).toHaveLength(0);
  await page.getByLabel("Ảnh QR ngân hàng").setInputFiles({name:"fixture.png",mimeType:"image/png",buffer:png});
  await page.getByRole("button",{name:"Gửi hồ sơ"}).click();await expect(page.getByRole("alert").filter({hasText:"Chưa xác nhận"})).toBeVisible();
  await page.getByRole("button",{name:"Gửi hồ sơ"}).click();await expect(page.getByText("Hồ sơ đã được ghi nhận.",{exact:false})).toBeVisible();
  expect(keys).toHaveLength(2);expect(keys[1]).toBe(keys[0]);
});
for(const action of ["approve","reject"] as const)test(`Admin ${action} rechecks detail and releases private QR preview`,async({page})=>{
  let decided=false,posts=0;
  await login(page,"ADMIN",async(route,path)=>{
    if(path.endsWith("/pending"))return route.fulfill({json:pageOf(decided?[]:[{id:"9",teacherId:"1",bankName:"Fixture Bank",submittedAt:account.createdAt}])});
    if(path.endsWith("/qr-image"))return route.fulfill({contentType:"image/png",body:png});
    if(path.endsWith("/"+action)){
      posts++;expect(route.request().method()).toBe("POST");
      if(action==="reject")expect(route.request().postDataJSON()).toEqual({reason:"Thông tin không khớp"});else expect(route.request().postData()).toBeNull();
      decided=true;return route.fulfill({status:204});
    }
    return route.fulfill({json:{teacherId:"1",account:{...account,reviewStatus:decided?(action==="approve"?"APPROVED":"REJECTED"):"PENDING",selectionStatus:decided&&action==="approve"?"ACTIVE":"INACTIVE"},reviewedBy:decided?"2":null,reviewedAt:decided?account.createdAt:null,rejectionReason:decided&&action==="reject"?"Thông tin không khớp":null}});
  });
  await adminQueue(page);await page.getByRole("link",{name:"Xem hồ sơ 9"}).click();
  await expect(page.getByText("Số tài khoản: 00001234")).toBeVisible();
  await page.getByRole("button",{name:"Xem ảnh QR riêng tư"}).click();
  const img=page.getByRole("img",{name:"Ảnh QR ngân hàng đã nộp"});await expect(img).toHaveAttribute("src",/^blob:/);
  const url=await img.getAttribute("src");await page.getByRole("button",{name:"Ẩn ảnh QR"}).click();
  expect(await page.evaluate(async u=>{try{await fetch(u!);return false;}catch{return true;}},url)).toBe(true);
  await page.getByRole("button",{name:action==="approve"?"Phê duyệt":"Từ chối",exact:true}).click();
  const confirm=page.getByRole("button",{name:action==="approve"?"Xác nhận phê duyệt":"Xác nhận từ chối"});
  if(action==="reject"){await confirm.click();await expect(page.getByRole("main").getByRole("alert")).toContainText("Nhập lý do");expect(posts).toBe(0);await page.getByLabel("Lý do từ chối").fill("Thông tin không khớp");}
  await confirm.evaluate(b=>{if(!(b instanceof HTMLButtonElement))throw new Error("Expected button");b.click();b.click();});await expect(page.getByText("Đã ghi nhận quyết định xét duyệt.")).toBeVisible();
  await expect(page.getByText(action==="approve"?"Đã phê duyệt · Đang sử dụng":"Đã từ chối · Không được chọn")).toBeVisible();expect(posts).toBe(1);
  expect(await page.evaluate(()=>[localStorage,sessionStorage].every(s=>!JSON.stringify(s).includes("00001234")))).toBe(true);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.getByRole("link",{name:"Danh sách chờ"}).click();await expect(page.getByText("Không có hồ sơ chờ trên trang này.")).toBeVisible();
});
for(const status of [401,403] as const)test(`Admin queue handles HTTP ${status}`,async({page})=>{
  await login(page,"ADMIN",r=>denied(r,status));await adminQueue(page);
  if(status===401)await expect(page).toHaveURL(/\/login\?next=/);
  else await expect(page.getByRole("main").getByRole("alert")).toContainText("không có quyền");
});
test("Concurrent decision conflict refreshes review without replay",async({page})=>{
  let posts=0;
  await login(page,"ADMIN",async(route,path)=>{
    if(path.endsWith("/pending"))return route.fulfill({json:pageOf([{id:"9",teacherId:"1",bankName:"Fixture",submittedAt:account.createdAt}])});
    if(path.endsWith("/approve")){posts++;return denied(route,409);}
    return route.fulfill({json:{teacherId:"1",account:{...account,reviewStatus:posts?"REJECTED":"PENDING"},reviewedBy:posts?"3":null,reviewedAt:posts?account.createdAt:null,rejectionReason:posts?"Thông tin sai":null}});
  });
  await adminQueue(page);await page.getByRole("link",{name:"Xem hồ sơ 9"}).click();
  await page.getByRole("button",{name:"Phê duyệt",exact:true}).click();await page.getByRole("button",{name:"Xác nhận phê duyệt"}).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("Trạng thái hồ sơ đã thay đổi");await expect(page.getByText("Lý do từ chối: Thông tin sai")).toBeVisible();expect(posts).toBe(1);
});
test("Teacher loses access and private submission controls disappear on 403",async({page})=>{
  await login(page,"TEACHER",r=>denied(r,403));await teacherPanel(page,"Tài khoản nhận tiền");
  await expect(page.getByRole("alert").filter({hasText:"không có quyền xem"})).toBeVisible();
  await expect(page.getByRole("button",{name:"Gửi hồ sơ"})).toHaveCount(0);
});
test("Direct anonymous review URLs require login before private reads",async({page})=>{
  let reads=0;
  await page.route("**/api/v1/admin/payment-accounts/**",r=>{reads++;return denied(r,401);});
  await page.goto("/admin/dashboard?section=receiving&id=9");await expect(page).toHaveURL(/\/login\?next=/);
  expect(reads).toBe(0);
});
test("Pending Teacher cannot open receiving management from navigation",async({page})=>{
  let reads=0;
  await login(page,"TEACHER",r=>{reads++;return denied(r,403);},"PENDING");
  await page.goto("/teacher/payment-accounts");
  await expect(page.getByRole("heading",{name:"Chưa thể mở không gian gia sư"})).toBeVisible();
  await expect(page.locator("[data-dashboard-layout] nav").getByRole("link",{name:"Tài khoản nhận tiền"})).toHaveCount(0);
  await expect(page.getByRole("button",{name:"Gửi hồ sơ"})).toHaveCount(0);
  expect(reads).toBe(0);
});

test("Admin confirmation moves and restores keyboard focus without submitting",async({page})=>{
  let posts=0;
  await login(page,"ADMIN",async(route,path)=>{
    if(route.request().method()==="POST"){posts++;return denied(route,409);}
    if(path.endsWith("/pending"))return route.fulfill({json:pageOf([{id:"9",teacherId:"1",bankName:"Fixture Bank",submittedAt:account.createdAt}])});
    return route.fulfill({json:{teacherId:"1",account,reviewedBy:null,reviewedAt:null,rejectionReason:null}});
  });
  await adminQueue(page);await page.getByRole("link",{name:"Xem hồ sơ 9"}).click();
  const approve=page.getByRole("button",{name:"Phê duyệt",exact:true});
  const reject=page.getByRole("button",{name:"Từ chối",exact:true});
  const back=page.getByRole("button",{name:"Quay lại",exact:true});
  await approve.focus();await page.keyboard.press("Enter");
  await expect(page.getByRole("button",{name:"Xác nhận phê duyệt",exact:true})).toBeFocused();
  await page.keyboard.press("Tab");await expect(back).toBeFocused();
  await page.keyboard.press("Enter");await expect(approve).toBeFocused();
  await page.keyboard.press("Tab");await expect(reject).toBeFocused();
  await page.keyboard.press("Enter");await expect(page.getByLabel("Lý do từ chối")).toBeFocused();
  await page.keyboard.press("Tab");await expect(page.getByRole("button",{name:"Xác nhận từ chối",exact:true})).toBeFocused();
  await page.keyboard.press("Tab");await expect(back).toBeFocused();
  await page.keyboard.press("Enter");await expect(reject).toBeFocused();
  expect(posts).toBe(0);
});

test("Teacher paginates history, finds the older ACTIVE account and releases owned QR evidence",async({page})=>{
  const history=Array.from({length:20},(_,i)=>({...account,id:String(40-i),reviewStatus:"REJECTED",accountNumber:"****1234"}));
  const selected={...account,id:"8",reviewStatus:"APPROVED",selectionStatus:"ACTIVE",accountNumber:"00005678"};
  const pages:number[]=[],details:string[]=[],images:string[]=[];
  await login(page,"TEACHER",async(route,path)=>{
    expect(route.request().method()).toBe("GET");
    if(path.endsWith("/8/qr-image")){images.push(path);return route.fulfill({contentType:"image/png",body:png});}
    if(path.endsWith("/8")){details.push(path);return route.fulfill({json:selected});}
    expect(path).toBe("/teacher/payment-accounts");
    const query=new URL(route.request().url()).searchParams,index=Number(query.get("page"));
    expect(query.get("size")).toBe("20");expect([0,1]).toContain(index);pages.push(index);
    return route.fulfill({json:{items:index===0?history:[{...selected,accountNumber:"****5678"}],page:index,size:20,hasNext:index===0}});
  });
  await teacherPanel(page,"Tài khoản nhận tiền");
  await expect(page.getByText("Fixture Bank · ****5678",{exact:true})).toBeVisible();
  await expect(page.getByRole("button",{name:"Chi tiết hồ sơ 40",exact:true})).toBeVisible();
  await expect(page.getByRole("button",{name:"Chi tiết hồ sơ 8",exact:true})).toHaveCount(0);
  await expect(page.getByRole("button",{name:"Trang trước",exact:true})).toBeDisabled();
  expect(pages).toEqual([0,1]);
  await page.getByRole("button",{name:"Trang sau",exact:true}).click();
  await expect(page.getByRole("button",{name:"Chi tiết hồ sơ 8",exact:true})).toBeVisible();
  await expect(page.getByRole("button",{name:"Chi tiết hồ sơ 40",exact:true})).toHaveCount(0);
  await expect(page.getByRole("button",{name:"Trang sau",exact:true})).toBeDisabled();
  await page.getByRole("button",{name:"Chi tiết hồ sơ 8",exact:true}).click();
  await expect(page.getByText("Chủ tài khoản: Gia sư",{exact:true})).toBeVisible();
  await expect(page.getByText("Số tài khoản: 00005678",{exact:true})).toBeVisible();
  const image=page.getByRole("img",{name:"Ảnh QR ngân hàng đã nộp"});
  await page.getByRole("button",{name:"Xem ảnh QR riêng tư"}).click();await expect(image).toHaveAttribute("src",/^blob:/);
  const closedUrl=await image.getAttribute("src");
  await page.getByRole("button",{name:"Ẩn ảnh QR"}).click();
  await expect(image).toHaveCount(0);
  expect(await page.evaluate(async url=>{try{await fetch(url!);return false;}catch{return true;}},closedUrl)).toBe(true);
  await page.getByRole("button",{name:"Xem ảnh QR riêng tư"}).click();await expect(image).toHaveAttribute("src",/^blob:/);
  const unmountedUrl=await image.getAttribute("src");
  await page.getByRole("button",{name:"Trang trước",exact:true}).click();
  await expect(page.getByRole("button",{name:"Chi tiết hồ sơ 40",exact:true})).toBeVisible();
  await expect(image).toHaveCount(0);await expect(page.getByRole("button",{name:"Trang trước",exact:true})).toBeDisabled();
  expect(await page.evaluate(async url=>{try{await fetch(url!);return false;}catch{return true;}},unmountedUrl)).toBe(true);
  expect(details).toEqual(["/teacher/payment-accounts/8"]);
  expect(images).toEqual(["/teacher/payment-accounts/8/qr-image","/teacher/payment-accounts/8/qr-image"]);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});

test("Admin paginates the pending queue and reloads it after approving the final page",async({page})=>{
  let decided=false,posts=0;
  const queue=Array.from({length:21},(_,i)=>({id:String(i+1),teacherId:String(i+10),bankName:"Fixture Bank",submittedAt:account.createdAt}));
  await login(page,"ADMIN",async(route,path)=>{
    if(path.endsWith("/pending")){
      const query=new URL(route.request().url()).searchParams,index=Number(query.get("page"));
      expect(query.get("size")).toBe("20");expect([0,1]).toContain(index);
      const rows=decided?queue.slice(0,20):queue;
      return route.fulfill({json:{items:rows.slice(index*20,(index+1)*20),page:index,size:20,hasNext:rows.length>(index+1)*20}});
    }
    if(path.endsWith("/21/approve")){expect(route.request().method()).toBe("POST");posts++;decided=true;return route.fulfill({status:204});}
    expect(path).toBe("/admin/payment-accounts/21");
    return route.fulfill({json:{teacherId:"30",account:{...account,id:"21",reviewStatus:decided?"APPROVED":"PENDING",selectionStatus:decided?"ACTIVE":"INACTIVE"},reviewedBy:decided?"1":null,reviewedAt:decided?account.createdAt:null,rejectionReason:null}});
  });
  await adminQueue(page);
  await expect(page.getByRole("link",{name:/^Xem hồ sơ /})).toHaveCount(20);
  await expect(page.getByRole("link",{name:"Xem hồ sơ 1",exact:true})).toBeVisible();
  await expect(page.getByRole("button",{name:"Trang trước",exact:true})).toBeDisabled();
  await page.getByRole("button",{name:"Trang sau",exact:true}).click();
  await expect(page.getByRole("link",{name:/^Xem hồ sơ /})).toHaveCount(1);
  await expect(page.getByRole("link",{name:"Xem hồ sơ 21",exact:true})).toBeVisible();
  await expect(page.getByRole("button",{name:"Trang sau",exact:true})).toBeDisabled();
  await page.getByRole("button",{name:"Trang trước",exact:true}).click();
  await expect(page.getByRole("link",{name:"Xem hồ sơ 1",exact:true})).toBeVisible();
  await page.getByRole("button",{name:"Trang sau",exact:true}).click();
  await page.getByRole("link",{name:"Xem hồ sơ 21",exact:true}).click();
  await page.getByRole("button",{name:"Phê duyệt",exact:true}).click();await page.getByRole("button",{name:"Xác nhận phê duyệt",exact:true}).click();
  await expect(page.getByText("Đã phê duyệt · Đang sử dụng",{exact:true})).toBeVisible();expect(posts).toBe(1);
  await page.getByRole("link",{name:"Danh sách chờ",exact:true}).click();
  await expect(page.getByRole("link",{name:/^Xem hồ sơ /})).toHaveCount(20);
  await expect(page.getByRole("link",{name:"Xem hồ sơ 21",exact:true})).toHaveCount(0);
  await expect(page.getByRole("button",{name:"Trang sau",exact:true})).toBeDisabled();
  await expect(page.getByRole("button",{name:"Trang trước",exact:true})).toBeDisabled();
});

for(const role of ["STUDENT","TEACHER"] as const)test(`${role} cannot open Admin receiving review or issue private requests`,async({page})=>{
  let reads=0;
  await login(page,role,route=>{reads++;return denied(route,403);});
  // Client navigation preserves the authenticated session; a full reload would only test anonymous access.
  await page.goto("/admin/dashboard?section=receiving&id=9");
  await expect(page).toHaveURL(/\/admin\/dashboard\?section=receiving&id=9$/);
  await expect(page.getByRole("heading",{name:"Không có quyền truy cập",exact:true})).toBeVisible();
  await expect(page.getByRole("button",{name:"Phê duyệt",exact:true})).toHaveCount(0);
  await expect(page.getByRole("button",{name:"Xem ảnh QR riêng tư",exact:true})).toHaveCount(0);
  expect(reads).toBe(0);
});
