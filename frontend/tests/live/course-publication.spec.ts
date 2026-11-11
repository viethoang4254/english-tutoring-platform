import { expect, test } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { api, fixture, mutation, register } from '../support/live-fixtures';

test('owner publishes a draft visible across public and Student catalogs', async ({page,request}) => {
  test.setTimeout(180_000);
  expect(process.env.GITHUB_ACTIONS).toBe('true');
  expect(process.env.TASK009_CONTAINER).toMatch(/^task009-[a-f0-9]+$/);
  const admin = await register(request,false);
  fixture('fixture',{action:'admin',id:admin.id});
  const adminLogin = await mutation(request,'/auth/login',{email:admin.email,password:admin.password});
  expect(adminLogin.status()).toBe(200);
  const adminHeaders = {Authorization:'Bearer '+(await adminLogin.json()).accessToken};
  const owner = await register(request,true);
  const application = await request.get(api+'/teacher/onboarding',{headers:owner.headers});
  expect(application.status()).toBe(200);
  expect((await request.post(api+'/admin/teacher-applications/'+(await application.json()).applicationId+'/approve',{headers:adminHeaders})).status()).toBe(204);
  const title = 'Publication '+randomUUID();
  const category = await request.post(api+'/admin/categories',{headers:adminHeaders,data:{name:title}});
  expect(category.status()).toBe(201);
  const created = await request.post(api+'/teacher/courses',{headers:owner.headers,data:{
    name:title,description:'Publication test',categoryId:(await category.json()).id,tuition:{amount:'1500000.00',currency:'VND'},
    plannedStartDate:new Date(Date.now()+30*86400000).toISOString().slice(0,10),timezone:'Asia/Ho_Chi_Minh',minStudents:1,maxStudents:10,sessionCount:2
  }});
  expect(created.status()).toBe(201);
  const id = (await created.json()).id;
  const course = api+'/teacher/courses/'+id;
  expect((await request.get(api+'/public/courses/'+id)).status()).toBe(404);
  const other = await register(request,true);
  const otherApplication = await request.get(api+'/teacher/onboarding',{headers:other.headers});
  expect(otherApplication.status()).toBe(200);
  expect((await request.post(api+'/admin/teacher-applications/'+(await otherApplication.json()).applicationId+'/approve',{headers:adminHeaders})).status()).toBe(204);
  expect((await request.post(course+'/publish',{headers:other.headers,data:{meetUrl:'https://meet.google.com/abc-defg-hij'}})).status()).toBe(404);
  expect((await request.put(course+'/schedule',{headers:owner.headers,data:{rules:[{dayOfWeek:2,startTime:'09:00',endTime:'10:00'}]}})).status()).toBe(200);
  expect((await request.post(course+'/sessions/generate',{headers:owner.headers})).status()).toBe(201);
  const expectBankConflict = async () => {
    const result = await request.post(course+'/publish',{headers:owner.headers,data:{meetUrl:'https://meet.google.com/abc-defg-hij'}});
    expect(result.status()).toBe(409);
    expect((await result.json()).code).toBe('COURSE_RECEIVING_ACCOUNT_REQUIRED');
    expect((await request.get(api+'/public/courses/'+id)).status()).toBe(404);
  };
  await expectBankConflict();
  for (const decision of ['reject','approve']) {
    const bankId=fixture('fixture',{action:'publication-account',id:owner.id});
    await expectBankConflict();
    expect((await request.post(api+'/admin/payment-accounts/'+bankId+'/'+decision,{
      headers:adminHeaders,...(decision==='reject'?{data:{reason:'CI review'}}:{})
    })).status()).toBe(204);
    if(decision==='reject') await expectBankConflict();
  }
  await page.goto('/login');
  await page.getByLabel('Email',{exact:true}).fill(owner.email);
  await page.getByLabel('Mật khẩu',{exact:true}).fill(owner.password);
  await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
  await expect(page).toHaveURL(/\/teacher$/);
  await page.evaluate(path=>(window as unknown as {next:{router:{push:(p:string)=>void}}}).next.router.push(path),'/teacher/courses/'+id);
  await page.getByText('Giới thiệu và lộ trình học',{exact:true}).click();
  await page.getByLabel('Mục tiêu học tập',{exact:true}).fill('Discuss travel plans');
  await page.getByRole('button',{name:'Thêm giai đoạn'}).click();
  await page.getByLabel('Tên giai đoạn').fill('Travel conversations');
  await page.getByRole('button',{name:'Thêm chủ đề'}).click();
  await page.getByLabel('Tên chủ đề').fill('At the station');
  await page.getByLabel('Mô tả chủ đề').fill('Ask for departure times');
  await page.getByRole('button',{name:'Lưu nội dung công khai'}).click();
  await expect(page.getByText('Đã lưu nội dung giới thiệu và lộ trình.')).toBeVisible();
  const saved = await request.get(course+'/outline',{headers:owner.headers});
  expect(saved.status()).toBe(200);
  expect((await saved.json()).stages[0].topics[0].title).toBe('At the station');
  await page.getByLabel('Liên kết Google Meet').fill('https://meet.google.com/abc-defg-hij');
  const response=page.waitForResponse(r=>r.url()===course+'/publish' && r.request().method()==='POST');
  await page.getByRole('button',{name:'Xuất bản khóa học'}).click();
  expect((await response).status()).toBe(200);
  await expect(page.getByText('Đã xuất bản khóa học.',{exact:true})).toBeVisible();
  const detail = await request.get(api+'/public/courses/'+id);
  expect(detail.status()).toBe(200);
  const publicDetail = await detail.json();
  expect(publicDetail.course).toMatchObject({id,name:title,teacher:{id:owner.id},sessionCount:2,tuition:{amount:'1500000.00',currency:'VND'}});
  expect(publicDetail.rules).toHaveLength(1);
  expect(JSON.stringify(publicDetail)).not.toContain('meet.google.com');
  for(const path of ['/','/courses']){
    await page.goto(path);
    await expect(page.getByRole('link',{name:title,exact:true})).toHaveAttribute('href','/courses/'+id);
  }
  await page.getByRole('link',{name:title,exact:true}).click();
  await expect(page).toHaveURL(new RegExp('/courses/'+id+'$'));
  await expect(page.getByRole('heading',{name:title,exact:true})).toBeVisible();
  await expect(page.getByText('Discuss travel plans',{exact:true})).toBeVisible();
  await page.getByText('1. Travel conversations',{exact:true}).click();
  await expect(page.getByText('Ask for departure times',{exact:true})).toBeVisible();
  const student=await register(request,false);
  await page.goto('/login');
  await page.getByLabel('Email',{exact:true}).fill(student.email);
  await page.getByLabel('Mật khẩu',{exact:true}).fill(student.password);
  await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
  await expect(page).toHaveURL(/\/student\/dashboard$/);
  const toggle=page.locator('button[aria-controls="student-navigation"]');
  if(await toggle.isVisible())await toggle.click();
  await page.locator('#student-navigation').getByRole('link',{name:'Khám phá khóa học'}).click();
  await page.getByRole('link',{name:title,exact:true}).click();
  await expect(page).toHaveURL(new RegExp('/student/dashboard/courses/'+id+'$'));
  await expect(page.getByRole('heading',{name:title,exact:true})).toBeVisible();
  await expect(page.getByText('Discuss travel plans',{exact:true})).toBeVisible();
  await page.getByText('1. Travel conversations',{exact:true}).click();
  await expect(page.getByText('Ask for departure times',{exact:true})).toBeVisible();
});
