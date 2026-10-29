import { expect, test, type Page } from '@playwright/test';
import { monthDays, shiftMonth, dayKey } from '../../src/features/student/dashboard-preview';

async function signIn(page: Page, role = 'STUDENT', profileStatus = 200) {
  await page.clock.setFixedTime(new Date('2026-10-18T03:00:00Z'));
  const calls: string[] = [];
  await page.route('**/api/v1/**', async route => {
    const request = route.request(); const path = new URL(request.url()).pathname;
    calls.push(path);
    if (path.endsWith('/csrf')) return route.fulfill({json:{token:'test-csrf'}});
    if (path.endsWith('/login')) return route.fulfill({json:{accessToken:'test-access',tokenType:'Bearer'}});
    if (path.endsWith('/users/me')) return route.fulfill({json:{id:'1',role,emailVerified:true}});
    if (path.endsWith('/profile')) {
      expect(request.headers().authorization).toBe('Bearer test-access');
      return route.fulfill({status:profileStatus,json:profileStatus === 200 ? {fullName:'Nguyễn Minh Anh',email:'student@example.test'} : {code:'UNAVAILABLE',message:'PRIVATE'}});
    }
    return route.abort();
  });
  await page.goto('/student/dashboard');
  await expect(page).toHaveURL(/\/login\?next=/);
  const dialog = page.locator('main');
  await dialog.getByLabel('Email',{exact:true}).fill('student@example.test');
  await dialog.getByLabel('Mật khẩu',{exact:true}).fill('password');
  const rejectedProfile = profileStatus === 401 ? page.waitForResponse(response => response.url().endsWith('/profile')) : null;
  await dialog.getByRole('button',{name:'Đăng nhập',exact:true}).click();
  if (rejectedProfile) await rejectedProfile;
  if (profileStatus !== 401) await expect(page.locator('main form input[name="email"]')).toHaveCount(0);
  return calls;
}

test('calendar arithmetic handles leap years, year boundaries and Vietnam dates', () => {
  expect(shiftMonth('2026-12',1)).toBe('2027-01');
  expect(shiftMonth('2026-01',-1)).toBe('2025-12');
  expect(monthDays('2028-02')).toContain('2028-02-29');
  expect(monthDays('2026-02')).not.toContain('2026-02-29');
  expect(new Date(monthDays('2026-10')[0]+'T12:00:00Z').getUTCDay()).toBe(1);
  expect(dayKey('2026-10-17T18:00:00Z')).toBe('2026-10-18');
});

test('calendar selection, navigation and preview states are responsive', async ({page}, info) => {
  await signIn(page);
  await expect(page.getByRole('heading',{name:/Xin chào, Nguyễn Minh Anh/})).toBeVisible();
  await expect(page.getByText('Bản xem trước · Dữ liệu minh họa')).toBeVisible();
  const calendar = page.locator('#lich-hoc');
  await expect(calendar.getByRole('heading',{name:'Giới thiệu bản thân',exact:true})).toBeVisible();
  await expect(calendar.getByRole('button',{name:'Vào lớp'})).toBeDisabled();
  await calendar.getByRole('button',{name:/20 tháng 10.*2 buổi học/}).click();
  await expect(calendar.getByRole('heading',{name:'Trao đổi trong cuộc họp'})).toBeVisible();
  await expect(calendar.getByRole('heading',{name:'Hội thoại theo tình huống'})).toBeVisible();
  await calendar.getByRole('button',{name:/23 tháng 10.*1 buổi học/}).click();
  await expect(calendar.getByText('Đã hủy · minh họa')).toBeVisible();
  await page.getByRole('button',{name:'Tháng sau',exact:true}).click();
  await expect(calendar.getByRole('heading',{name:'tháng 11 năm 2026',exact:true})).toBeVisible();
  await expect(page.getByText('Không có buổi học trong ngày này')).toBeVisible();
  await page.getByRole('button',{name:'Tháng trước',exact:true}).click();
  await page.getByRole('button',{name:'Hôm nay',exact:true}).click();
  await expect(calendar.getByRole('button',{pressed:true})).toHaveAttribute('aria-current','date');
  expect(await page.evaluate(()=>document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({path:`test-results/dashboard-${info.project.name}.png`,fullPage:true});
  await page.getByLabel('Chế độ xem trước').selectOption('empty');
  await expect(page.getByText('Chưa có buổi học sắp tới.')).toBeVisible();
  await expect(page.getByRole('heading',{name:'Chưa có khóa học trong bản xem trước'})).toBeVisible();
  const toggle = page.locator('button[aria-controls="student-navigation"]');
  await expect(toggle).toBeAttached();
  if (await toggle.isVisible() && await toggle.getAttribute('aria-expanded') === 'false') await toggle.click();
  await page.getByRole('navigation',{name:'Điều hướng học viên'}).getByRole('link',{name:'Bài tập',exact:true}).click();
  await expect(page.locator('#bai-tap')).toBeInViewport();
  await page.getByRole('link',{name:'Hồ sơ của tôi'}).click();
  await expect(page).toHaveURL(/\/account$/);
  await page.locator('.header-account-menu summary').click();
  await page.getByRole('link',{name:'Bảng học tập',exact:true}).click();
  await expect(page.getByRole('heading',{name:/Xin chào, Nguyễn Minh Anh/})).toBeVisible();
});

for (const role of ['TEACHER','ADMIN']) test(`${role} preserves its existing account workspace rather than entering the Student dashboard`, async ({page}) => {
  await signIn(page,role);
  await expect(page).toHaveURL(role === 'TEACHER' ? /\/teacher$/ : /\/admin\/dashboard$/);
  await expect(page.getByRole('heading',{name:/Xin chào,/})).toHaveCount(0);
  await page.evaluate(() => (window as unknown as { next: { router: { push: (path: string) => void } } }).next.router.push('/student/dashboard'));
  await expect(page.getByRole('heading',{name:'Không gian dành cho học viên',exact:true})).toBeVisible();
  await expect(page.locator('#lich-hoc')).toHaveCount(0);
});
for (const status of [401,403,503]) test(`profile ${status} fails safely without automatic retry`,async ({page})=>{
  const calls = await signIn(page,'STUDENT',status);
  await expect(page.getByRole('heading',{name:status===401?'Đăng nhập':'Chưa thể mở bảng học tập'})).toBeVisible();
  expect(calls.filter(path=>path.endsWith('/profile'))).toHaveLength(1);
  await expect(page.getByText('PRIVATE')).toHaveCount(0);
  if(status===503){await page.getByRole('button',{name:'Thử lại',exact:true}).click();await expect.poll(()=>calls.filter(path=>path.endsWith('/profile')).length).toBe(2);}
  else await expect(page.getByRole('button',{name:'Thử lại',exact:true})).toHaveCount(0);
});
test('direct navigation and refresh do not silently restore authentication',async({page})=>{
  let requests=0;await page.route('**/api/v1/**',route=>{requests++;return route.abort();});
  await page.goto('/student/dashboard');await page.reload();
  await expect(page.getByRole('heading',{name:'Đăng nhập',exact:true})).toBeVisible();expect(requests).toBe(0);
});