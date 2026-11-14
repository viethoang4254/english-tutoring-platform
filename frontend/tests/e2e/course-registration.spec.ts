import {expect,test} from "@playwright/test";
import {setupCheckout,signInCheckout,confirm,courseId,enrollmentId,checkoutPath} from "../support/course-checkout";

for(const responseStatus of [201,200])test(`Student reservation ${responseStatus} preserves PENDING and real IDs`,async({page})=>{
  const {calls}=await setupCheckout(page,{responseStatus});await signInCheckout(page);
  expect(calls.filter(c=>c.startsWith("POST")&&!c.includes("/auth/"))).toEqual([]);
  await confirm(page).click();await expect(page).toHaveURL(`/student/enrollments?id=${enrollmentId}`);
  await expect(page.getByText("Chờ thanh toán",{exact:true})).toBeVisible();
  await expect(page.getByText(/Nội dung chuyển khoản: ELPFIXTURE123/)).toBeVisible();
  expect(calls.filter(c=>c.endsWith("/enrollment"))).toEqual([`POST /api/v1/student/courses/${courseId}/enrollment`]);
  expect(calls.filter(c=>c.endsWith("/payments"))).toHaveLength(1);
  await expect(page.locator('a[href*="meet.google"]')).toHaveCount(0);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});
for(const role of ["TEACHER","ADMIN"] as const)test(`${role} cannot reserve`,async({page})=>{
  const {calls}=await setupCheckout(page,{role});await signInCheckout(page,role);await page.goto(checkoutPath);
  await expect(page.getByText("Tài khoản hiện tại không có quyền truy cập trang này.")).toBeVisible();
  await expect(confirm(page)).toHaveCount(0);
  expect(calls.filter(c=>c.includes("/student/"))).toEqual([]);
});
for(const [responseStatus,code,message] of [
  [409,"COURSE_FULL","Khóa học đã hết chỗ."],
  [409,"COURSE_NOT_ENROLLABLE","Khóa học không còn nhận đăng ký hoặc đã bắt đầu."],
  [409,"ENROLLMENT_RENEWAL_REQUIRED","Giữ chỗ đã hết hạn. Bạn cần chủ động gia hạn."],
  [409,"ENROLLMENT_STATE_CONFLICT","Trạng thái đăng ký không cho phép thao tác này."],
  [404,"COURSE_NOT_FOUND","Khóa học không tồn tại hoặc không còn khả dụng."],
  [403,"ACCESS_DENIED","Bạn không có quyền truy cập đăng ký này."],
  [401,"UNAUTHORIZED",""],
  [503,"ENROLLMENT_UNAVAILABLE","Không thể tải hoặc cập nhật đăng ký. Vui lòng tải lại dữ liệu."]
] as const)test(`reservation handles ${code} without granting access`,async({page})=>{
  const {calls}=await setupCheckout(page,{responseStatus,code});await signInCheckout(page);await confirm(page).click();
  if(responseStatus===401){await expect(page).toHaveURL(/\/login\?next=/);await expect(page.getByLabel("Email",{exact:true})).toBeVisible();}
  else{await expect(page.getByRole("region", { name: "Xác nhận đăng ký", exact: true }).getByRole("alert")).toContainText(message);await expect(confirm(page)).toBeDisabled();}
  expect(calls.filter(c=>c.endsWith("/enrollment"))).toHaveLength(1);expect(calls.filter(c=>c.endsWith("/payments"))).toHaveLength(0);
  await expect(page.getByText("private diagnostic")).toHaveCount(0);await expect(page.locator('a[href*="meet.google"]')).toHaveCount(0);
});
test("pending submission prevents repeated mutations",async({page})=>{
  let release!:()=>void;const gate=new Promise<void>(resolve=>{release=resolve;});
  const {calls}=await setupCheckout(page,{gate});await signInCheckout(page);
  try{await confirm(page).click();const busy=page.getByRole("button",{name:"Đang xác nhận…",exact:true});await expect(busy).toBeDisabled();
    await busy.evaluate(button=>{(button as HTMLButtonElement).click();(button as HTMLButtonElement).click();});
    await expect.poll(()=>calls.filter(c=>c.endsWith("/enrollment")).length).toBe(1);
  }finally{release();}
  await expect(page).toHaveURL(`/student/enrollments?id=${enrollmentId}`);await expect(page.getByText(/Nội dung chuyển khoản:/)).toBeVisible();
  expect(calls.filter(c=>c.endsWith("/payments"))).toHaveLength(1);
});
test("uncertain network outcome allows explicit idempotent retry only",async({page})=>{
  const {calls}=await setupCheckout(page,{network:true,responseStatus:200});await signInCheckout(page);await confirm(page).click();
  await expect(page.getByRole("region", { name: "Xác nhận đăng ký", exact: true }).getByRole("alert")).toContainText("Chưa xác nhận được kết quả.");expect(calls.filter(c=>c.endsWith("/enrollment"))).toHaveLength(1);
  await page.getByRole("button",{name:"Tải lại thông tin"}).click();await expect(confirm(page)).toBeEnabled();await confirm(page).click();
  await expect(page).toHaveURL(`/student/enrollments?id=${enrollmentId}`);expect(calls.filter(c=>c.endsWith("/enrollment"))).toHaveLength(2);
});
test("existing ACTIVE relationship navigates without claiming a new reservation",async({page})=>{
  const {calls}=await setupCheckout(page,{active:true});await signInCheckout(page);
  await expect(page.getByText("Bạn đang học khóa học này.")).toBeVisible();await expect(confirm(page)).toBeDisabled();
  await expect(page.getByRole("link",{name:"Xem đăng ký hiện có"})).toHaveAttribute("href",`/student/enrollments?id=${enrollmentId}`);
  expect(calls.filter(c=>c.endsWith("/enrollment")||c.endsWith("/payments"))).toEqual([]);
});
test("anonymous direct entry and refresh never reserve; login preserves checkout",async({page})=>{
  const {calls}=await setupCheckout(page);await page.goto(checkoutPath);await expect(page).toHaveURL(/\/login\?next=/);
  await page.reload();await expect(page.getByLabel("Email",{exact:true})).toBeVisible();expect(calls.filter(c=>c.includes("/student/"))).toEqual([]);
  await page.goto(`/courses/${courseId}`);await signInCheckout(page);await expect(confirm(page)).toBeEnabled();
  await page.reload();await expect(confirm(page)).toBeEnabled();expect(calls.filter(c=>c.endsWith("/enrollment")||c.endsWith("/payments"))).toEqual([]);
});
