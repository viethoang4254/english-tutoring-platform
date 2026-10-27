import { expect, test } from '@playwright/test';
for (const entry of ['/login', '/']) test(`Student login from ${entry} establishes session before redirect and logout protects dashboard`, async ({page}) => {
 let fail = true; let refreshes = 0;
 await page.route('**/api/v1/**', async route => {
  const path = new URL(route.request().url()).pathname;
  if(path.endsWith('/csrf')) return route.fulfill({json:{token:'test-csrf'}});
  if(path.endsWith('/login')) return route.fulfill(fail ? {status:401,json:{code:'INVALID_CREDENTIALS'}} : {json:{accessToken:'test-access',tokenType:'Bearer'}});
  if(path.endsWith('/users/me')) return route.fulfill({json:{id:'1',role:'STUDENT',emailVerified:false}});
  if(path.endsWith('/profile')) return route.fulfill({json:{fullName:'Học viên thử nghiệm',email:'student@example.test'}});
  if(path.endsWith('/refresh')) refreshes++;
  return route.fulfill({status:204});
 });
 await page.goto(entry);
 if(entry==='/') await page.getByRole('button',{name:'Tài khoản',exact:true}).click();
 await page.getByLabel('Email',{exact:true}).fill('student@example.test');
 await page.getByLabel('Mật khẩu',{exact:true}).fill('password');
 await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
 await expect(page.getByRole('button',{name:'Đăng nhập',exact:true})).toBeEnabled();
 await expect(page).toHaveURL(new RegExp(entry === '/' ? '/$' : '/login$'));
 fail=false;
 await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
 await expect(page).toHaveURL(/\/student\/dashboard$/);
 await expect(page.getByRole('heading',{name:/Xin chào,/})).toBeVisible();
 // Client navigation preserves the in-memory session; full reload deliberately does not.
 for(const label of ['Đăng nhập','Đăng ký học viên']) {
  await page.getByRole('contentinfo').getByRole('link',{name:label,exact:true}).click();
  await expect(page).toHaveURL(/\/student\/dashboard$/);
  await expect(page.getByRole('heading',{name:/Xin chào,/})).toBeVisible();
 }
 await page.getByRole('contentinfo').getByRole('link',{name:'Trang chủ',exact:true}).click();
 await expect(page).toHaveURL(/\/#trang-chu$/);
 await page.locator('.header-account-menu summary').click();
 await page.getByRole('link',{name:'Bảng học tập',exact:true}).click();
 await expect(page).toHaveURL(/\/student\/dashboard$/);
 await page.locator('.header-account-menu summary').click();
 await page.getByRole('button',{name:'Đăng xuất',exact:true}).click();
 await expect(page).toHaveURL(/\/login\?next=\/student\/dashboard$/);
 await expect(page.getByLabel('Email',{exact:true})).toBeVisible();
 expect(refreshes).toBe(0);
});