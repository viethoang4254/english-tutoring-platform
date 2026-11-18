import {expect,test} from "@playwright/test";
import demos from "../../../backend/src/test/resources/demo-courses.json";
import {captureReview} from "../support/ui-review";

test("@demo-courses four synthetic public projections preserve safe discovery and details",async({page},info)=>{
  expect(demos).toHaveLength(4);expect(demos.flatMap(d=>d.topics)).toHaveLength(20);
  // Public projection fixture only: the disposable database seed remains DRAFT until legitimate publication.
  const courses=demos.map((d,i)=>({id:String(9100+i),name:d.name,description:d.description,
    tuition:{amount:d.amount,currency:"VND"},category:{id:String(i+1),name:d.category},
    teacher:{id:"21",fullName:"Giáo viên demo"},plannedStartDate:d.startDate,timezone:"Asia/Ho_Chi_Minh",sessionCount:5}));
  await page.route("**/api/v1/**",route=>{
    const path=new URL(route.request().url()).pathname;
    if(path==="/api/v1/public/courses")return route.fulfill({json:{items:courses,page:0,size:20,hasNext:false}});
    const index=courses.findIndex(c=>path===`/api/v1/public/courses/${c.id}`);
    if(index>=0){const d=demos[index];return route.fulfill({json:{course:courses[index],rules:[{id:"1",dayOfWeek:d.weekday,startTime:d.startTime,endTime:d.endTime}],outline:{introduction:d.description,objectives:d.objectives,audience:d.level,requirements:d.requirements,outcomes:d.outcomes,stages:[{title:"Năm buổi thực hành",topics:d.topics.map((title,i)=>({title:`Buổi ${i+1}: ${title}`,description:d.sessionObjectives[i]}))}]}}});}
    if(path.endsWith('/csrf'))return route.fulfill({json:{token:'synthetic-csrf'}});
    if(path.endsWith('/refresh'))return route.fulfill({json:{accessToken:'synthetic-student',tokenType:'Bearer'}});
    if(path.endsWith('/users/me'))return route.fulfill({json:{id:'1',role:'STUDENT',emailVerified:true}});
    if(path.endsWith('/profile'))return route.fulfill({json:{fullName:'Học viên demo',email:'demo@example.invalid'}});
    return route.fulfill({status:401,json:{code:"AUTHENTICATION_REQUIRED",message:"Authentication required"}});
  });
  await page.goto("/courses");
  for(const d of demos)await expect(page.getByRole("heading",{name:d.name,exact:true})).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await captureReview(page,info,"course-discovery");
  await page.goto("/courses/9100");
  await expect(page.getByRole("heading",{level:1})).toHaveText(demos[0].name);
  await expect(page.getByText("5 buổi học · Múi giờ: Asia/Ho_Chi_Minh",{exact:true})).toBeVisible();
  await page.getByText("1. Năm buổi thực hành",{exact:true}).click();
  for(const [i,title] of demos[0].topics.entries()){await expect(page.getByText(`Buổi ${i+1}: ${title}`,{exact:true})).toBeVisible();await expect(page.getByText(demos[0].sessionObjectives[i],{exact:true})).toBeVisible();}
  await expect(page.locator('a[href*="meet.google.com"]')).toHaveCount(0);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await captureReview(page,info,"demo-course-detail");
  for(const [index,d] of demos.entries()){
    await page.goto(`/student/dashboard/courses/${9100+index}`);
    await expect(page.getByRole('heading',{level:1,name:d.name,exact:true})).toBeVisible();
    const roadmap=page.getByRole('region',{name:'Lộ trình khóa học',exact:true});
    await expect(roadmap).toBeVisible();
    await expect(roadmap.getByText(/90 phút\/buổi/)).toBeVisible();
    await roadmap.locator('summary').click();
    for(const [i,title] of d.topics.entries()){
      await expect(roadmap.getByText(`Buổi ${i+1}: ${title}`,{exact:true})).toBeVisible();
      await expect(roadmap.getByText(d.sessionObjectives[i],{exact:true})).toBeVisible();
    }
    await expect(page.getByText(d.outcomes,{exact:true})).toBeVisible();
    await expect(page.getByText(d.requirements,{exact:false})).toBeVisible();
    await expect(page.locator('a[href*="meet.google.com"]')).toHaveCount(0);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
    if(index===0)await captureReview(page,info,'student-course-roadmap');
  }
});
