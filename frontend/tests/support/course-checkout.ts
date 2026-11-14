import {readFileSync} from "node:fs";
import {join} from "node:path";
import {expect,type Page} from "@playwright/test";
import type {Checkout} from "../../src/features/enrollment/course-checkout";
import type {Enrollment} from "../../src/features/enrollment/enrollment";
export const courseId="9007199254740993", enrollmentId="9007199254740994";
export const checkoutPath=`/student/dashboard/courses/${courseId}/checkout`;
export const row:Enrollment={id:enrollmentId,courseId,courseTitle:"Khóa học đăng ký",status:"PENDING",enrolledAt:null,seatReservedUntil:"2030-01-01T00:15:00Z",reservationExpired:false,cancelledAt:null,cancellationReason:null,createdAt:"2030-01-01T00:00:00Z",updatedAt:"2030-01-01T00:00:00Z"};
export const quote:Checkout={course:{id:courseId,name:row.courseTitle,description:"Thông tin công khai",tuition:{amount:"2000000.00",currency:"VND"},category:{id:"11",name:"Giao tiếp"},teacher:{id:"21",fullName:"Gia sư An"},plannedStartDate:"2030-01-08",timezone:"Asia/Ho_Chi_Minh",sessionCount:12},
  rules:[{id:"1",dayOfWeek:2,startTime:"19:00:00",endTime:"20:30:00"}],
  price:{originalAmount:"2000000.00",discountPercent:"10",exactDiscountAmount:"200000",roundingAdjustment:"0",finalAmount:"1800000",currency:"VND"},
  enrollment:null,paymentId:null,available:true,blockedReason:null,calculatedAt:"2030-01-01T00:00:00Z"};
export const payment={id:"7",enrollmentId,status:"PENDING",originalAmount:"2000000",discountPercent:"10",exactDiscountAmount:"200000",roundingAdjustment:"0",finalAmount:"1800000",currency:"VND",bankName:"Ngân hàng minh họa",accountNumber:"00123456789",accountHolderName:"TAI KHOAN KIEM THU",transferReference:"ELPFIXTURE123",expiresAt:row.seatReservedUntil};
// Version 1 QR, harmless payload "CHECKOUT DEMO", four-module quiet zone.
const png=readFileSync(join(__dirname,"checkout-demo.png"));
type Options={role?:"STUDENT"|"TEACHER"|"ADMIN";responseStatus?:number;code?:string;network?:boolean;active?:boolean;gate?:Promise<void>;existingPayment?:boolean;priceChanged?:boolean;paymentFailure?:boolean;blocked?:string};
export async function setupCheckout(page:Page,options:Options={}) {
  await page.clock.setFixedTime(new Date(quote.calculatedAt));
  const calls:string[]=[];
  let current:Enrollment={...row,...(options.active?{status:"ACTIVE" as const}:{})};
  let latest=options.existingPayment?{...payment}:null;
  let enrollmentExists=!!options.existingPayment||!!options.active;
  let enrollmentRequests=0,paymentRequests=0,remembered=false;
  await page.route("**/api/v1/**",async route=>{
    const request=route.request(),path=new URL(request.url()).pathname;
    calls.push(`${request.method()} ${path}`);
    if(path.endsWith("/csrf"))return route.fulfill({json:{token:"fixture-csrf"}});
    if(path.endsWith("/login")) {
      expect(request.headers()["x-xsrf-token"]).toBe("fixture-csrf");remembered=!!request.postDataJSON()?.rememberMe;
      return route.fulfill({json:{accessToken:"fixture-token",tokenType:"Bearer"}});
    }
    if(path.endsWith("/refresh")&&remembered)return route.fulfill({json:{accessToken:"fixture-token",tokenType:"Bearer"}});
    if(path.endsWith("/users/me"))return route.fulfill({json:{id:"1",role:options.role??"STUDENT",emailVerified:false}});
    if(path.endsWith("/profile"))return route.fulfill({json:{fullName:"Học viên kiểm thử",email:"fixture@example.invalid"}});
    if(path.endsWith("/teacher/onboarding"))return route.fulfill({json:{applicationId:"1",status:"APPROVED",submittedAt:quote.calculatedAt,reviewedAt:quote.calculatedAt,rejectionReason:null}});
    if(path===`/api/v1/public/courses/${courseId}`)return route.fulfill({json:{course:quote.course,rules:quote.rules}});
    if(path.startsWith("/api/v1/student/"))expect(request.headers().authorization).toBe("Bearer fixture-token");
    if(path===`/api/v1/student/courses/${courseId}/checkout`)return route.fulfill({json:{...quote,enrollment:enrollmentExists?current:null,paymentId:latest?.id??null,available:!options.active&&!options.blocked,blockedReason:options.active?"ENROLLMENT_ACTIVE":options.blocked??null}});
    if(path===`/api/v1/student/courses/${courseId}/enrollment`) {
      expect(request.method()).toBe("POST");expect(request.postData()).toBeNull();expect(request.headers().cookie).toBeUndefined();enrollmentRequests++;
      if(options.gate)await options.gate;
      if(options.network&&enrollmentRequests===1)return route.abort();
      if(options.code)return route.fulfill({status:options.responseStatus??409,json:{code:options.code,message:"private diagnostic"}});
      enrollmentExists=true;return route.fulfill({status:options.responseStatus??201,json:current});
    }
    if(path===`/api/v1/student/enrollments/${enrollmentId}/payments`){
      expect(request.method()).toBe("POST");expect(request.postData()).toBeNull();paymentRequests++;
      latest??={...payment,...(options.priceChanged?{finalAmount:"1900000"}:{})};
      if(options.paymentFailure&&paymentRequests===1)return route.abort();
      return route.fulfill({status:options.existingPayment?200:201,json:latest});
    }
    if(path.endsWith("/payments/latest"))return latest?route.fulfill({json:{id:latest.id}}):route.fulfill({status:204});
    if(path===`/api/v1/student/payments/7`)return route.fulfill({json:latest});
    if(path===`/api/v1/student/payments/7/qr-image`){expect(request.headers().authorization).toBe("Bearer fixture-token");return route.fulfill({contentType:"image/png",body:png});}
    if(path===`/api/v1/student/enrollments/${enrollmentId}`)return route.fulfill({json:current});
    if(path===`/api/v1/student/enrollments`)return route.fulfill({json:{items:enrollmentExists?[current]:[],page:0,size:20,hasNext:false}});
    return route.fulfill({status:404,json:{code:"NOT_FOUND",message:"Not found"}});
  });
  await page.goto(`/courses/${courseId}`);
  await expect(page.getByRole("heading",{name:quote.course.name,exact:true})).toBeVisible();
  return {calls,confirmPayment:()=>{latest={...payment,status:"CONFIRMED"};current={...row,status:"ACTIVE",seatReservedUntil:null};},setPaymentStatus:(status:string)=>{latest={...payment,status};}};
}
export async function signInCheckout(page:Page,role="STUDENT") {
  await page.getByRole("link",{name:"Đăng nhập để đăng ký",exact:true}).click();
  await expect(page).toHaveURL(new RegExp("/login\\?next="));
  await page.getByLabel("Email",{exact:true}).fill("fixture@example.invalid");
  await page.getByLabel("Mật khẩu",{exact:true}).fill("fixture-password");
  await page.getByLabel("Ghi nhớ đăng nhập").check();
  await page.getByRole("button",{name:"Đăng nhập",exact:true}).click();
  await expect(page).toHaveURL(role==="STUDENT"?checkoutPath:role==="TEACHER"?/\/teacher$/:/\/admin\/dashboard$/);
  if(role==="STUDENT")await expect(page.getByRole("heading",{name:"Xác nhận đăng ký",exact:true})).toBeVisible();
}
export const confirm=(page:Page)=>page.getByRole("button",{name:"Xác nhận và thanh toán",exact:true});
