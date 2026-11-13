import { test, expect } from '@playwright/test';
for (const status of ['APPROVED', 'PENDING', 'REJECTED']) test(`Teacher dedicated login enters shared Dashboard with ${status} permissions`, async ({page}) => {
 let fail=true;
 await page.route('**/api/v1/**',r=>{
  const path=new URL(r.request().url()).pathname;
  if(path.endsWith('/csrf'))return r.fulfill({json:{token:'fixture'}});
  if(path.endsWith('/login'))return r.fulfill(fail?{status:401,json:{code:'INVALID_CREDENTIALS'}}:{json:{accessToken:'fixture',tokenType:'Bearer'}});
  if(path.endsWith('/users/me'))return r.fulfill({json:{id:'1',role:'TEACHER',emailVerified:true}});
  if(path.endsWith('/onboarding'))return r.fulfill({json:{applicationId:'1',status}});
  if(path.endsWith('/profile'))return r.fulfill({json:{id:'1',fullName:'Fixture',email:'fixture@example.invalid',emailVerified:true,username:null,phone:null}});
  if(path.endsWith('/courses'))return r.fulfill({json:{items:[],page:0,size:20,hasNext:false}});
  return r.fulfill({status:403,json:{code:'ACCESS_DENIED'}});
 });
 await page.goto('/login?next=/teacher');
 await page.getByLabel('Email',{exact:true}).fill('fixture@example.invalid');
 await page.getByLabel('Mật khẩu',{exact:true}).fill('fixture-password');
 await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
 await expect(page.getByRole('status')).not.toBeEmpty();
 await expect(page).toHaveURL(/\/login\?next=/);
 fail=false;
 await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
 await expect(page).toHaveURL(/\/teacher$/);
 await expect(page.getByRole('heading',{level:1,name:'Tổng quan',exact:true})).toBeVisible();
 const nav=page.locator('aside nav');await expect(nav).toBeAttached();
 if(status==='APPROVED')await expect(nav.locator('a[href="/teacher/courses"]')).toHaveCount(1);
 else await expect(nav.locator('a[href="/teacher/courses"]')).toHaveCount(0);
 await expect(page.getByRole('contentinfo')).toHaveCount(0);
 await expect(page).toHaveURL(/\/teacher$/);
});

for(const remember of [false,true]) test(`Login persistence and logout remember=${remember}`,async({page})=>{
 let refreshes=0,logouts=0;
 await page.route('**/api/v1/**',r=>{
  const path=new URL(r.request().url()).pathname;
  expect(new URL(r.request().url()).search).toBe('');
  if(path.endsWith('/csrf'))return r.fulfill({json:{token:'fixture'}});
  if(path.endsWith('/login')){
   expect(r.request().method()).toBe('POST');expect(r.request().postDataJSON().rememberMe).toBe(remember);
   return r.fulfill({json:{accessToken:'fixture',tokenType:'Bearer'}});
  }
  if(path.endsWith('/refresh')){refreshes++;return r.fulfill({json:{accessToken:'rotated',tokenType:'Bearer'}});}
  if(path.endsWith('/logout')){logouts++;return r.fulfill({status:204});}
  if(path.endsWith('/users/me'))return r.fulfill({json:{id:'1',role:'TEACHER',emailVerified:false}});
  if(path.endsWith('/onboarding'))return r.fulfill({json:{applicationId:'1',status:'APPROVED'}});
  return r.fulfill({status:204});
 });
 await page.goto('/login');
 await page.getByLabel('Email',{exact:true}).fill('fixture@example.invalid');
 await page.getByLabel('Mật khẩu',{exact:true}).fill('fixture-password');
 await page.getByLabel('Ghi nhớ đăng nhập').setChecked(remember);
 await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
 await expect(page).toHaveURL(/\/teacher$/);
 expect(await page.evaluate(()=>localStorage.getItem('remember-login'))).toBe(remember?'1':null);
 await page.reload();
 if(remember){
  await expect(page.getByRole('heading',{level:1,name:'Tổng quan',exact:true})).toBeVisible();
  expect(refreshes).toBe(1);
  await page.locator('[data-dashboard-account] summary').click();
  await page.locator('[data-dashboard-account]').getByRole('button',{name:'Đăng xuất',exact:true}).click();
  await expect.poll(()=>logouts).toBe(1);
  expect(await page.evaluate(()=>localStorage.getItem('remember-login'))).toBeNull();
  await page.reload();
 }
 await expect(page).toHaveURL(remember?/\/account$/:/\/login/);
 await expect(page.getByLabel('Email',{exact:true})).toBeVisible();
 expect(refreshes).toBe(remember?1:0);
 expect(page.url()).not.toContain('fixture-password');
});
test('Login shows Vietnamese validation and bounded network errors',async({page})=>{
 let mode='offline';let requests=0;
 await page.route('**/api/v1/**',r=>{
  const path=new URL(r.request().url()).pathname;
  if(path.endsWith('/csrf'))return r.fulfill({json:{token:'fixture'}});
  requests++;
  if(mode==='offline')return r.abort();
  return r.fulfill({status:500,json:{code:'INTERNAL_ERROR',message:'PRIVATE'}});
 });
 await page.goto('/login');
 const submit=page.getByRole('button',{name:'Đăng nhập',exact:true});
 await submit.click();await expect(page.getByRole('status')).toHaveText('Vui lòng nhập email.');
 await page.getByLabel('Email',{exact:true}).fill('invalid');await submit.click();await expect(page.getByRole('status')).toHaveText('Email không hợp lệ.');
 await page.getByLabel('Email',{exact:true}).fill('fixture@example.invalid');await submit.click();await expect(page.getByRole('status')).toHaveText('Vui lòng nhập mật khẩu.');
 expect(requests).toBe(0);
 await page.getByLabel('Mật khẩu',{exact:true}).fill('fixture-password');await submit.click();
 await expect(page.getByRole('status')).toHaveText('Không thể kết nối đến máy chủ. Vui lòng thử lại.');
 mode='server';await submit.click();await expect(page.getByRole('status')).toHaveText('Hệ thống đang gặp sự cố. Vui lòng thử lại sau.');
 expect(page.url()).not.toContain('password');
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});
