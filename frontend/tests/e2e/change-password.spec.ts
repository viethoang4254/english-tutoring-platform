import {expect,test} from '@playwright/test';
for(const status of [204,400,401,403,500,503]) test(`change password handles ${status} without retries`,async({page})=>{
 const calls:{body:unknown;headers:Record<string,string>}[]=[];
 await page.route('**/api/v1/**',async route=>{
  const r=route.request(),path=new URL(r.url()).pathname;
  if(r.method()==='OPTIONS'){await route.fulfill({status:204});return;}
  if(path.endsWith('/csrf')) await route.fulfill({json:{token:'csrf-test'}});
  else if(path.endsWith('/login')) await route.fulfill({json:{accessToken:'access-test',tokenType:'Bearer',issuedAt:'2026-10-15T00:00:00Z',expiresAt:'2099-10-15T00:15:00Z'}});
  else if(path.endsWith('/users/me')) await route.fulfill({json:{id:'1',role:'STUDENT',emailVerified:false}});
  else if(path.endsWith('/password/change')){calls.push({body:r.postDataJSON(),headers:r.headers()});await new Promise(resolve=>setTimeout(resolve,100));await route.fulfill(status===204?{status}:{status,json:{code:status===503?'AUTHENTICATION_OUTCOME_UNKNOWN':'INTERNAL_ERROR',message:'PRIVATE'}});}
  else await route.abort();
 });
 await page.goto('/account');await page.getByLabel('Email',{exact:true}).fill('student@example.test');await page.getByLabel('Mật khẩu',{exact:true}).fill('old password');await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
 await page.getByText('Đổi mật khẩu',{exact:true}).click();
 await page.getByLabel('Mật khẩu hiện tại',{exact:true}).fill('old password');await page.getByLabel('Mật khẩu mới',{exact:true}).fill('new password');await page.getByLabel('Xác nhận mật khẩu',{exact:true}).fill('mismatch password');await page.getByRole('button',{name:'Lưu mật khẩu',exact:true}).click();
 await expect(page.getByText('Mật khẩu xác nhận không khớp.')).toBeVisible();expect(calls).toHaveLength(0);
 await page.getByLabel('Xác nhận mật khẩu',{exact:true}).fill('new password');await page.getByRole('button',{name:'Lưu mật khẩu',exact:true}).evaluate((b:HTMLButtonElement)=>{b.click();b.click();});
 await expect.poll(()=>calls.length).toBe(1);expect(calls[0].body).toEqual({currentPassword:'old password',newPassword:'new password'});expect(calls[0].headers.authorization).toBe('Bearer access-test');expect(calls[0].headers['x-xsrf-token']).toBe('csrf-test');
 if(status===204){await expect(page.getByText('Đổi mật khẩu thành công. Các phiên đăng nhập khác đã được đăng xuất.')).toBeVisible();await expect(page.getByLabel('Mật khẩu hiện tại',{exact:true})).toHaveValue('');}
 else if(status===401)await expect(page.getByRole('button',{name:'Đăng nhập',exact:true})).toBeVisible();
 else await expect(page.getByRole('button',{name:'Lưu mật khẩu',exact:true})).toBeEnabled();
 await expect(page.getByText('PRIVATE')).toHaveCount(0);expect(calls).toHaveLength(1);expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});
