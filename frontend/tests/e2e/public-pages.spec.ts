import {expect,test} from "@playwright/test";
import {captureReview} from "../support/ui-review";

test("@public-pages public navigation and contact validation, failure and confirmed submission",async({page},info)=>{
  // Assert our iframe contract without depending on the external Google response.
  await page.route("https://www.google.com/maps/embed?*",route=>route.fulfill({contentType:"text/html",body:"<!doctype html><html lang='vi'><body></body></html>"}));
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
  await expect(page.getByRole("heading",{level:1})).toHaveText("Về chúng tôi");
  await expect(page.getByRole("link",{name:"Khám phá khóa học",exact:true})).toHaveAttribute("href","/courses");
  await expect(page.locator('a[href="/become-a-teacher"]')).toHaveCount(0);
  for(const title of ["Chào mừng bạn đến với Tiếng Anh Cùng Gia Sư","Mục tiêu của chúng tôi","Học tiếng Anh mọi lúc, mọi nơi","Đồng hành cùng giáo viên","Học tập linh hoạt","Lộ trình rõ ràng","Kết nối giáo viên","Trải nghiệm minh bạch"])
    await expect(page.getByRole('heading',{name:title,exact:true})).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  for(const heading of await page.locator('main h1, main h2').all()){
    const box=await heading.boundingBox();expect(box).not.toBeNull();expect(box!.width).toBeGreaterThan(200);
  }
  await expect(page.locator("footer").getByRole("link",{name:"Giới thiệu",exact:true})).toHaveAttribute("href","/about");
  await captureReview(page,info,"about");
  await page.getByRole('link',{name:'Khám phá khóa học',exact:true}).click();
  await expect(page).toHaveURL(/\/courses$/);
  await page.goto('/about');
  if(info.project.name==="mobile")await page.locator("header summary").click();
  const nav=page.getByRole("navigation",{name:info.project.name==="mobile"?"Điều hướng di động":"Điều hướng chính",exact:true});
  await nav.getByRole("link",{name:"Liên hệ",exact:true}).click();
  await expect(page).toHaveURL(/\/contact$/);
  await expect(page.getByRole("heading",{level:1})).toHaveText("Liên hệ");
  await expect(page.getByRole("navigation",{name:"Đường dẫn"})).toBeVisible();
  await expect(page.getByRole("heading",{name:"Giữ liên lạc với chúng tôi",exact:true})).toBeVisible();
  const contact=page.getByRole("region",{name:"Gửi tin nhắn",exact:true});
  await expect(page.getByLabel("Số điện thoại (không bắt buộc)")).toBeVisible();
  await expect(page.getByRole('heading',{name:'Đăng ký nhận tư vấn',exact:true})).toBeVisible();
  const mapFrame=page.locator('main iframe');
  await expect(mapFrame).toBeVisible();
  await expect(mapFrame).toHaveAttribute('src','https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3834.639338406222!2d108.22228985!3d16.03227955!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x314219ee598df9c5%3A0x3109ad2f035819b5!2zNTY2IE7DumkgVGjDoG5oLCBIw7JhIEPGsOG7nW5nLCDEkMOgIE7hurVuZywgVmnhu4d0IE5hbQ!5e0!3m2!1svi!2s!4v1795005936915!5m2!1svi!2s');
  await expect(mapFrame).toHaveAttribute('src',/^https:\/\/www\.google\.com\/maps\/embed\?pb=/);
  await expect(mapFrame).toHaveAttribute('title','Bản đồ địa chỉ 566 Núi Thành, Đà Nẵng');
  const frameBox=await mapFrame.boundingBox();expect(frameBox).not.toBeNull();expect(frameBox!.height).toBe(info.project.name==="mobile"?300:420);
  await expect(page.locator('main dt')).toHaveCount(3);
  await expect(page.locator('main dd').filter({hasText:'566 Núi Thành, phường Hòa Cường, thành phố Đà Nẵng, Việt Nam'})).toBeVisible();
  await expect(page.getByRole('link',{name:'0905700682',exact:true})).toHaveAttribute('href','tel:+84905700682');
  await expect(page.getByRole('link',{name:'viehoang21104@gmail.com',exact:true})).toHaveAttribute('href','mailto:viehoang21104@gmail.com');
  const map=page.getByRole('region',{name:'Vị trí liên hệ',exact:true});
  await expect(map).toContainText('Tiếng Anh Cùng Gia Sư');
  await expect(map).toContainText('566 Núi Thành, phường Hòa Cường, thành phố Đà Nẵng, Việt Nam');
  await expect(map.getByRole('link',{name:/Mở Google Maps/})).toHaveAttribute('href','https://www.google.com/maps/place/566+N%C3%BAi+Th%C3%A0nh,+H%C3%B2a+C%C6%B0%E1%BB%9Dng,+%C4%90%C3%A0+N%E1%BA%B5ng,+Vi%E1%BB%87t+Nam/@16.0321534,108.219591,17z/data=!3m1!4b1!4m6!3m5!1s0x314219ee4554218d:0x7ff6c1a286a840!8m2!3d16.0321534!4d108.2221659!16s%2Fg%2F11wr155fsn?hl=vi&entry=ttu&g_ep=EgoyMDI2MTExNS4wIKXMDSoASAFQAw%3D%3D');
  const mapBox=await map.boundingBox();expect(mapBox).not.toBeNull();expect(mapBox!.width).toBeGreaterThan(250);
  await expect(page.locator('main')).not.toContainText('{{CONTACT_');
  await expect(page.locator('footer a[href="/become-a-teacher"]')).toHaveCount(1);
  await page.getByRole('link',{name:'Gửi nhu cầu tư vấn',exact:true}).click();
  await expect(page).toHaveURL(/#gui-tin-nhan$/);
  await page.getByRole("button",{name:"Gửi tin nhắn"}).click();expect(submissions).toBe(0);
  await page.getByLabel("Họ và tên",{exact:true}).fill("Học viên demo");
  await page.getByLabel("Email",{exact:true}).fill("invalid");
  await page.getByLabel("Tiêu đề",{exact:true}).fill("Tìm hiểu khóa học");
  await page.getByLabel("Nội dung",{exact:true}).fill("Tôi muốn tìm hiểu lịch học trực tuyến.");
  await page.getByRole("button",{name:"Gửi tin nhắn"}).click();expect(submissions).toBe(0);
  await page.getByLabel("Email",{exact:true}).fill("demo@example.invalid");
  await page.getByRole("button",{name:"Gửi tin nhắn"}).click();
  // Next.js also exposes a page-level route-announcement alert.
  await expect(contact.getByRole("alert")).toContainText("Chưa gửi lời nhắn");
  available=true;await page.getByRole("button",{name:"Gửi tin nhắn"}).click();
  await expect(contact.getByRole("status")).toContainText("Máy chủ thư đã tiếp nhận");expect(submissions).toBe(2);
  await expect(page.getByLabel("Nội dung",{exact:true})).toHaveValue("");
  await expect(page.locator("footer").getByRole("link",{name:"Gửi lời nhắn",exact:true})).toHaveAttribute("href","/contact");
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await captureReview(page,info,"contact");
});
