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
 const nav=page.locator('#dashboard-navigation');await expect(nav).toBeAttached();
 if(status==='APPROVED')await expect(nav.locator('a[href="/teacher/courses"]')).toHaveCount(1);
 else await expect(nav.locator('a[href="/teacher/courses"]')).toHaveCount(0);
 await page.getByRole('contentinfo').getByRole('link',{name:'Đăng nhập',exact:true}).click();
 await expect(page).toHaveURL(/\/teacher$/);
});
