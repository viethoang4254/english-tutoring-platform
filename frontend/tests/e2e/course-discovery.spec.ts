import { expect, test } from '@playwright/test';
import type { PublicCourse, PublicCourseDetail } from '../../src/features/course/public-courses';

const first: PublicCourse = {
 id:'9007199254740993',name:'Tiếng Anh hằng ngày',description:'Thực hành giao tiếp hằng ngày.',
 tuition:{amount:'1800000.00',currency:'VND'},category:{id:'11',name:'Giao tiếp'},
 teacher:{id:'21',fullName:'Gia sư An'},plannedStartDate:'2030-01-08',timezone:'Asia/Ho_Chi_Minh',sessionCount:12,
};
const second: PublicCourse = {...first,id:'9007199254740994',name:'Tiếng Anh trong công việc',description:'Giao tiếp với đồng nghiệp.',category:{id:'12',name:'Công việc'},teacher:{id:'22',fullName:'Gia sư Bình'},tuition:{amount:'2000000.50',currency:'VND'},timezone:'Asia/Ho_Chi_Minh'};
const detail: PublicCourseDetail = {course:second,outline:{introduction:"Luyện giao tiếp trong các cuộc họp trực tuyến, từ lời chào đầu tiên đến lúc thống nhất kế hoạch với đồng nghiệp.",objectives:"Trình bày ý kiến và đặt câu hỏi làm rõ trong cuộc họp.",audience:"Người đi làm thường xuyên trao đổi với đồng nghiệp quốc tế.",requirements:"Có thể giới thiệu bản thân và hiểu câu hỏi đơn giản.",outcomes:"Tóm tắt quyết định và xác nhận các bước tiếp theo bằng tiếng Anh.",stages:[{title:"Cuộc họp",topics:[{title:"Đặt lịch",description:"Thống nhất thời gian."},{title:"Trao đổi ý kiến",description:"Đề xuất phương án và phản hồi ý kiến của đồng nghiệp."}]},{title:"Trình bày kế hoạch",topics:[{title:"Giới thiệu đề xuất",description:"Sắp xếp ý chính thành bài trình bày ngắn."}]}]},rules:[{id:'31',dayOfWeek:2,startTime:'09:00',endTime:'10:30'},{id:'32',dayOfWeek:7,startTime:'14:00',endTime:'15:00'}]};
const coursePage = (items: PublicCourse[],page=0,hasNext=false) => ({items,page,size:20,hasNext});

test('catalog search, category filters, details and responsive public data are safe',async({page},info)=>{
 const forbidden: string[]=[];
 await page.route('**/api/v1/**',route=>{
  const request=route.request();const path=new URL(request.url()).pathname;
  expect(request.method()).toBe('GET');expect(request.headers()['authorization']).toBeUndefined();
  if(path === '/api/v1/public/courses')return route.fulfill({json:coursePage([first,second])});
  if(path === `/api/v1/public/courses/${second.id}`)return route.fulfill({json:detail});
  forbidden.push(path);return route.abort();
 });
 await page.goto('/courses');
 await expect(page.getByRole('banner')).toBeVisible();
 await expect(page.getByRole('heading',{name:/Tìm khóa học/})).toBeVisible();
 await expect(page.getByRole('status')).toHaveText('2 khóa học trên trang 1');
 await expect(page.getByText('Gia sư An',{exact:true})).toBeVisible();
 await expect(page.getByText(`Mã khóa học: ${first.id}`,{exact:true})).toBeVisible();
 await expect(page.getByText('1.800.000 ₫',{exact:true})).toBeVisible();
 await page.getByLabel('Tìm khóa học',{exact:true}).fill('hang ngay');
 await expect(page.getByRole('status')).toHaveText('1 khóa học trên trang 1');
 await page.getByRole('combobox',{name:'Danh mục',exact:true}).selectOption('12');
 await expect(page.getByRole('heading',{name:'Không tìm thấy khóa học phù hợp'})).toBeVisible();
 await page.getByRole('button',{name:'Đặt lại tìm kiếm'}).click();
 await page.getByRole('combobox',{name:'Danh mục',exact:true}).selectOption('12');
 await expect(page.getByRole('status')).toHaveText('1 khóa học trên trang 1');
 await page.getByRole('button',{name:'Xóa bộ lọc'}).click();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.screenshot({path:`test-results/courses-${info.project.name}.png`,fullPage:true});
 await page.getByRole('link',{name:`Xem chi tiết ${second.name}`,exact:true}).click();
 await expect(page).toHaveURL(new RegExp(`/courses/${second.id}$`));
 await page.reload();
 await expect(page.getByRole('heading',{name:second.name,exact:true})).toBeVisible();
 await expect(page.getByText('2.000.001 ₫',{exact:true})).toBeVisible();
 await expect(page.getByText(`Mã khóa học: ${second.id}`,{exact:true})).toBeVisible();
 await expect(page.getByText('Thứ 3 · 09:00–10:30',{exact:true})).toBeVisible();
 await expect(page.getByText('Chủ nhật · 14:00–15:00',{exact:true})).toBeVisible();
 await expect(page.getByText('12 buổi học · Múi giờ: Asia/Ho_Chi_Minh',{exact:true})).toBeVisible();
 await expect(page.getByText('Dự kiến bắt đầu: 08/01/2030',{exact:true})).toBeVisible();
 await expect(page.getByRole('button',{name:'Đăng nhập để đăng ký',exact:true})).toBeVisible();
 await expect(page.locator('a[href*="meet.google"]')).toHaveCount(0);
 await expect(page.getByText(/Dữ liệu mẫu|Dữ liệu minh họa/)).toHaveCount(0);
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.getByRole('link',{name:'Lộ trình',exact:true}).click();
 await expect(page.getByText('Thống nhất thời gian.',{exact:true})).toBeHidden();
 await page.getByText('1. Cuộc họp',{exact:true}).click();
 await expect(page.getByText('Thống nhất thời gian.',{exact:true})).toBeVisible();
 await page.screenshot({path:`test-results/course-detail-${info.project.name}.png`,fullPage:true});
 await page.getByRole('link',{name:'← Khám phá khóa học khác'}).click();
 await expect(page).toHaveURL(/\/courses$/);expect(forbidden).toEqual([]);
 await page.goto('/courses/not-a-course');
 await expect(page.getByRole('heading',{name:/Không tìm thấy/})).toBeVisible();
});

test('Student dashboard discovers courses from sidebar and empty enrollments without changing calendar',async({page})=>{
 await page.route('**/api/v1/**',route=>{
  const path=new URL(route.request().url()).pathname;
  if(path === '/api/v1/public/courses')return route.fulfill({json:coursePage([first,second])});
  if(path.endsWith('/csrf'))return route.fulfill({json:{token:'test-csrf'}});
  if(path === `/api/v1/public/courses/${second.id}`)return route.fulfill({json:detail});
  if(path.endsWith('/login')||path.endsWith('/refresh'))return route.fulfill({json:{accessToken:'test-access',tokenType:'Bearer'}});
  if(path.endsWith('/users/me'))return route.fulfill({json:{id:'1',role:'STUDENT',emailVerified:false}});
  if(path.endsWith('/profile'))return route.fulfill({json:{fullName:'Học viên kiểm thử',email:'student@example.test'}});
  return route.abort();
 });
 await page.goto('/login');await page.getByLabel('Email',{exact:true}).fill('student@example.test');await page.getByLabel('Mật khẩu',{exact:true}).fill('password');await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
 await expect(page).toHaveURL(/\/student\/dashboard$/);
 const toggle=page.locator('button[aria-controls="student-navigation"]');
 await expect(toggle).toBeAttached();
 if(await toggle.isVisible() && await toggle.getAttribute('aria-expanded')==='false')await toggle.click();
 await page.getByRole('navigation',{name:'Điều hướng học viên'}).getByRole('link',{name:'Khám phá khóa học',exact:true}).click();
 await expect(page).toHaveURL(/\/student\/dashboard\/courses$/);
 await expect(page.getByRole('banner')).toHaveCount(0);
 await page.getByRole('combobox',{name:'Danh mục',exact:true}).selectOption('12');
 await expect(page.getByText('1 khóa học trên trang 1')).toBeVisible();
 await page.getByRole('link',{name:`Xem chi tiết ${second.name}`}).click();
 await expect(page).toHaveURL(new RegExp(`/student/dashboard/courses/${second.id}$`));
 await page.reload();
 await expect(page.getByRole('button',{name:'Đăng ký giữ chỗ'})).toBeVisible();
 await expect(page.getByText('Thứ 3 · 09:00–10:30',{exact:true})).toBeVisible();
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
 await page.goBack();
 await expect(page.getByRole('heading',{name:'Khám phá khóa học',exact:true})).toBeVisible();
 await expect(page.getByRole('combobox',{name:'Danh mục',exact:true})).toHaveValue('12');
 await page.goForward();
 await expect(page.getByRole('heading',{name:second.name,exact:true})).toBeVisible();
 if(await toggle.isVisible() && await toggle.getAttribute('aria-expanded')==='false')await toggle.click();
 await page.locator('#student-navigation').getByRole('link',{name:'Lịch học của tôi'}).click();
 await page.getByLabel('Chế độ xem trước').selectOption('empty');
 await expect(page.getByText('Không có buổi học trong ngày này')).toBeVisible();
 await expect(page.getByRole('heading',{name:'Chưa có khóa học trong bản xem trước'})).toBeVisible();
 await page.locator('#khoa-hoc-da-dang-ky').getByRole('link',{name:'Khám phá khóa học',exact:true}).click();
 await expect(page).toHaveURL(/\/student\/dashboard\/courses$/);
});
test('public list paginates forward and back using backend hasNext',async({page})=>{
 const pages: string[]=[];
 await page.route('**/api/v1/public/courses?*',route=>{
  const url=new URL(route.request().url());const current=url.searchParams.get('page')!;pages.push(current);
  expect(url.searchParams.get('size')).toBe('20');
  return route.fulfill({json:current==='0'?coursePage([first],0,true):coursePage([second],1,false)});
 });
 await page.goto('/courses');
 await expect(page.getByRole('heading',{name:first.name,exact:true})).toBeVisible();
 await expect(page.getByRole('button',{name:'Trang trước',exact:true})).toBeDisabled();
 await page.getByRole('button',{name:'Trang sau',exact:true}).click();
 await expect(page.getByRole('heading',{name:second.name,exact:true})).toBeVisible();
 await expect(page.getByRole('heading',{name:first.name,exact:true})).toHaveCount(0);
 await expect(page.getByRole('button',{name:'Trang sau',exact:true})).toBeDisabled();
 await page.getByRole('button',{name:'Trang trước',exact:true}).click();
 await expect(page.getByRole('heading',{name:first.name,exact:true})).toBeVisible();
 expect(pages).toEqual(['0','1','0']);
});
test('empty public list never substitutes sample Courses',async({page})=>{
 await page.route('**/api/v1/public/courses?*',route=>route.fulfill({json:coursePage([])}));
 await page.goto('/courses');
 await expect(page.getByRole('heading',{name:'Chưa có khóa học trên trang này'})).toBeVisible();
 await expect(page.locator('main article')).toHaveCount(0);
 await expect(page.getByRole('button',{name:'Trang sau',exact:true})).toBeDisabled();
});
test('list failure is safe and retries only on explicit action',async({page})=>{
 let calls=0;
 await page.route('**/api/v1/public/courses?*',route=>++calls===1?route.fulfill({status:503,json:{code:'COURSE_UNAVAILABLE',message:'private database diagnostic'}}):route.fulfill({json:coursePage([first])}));
 await page.goto('/courses');
 await expect(page.getByRole('main').getByRole('alert')).toHaveText('Chưa thể tải khóa học. Vui lòng thử lại.');
 await expect(page.getByText('private database diagnostic')).toHaveCount(0);
 await expect(page.locator('main article')).toHaveCount(0);expect(calls).toBe(1);
 await page.getByRole('button',{name:'Thử lại',exact:true}).click();
 await expect(page.getByRole('heading',{name:first.name,exact:true})).toBeVisible();expect(calls).toBe(2);
});
test('unknown and non-public Course IDs show the same safe missing state',async({page})=>{
 await page.route('**/api/v1/public/courses/*',route=>route.fulfill({status:404,json:{code:'COURSE_NOT_FOUND',message:'Course request could not be completed.'}}));
 for(const id of ['99','100']) {
  await page.goto(`/courses/${id}`);
  await expect(page.getByRole('heading',{name:'Không tìm thấy khóa học',exact:true})).toBeVisible();
  await expect(page.getByRole('main').getByRole('alert')).toHaveText('Khóa học không tồn tại hoặc chưa được công khai.');
  await expect(page.getByRole('button',{name:'Đăng nhập để đăng ký'})).toHaveCount(0);
 }
});
test('detail retries transient failures and handles missing weekly rules',async({page})=>{
 let calls=0;
 await page.route('**/api/v1/public/courses/*',route=>++calls===1?route.abort():route.fulfill({json:{course:first,rules:[]}}));
 await page.goto(`/courses/${first.id}`);
 await expect(page.getByRole('heading',{name:'Chưa thể tải khóa học',exact:true})).toBeVisible();expect(calls).toBe(1);
 await page.getByRole('button',{name:'Thử lại',exact:true}).click();
 await expect(page.getByRole('heading',{name:first.name,exact:true})).toBeVisible();
 await expect(page.getByText('Chưa có lịch học hằng tuần.',{exact:true})).toBeVisible();expect(calls).toBe(2);
});
test('loading state remains visible until public data arrives',async({page})=>{
 let release!: ()=>void;const gate=new Promise<void>(resolve=>{release=resolve;});
 await page.route('**/api/v1/public/courses?*',async route=>{await gate;await route.fulfill({json:coursePage([first])});});
 await page.goto('/courses');
 try { await expect(page.getByRole('status')).toHaveText('Đang tải khóa học…'); }
 finally { release(); }
 await expect(page.getByRole('heading',{name:first.name,exact:true})).toBeVisible();
});
