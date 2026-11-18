import {expect,test} from "@playwright/test";
import {captureReview} from "../support/ui-review";

test("@public-pages public navigation and contact validation, failure and confirmed submission",async({page},info)=>{
  let submissions=0,available=false;
  await page.route("**/api/v1/**",async route=>{
    const path=new URL(route.request().url()).pathname;
    if(path==="/api/v1/auth/csrf")return route.fulfill({json:{token:"synthetic-contact-csrf"}});
    if(path==="/api/v1/auth/contact"){
      submissions++;expect(route.request().method()).toBe("POST");
      expect(route.request().headers()["x-xsrf-token"]).toBe("synthetic-contact-csrf");
      expect(route.request().postDataJSON()).toEqual({fullName:"Học viên demo",email:"demo@example.invalid",phone:"",subject:"Tìm hiểu khóa học",message:"Tôi muốn tìm hiểu lịch học trực tuyến."});
      return route.fulfill(available?{json:{status:"ACCEPTED_BY_MAIL_SERVER"}}:{status:503,json:{code:"CONTACT_UNAVAILABLE",message:"Unavailable"}});
    }
    return route.fulfill({status:401,json:{code:"AUTHENTICATION_REQUIRED",message:"Authentication required"}});
  });
  await page.goto("/about");
  await expect(page.getByRole("heading",{level:1})).toHaveText("Học theo khóa, tiến bộ cùng gia sư");
  await expect(page.getByRole("link",{name:"Khám phá khóa học",exact:true})).toHaveAttribute("href","/courses");
  await expect(page.getByRole("link",{name:"Đăng ký giáo viên",exact:true})).toHaveAttribute("href","/become-a-teacher");
  await expect(page.locator("footer").getByRole("link",{name:"Giới thiệu",exact:true})).toHaveAttribute("href","/about");
  await captureReview(page,info,"about");
  if(info.project.name==="mobile")await page.locator("header summary").click();
  const nav=page.getByRole("navigation",{name:info.project.name==="mobile"?"Điều hướng di động":"Điều hướng chính",exact:true});
  await nav.getByRole("link",{name:"Liên hệ",exact:true}).click();
  await expect(page).toHaveURL(/\/contact$/);
  await expect(page.getByRole("heading",{level:1})).toHaveText("Liên hệ");
  await expect(page.getByRole("navigation",{name:"Đường dẫn"})).toBeVisible();
  await expect(page.getByRole("heading",{name:"Thông tin liên hệ",exact:true})).toBeVisible();
  const contact=page.getByRole("region",{name:"Gửi lời nhắn",exact:true});
  await expect(page.getByLabel("Số điện thoại (không bắt buộc)")).toBeVisible();
  await page.getByRole("button",{name:"Gửi lời nhắn"}).click();expect(submissions).toBe(0);
  await page.getByLabel("Họ và tên",{exact:true}).fill("Học viên demo");
  await page.getByLabel("Email",{exact:true}).fill("invalid");
  await page.getByLabel("Chủ đề",{exact:true}).fill("Tìm hiểu khóa học");
  await page.getByLabel("Nội dung",{exact:true}).fill("Tôi muốn tìm hiểu lịch học trực tuyến.");
  await page.getByRole("button",{name:"Gửi lời nhắn"}).click();expect(submissions).toBe(0);
  await page.getByLabel("Email",{exact:true}).fill("demo@example.invalid");
  await page.getByRole("button",{name:"Gửi lời nhắn"}).click();
  // Next.js also exposes a page-level route-announcement alert.
  await expect(contact.getByRole("alert")).toContainText("Chưa gửi lời nhắn");
  available=true;await page.getByRole("button",{name:"Gửi lời nhắn"}).click();
  await expect(contact.getByRole("status")).toContainText("Máy chủ thư đã tiếp nhận");expect(submissions).toBe(2);
  await expect(page.getByLabel("Nội dung",{exact:true})).toHaveValue("");
  await expect(page.locator("footer").getByRole("link",{name:"Gửi lời nhắn",exact:true})).toHaveAttribute("href","/contact");
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await captureReview(page,info,"contact");
});
