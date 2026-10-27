import { expect, test } from '@playwright/test';

test('catalog search, category filters, details and responsive preview are safe',async({page},info)=>{
 let calls=0;await page.route('**/api/v1/**',route=>{calls++;return route.abort();});
 await page.goto('/courses');
 await expect(page.getByRole('heading',{name:/Tìm khóa học/})).toBeVisible();
 await expect(page.getByText('Bản xem trước · Dữ liệu minh họa')).toBeVisible();
 await expect(page.getByRole('status')).toHaveText('2 khóa học minh họa phù hợp');
 await page.getByLabel('Tìm khóa học').fill('hang ngay');
 await expect(page.getByRole('status')).toHaveText('1 khóa học minh họa phù hợp');
 await page.getByLabel('Danh mục').selectOption('work');
 await expect(page.getByRole('heading',{name:'Không tìm thấy khóa học phù hợp'})).toBeVisible();
 await page.getByRole('button',{name:'Đặt lại tìm kiếm'}).click();
 await page.getByLabel('Danh mục').selectOption('work');
 await expect(page.getByRole('status')).toHaveText('1 khóa học minh họa phù hợp');
 await page.getByRole('button',{name:'Xóa bộ lọc'}).click();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.screenshot({path:`test-results/courses-${info.project.name}.png`,fullPage:true});
 await page.getByRole('link',{name:'Xem chi tiết Tiếng Anh trong công việc',exact:true}).click();
 await expect(page).toHaveURL(/\/courses\/demo-work$/);
 await page.reload();
 await expect(page.getByRole('heading',{name:'Tiếng Anh trong công việc',exact:true})).toBeVisible();
 await expect(page.getByText('Chi tiết khóa học minh họa')).toBeVisible();
 await expect(page.getByRole('button',{name:'Đăng ký khóa học — chưa khả dụng'})).toBeDisabled();
 await expect(page.getByText('Chưa có thông tin công khai')).toBeVisible();
 await expect(page.locator('a[href*="meet.google"]')).toHaveCount(0);
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.screenshot({path:`test-results/course-detail-${info.project.name}.png`,fullPage:true});
 await page.getByRole('link',{name:'← Khám phá khóa học khác'}).click();
 await expect(page).toHaveURL(/\/courses$/);expect(calls).toBe(0);
 await page.goto('/courses/not-a-course');
 await expect(page.getByRole('heading',{name:/Không tìm thấy/})).toBeVisible();
});

test('Student dashboard discovers courses from sidebar and empty enrollments without changing calendar',async({page},info)=>{
 await page.route('**/api/v1/**',route=>{
  const path=new URL(route.request().url()).pathname;
  if(path.endsWith('/csrf'))return route.fulfill({json:{token:'test-csrf'}});
  if(path.endsWith('/login'))return route.fulfill({json:{accessToken:'test-access',tokenType:'Bearer'}});
  if(path.endsWith('/users/me'))return route.fulfill({json:{id:'1',role:'STUDENT',emailVerified:false}});
  if(path.endsWith('/profile'))return route.fulfill({json:{fullName:'Học viên kiểm thử',email:'student@example.test'}});
  return route.abort();
 });
 await page.goto('/login');await page.getByLabel('Email',{exact:true}).fill('student@example.test');await page.getByLabel('Mật khẩu',{exact:true}).fill('password');await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
 await expect(page).toHaveURL(/\/student\/dashboard$/);
 if(info.project.name==='narrow')await page.getByRole('button',{name:'Điều hướng học tập'}).click();
 await page.getByRole('navigation',{name:'Điều hướng học viên'}).getByRole('link',{name:'Khám phá khóa học',exact:true}).click();
 await expect(page).toHaveURL(/\/courses$/);
 await page.locator('.header-account-menu summary').click();await page.getByRole('link',{name:'Bảng học tập',exact:true}).click();
 await page.getByLabel('Chế độ xem trước').selectOption('empty');
 await expect(page.getByText('Không có buổi học trong ngày này')).toBeVisible();
 await expect(page.getByRole('heading',{name:'Chưa có khóa học trong bản xem trước'})).toBeVisible();
 await page.locator('#khoa-hoc-da-dang-ky').getByRole('link',{name:'Khám phá khóa học',exact:true}).click();
 await expect(page).toHaveURL(/\/courses$/);
});