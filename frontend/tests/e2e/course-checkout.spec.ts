import {expect,test} from "@playwright/test";
import {captureReview} from "../support/ui-review";
import {setupCheckout,signInCheckout,confirm,enrollmentId,quote,payment,checkoutPath} from "../support/course-checkout";

test("@checkout-review confirmation, protected VietQR and confirmed state",async({page},info)=>{
  const fixture=await setupCheckout(page);await signInCheckout(page);
  await expect(confirm(page)).toBeEnabled();
  for(const text of [quote.course.name,"Giao tiếp","Gia sư An","08/01/2030","Giờ Việt Nam (UTC+7)","Thứ Ba · 19:00–20:30","2.000.000 ₫","10%","1.800.000 ₫"])
    await expect(page.getByText(text,{exact:true})).toBeVisible();
  expect(fixture.calls.filter(c=>c.endsWith("/enrollment")||c.endsWith("/payments"))).toEqual([]);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await captureReview(page,info,"enrollment-confirmation");
  await confirm(page).click();await expect(page).toHaveURL(`/student/enrollments?id=${enrollmentId}`);
  await expect(page.getByText(`Nội dung chuyển khoản: ${payment.transferReference}`,{exact:true})).toBeVisible();
  await expect(page.getByText(/Hết hạn thanh toán:/)).toContainText("07:15");
  await page.getByRole("button",{name:"VietQR thanh toán khóa học",exact:true}).click();
  const qr=page.getByRole("img",{name:"VietQR thanh toán khóa học",exact:true});await expect(qr).toBeVisible();
  await expect.poll(()=>qr.evaluate(el=>[(el as HTMLImageElement).naturalWidth,(el as HTMLImageElement).naturalHeight])).toEqual([232,232]);
  await expect(page.getByRole("button",{name:"Mở khóa học",exact:true})).toHaveCount(0);
  await expect(page.locator('a[href*="meet.google"]')).toHaveCount(0);
  const copied:string[]=[];await page.exposeFunction("recordCopied",(value:string)=>copied.push(value));
  await page.evaluate(()=>Object.defineProperty(navigator,"clipboard",{configurable:true,value:{writeText:(value:string)=>(window as unknown as {recordCopied:(s:string)=>Promise<void>}).recordCopied(value)}}));
  for(const label of ["Sao chép số tài khoản","Sao chép số tiền","Sao chép nội dung"])await page.getByRole("button",{name:label,exact:true}).click();
  await expect.poll(()=>copied).toEqual([payment.accountNumber,payment.finalAmount,payment.transferReference]);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await captureReview(page,info,"vietqr-payment");
  fixture.confirmPayment();await page.getByRole("button",{name:"Tải lại thanh toán",exact:true}).click();
  await expect(page.getByText("Thanh toán đã được xác nhận. Đăng ký đang hoạt động.")).toBeVisible();
  await expect(qr).toHaveCount(0);await expect(page.getByRole("button",{name:"Mở khóa học",exact:true})).toBeVisible();
  await captureReview(page,info,"payment-confirmed");
  expect(fixture.calls.filter(c=>c.endsWith("/activate")||c.includes("confirm-manually"))).toEqual([]);
});
test("changed price requires acknowledgment before any transfer instruction",async({page})=>{
  const {calls}=await setupCheckout(page,{priceChanged:true});await signInCheckout(page);await confirm(page).click();
  await expect(page.getByRole("alert")).toContainText("Học phí đã thay đổi.");await expect(page.getByRole("alert")).toContainText("1.900.000 ₫");
  await expect(page).toHaveURL(checkoutPath);await expect(page.getByRole("img",{name:"VietQR thanh toán khóa học"})).toHaveCount(0);
  expect(calls.some(c=>c.includes("qr-image"))).toBe(false);
  await page.getByRole("button",{name:"Tôi đồng ý với số tiền mới"}).click();
  await expect(page).toHaveURL(`/student/enrollments?id=${enrollmentId}`);await expect(page.getByText("Số tiền: 1.900.000 ₫",{exact:true})).toBeVisible();
});
test("existing pending payment recovers the same payment identifier",async({page})=>{
  const {calls}=await setupCheckout(page,{existingPayment:true});await signInCheckout(page);await expect(page.getByText("Đăng ký hiện tại:",{exact:false})).toContainText("Chờ thanh toán");
  await confirm(page).click();await expect(page).toHaveURL(`/student/enrollments?id=${enrollmentId}`);
  await expect(page.getByText(`Nội dung chuyển khoản: ${payment.transferReference}`,{exact:true})).toBeVisible();
  expect(calls.filter(c=>c.endsWith("/payments"))).toHaveLength(1);
  expect(calls.filter(c=>c==="GET /api/v1/student/payments/7")).toHaveLength(1);
});
test("uncertain payment result preserves reservation and recovers without another creation",async({page})=>{
  const {calls}=await setupCheckout(page,{paymentFailure:true});await signInCheckout(page);await confirm(page).click();
  await expect(page.getByRole("alert")).toContainText("Chưa xác nhận được kết quả.");
  await page.getByRole("link",{name:"Kiểm tra đăng ký và khôi phục thanh toán"}).click();
  await expect(page.getByText(`Nội dung chuyển khoản: ${payment.transferReference}`,{exact:true})).toBeVisible();
  expect(calls.filter(c=>c.endsWith("/payments"))).toHaveLength(1);expect(calls.filter(c=>c.endsWith("/enrollment"))).toHaveLength(1);
});
for (const lostResponse of [true,false]) test(lostResponse
  ? "lost creation response recovers the repriced payment and requires acknowledgment"
  : "direct recovery without a reviewed amount requires acknowledgment",async({page})=>{
  const {calls}=await setupCheckout(page,lostResponse
    ? {priceChanged:true,paymentFailure:true} : {existingPayment:true});
  await signInCheckout(page);
  if(lostResponse){
    await confirm(page).click();
    await page.getByRole("link",{name:"Kiểm tra đăng ký và khôi phục thanh toán"}).click();
  } else {
    await page.goto(`/student/enrollments?id=${enrollmentId}`);
  }
  const gate=page.getByRole("button",{name:"Xác nhận số tiền thanh toán",exact:true});
  for(const refresh of [false,true]){
    if(refresh)await page.reload();
    await expect(gate).toBeVisible();
    await expect(page.getByText(lostResponse?"1.900.000 ₫":"1.800.000 ₫",{exact:true})).toBeVisible();
    await expect(page.getByText(lostResponse?/Học phí đã thay đổi/:/kiểm tra số tiền trước khi chuyển khoản/)).toBeVisible();
    for(const value of [payment.accountNumber,payment.bankName,payment.transferReference]){
      await expect(page.getByText(value,{exact:false})).toHaveCount(0);
    }
    await expect(page.getByRole("button",{name:/Sao chép|VietQR/})).toHaveCount(0);
    expect(calls.some(c=>c.endsWith("/qr-image"))).toBe(false);
  }
  await gate.click();
  await expect(page.getByText(`Nội dung chuyển khoản: ${payment.transferReference}`,{exact:true})).toBeVisible();
  await page.getByRole("button",{name:"VietQR thanh toán khóa học"}).click();
  await expect(page.getByRole("img",{name:"VietQR thanh toán khóa học"})).toBeVisible();
  expect(calls.filter(c=>c.endsWith("/qr-image"))).toEqual(["GET /api/v1/student/payments/7/qr-image"]);
  expect(calls.filter(c=>c.startsWith("POST ")&&c.endsWith("/payments"))).toHaveLength(lostResponse?1:0);
});
for(const reason of ["COURSE_FULL","COURSE_NOT_ENROLLABLE","RECEIVING_ACCOUNT_UNAVAILABLE"])test(`preview blocks ${reason} without mutations`,async({page})=>{
  const {calls}=await setupCheckout(page,{blocked:reason});await signInCheckout(page);await expect(confirm(page)).toBeDisabled();
  expect(calls.filter(c=>c.startsWith("POST /api/v1/student/"))).toEqual([]);
});
for(const state of ["EXPIRED","CANCELLED"])test(`${state} payment hides transfer instructions and protected access`,async({page})=>{
  const fixture=await setupCheckout(page,{existingPayment:true});await signInCheckout(page);fixture.setPaymentStatus(state);
  await page.getByRole("link",{name:"Xem đăng ký hiện có"}).click();await expect(page.getByText("Trạng thái thanh toán:",{exact:false})).toBeVisible();
  await expect(page.getByRole("button",{name:"VietQR thanh toán khóa học",exact:true})).toHaveCount(0);
  await expect(page.getByText(/Nội dung chuyển khoản:/)).toHaveCount(0);await expect(page.getByRole("button",{name:"Mở khóa học"})).toHaveCount(0);
});
test("unsafe login return path is ignored and credentials never enter the URL",async({page})=>{
  await setupCheckout(page);await page.goto("/login?next="+encodeURIComponent("//evil.invalid/checkout"));
  await page.getByLabel("Email",{exact:true}).fill("fixture@example.invalid");await page.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");
  await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();await expect(page).toHaveURL(/\/student\/dashboard$/);
  expect(page.url()).not.toContain("fixture-password");expect(page.url()).not.toContain("fixture%40");
});
test("denied payment recovery never renders bank details or QR",async({page})=>{
  await setupCheckout(page,{existingPayment:true});await signInCheckout(page);
  await page.route("**/api/v1/student/payments/7",route=>route.fulfill({status:404,json:{code:"PAYMENT_NOT_FOUND",message:"private diagnostic"}}));
  await page.getByRole("link",{name:"Xem đăng ký hiện có"}).click();await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByText(payment.accountNumber,{exact:false})).toHaveCount(0);
  await expect(page.getByRole("button",{name:"VietQR thanh toán khóa học"})).toHaveCount(0);
  await expect(page.getByText("private diagnostic")).toHaveCount(0);
});
