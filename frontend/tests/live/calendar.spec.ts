import {expect, test} from '@playwright/test';
import {api, fixture, mutation, register} from '../support/live-fixtures';
import {teacherPanel} from '../support/teacher-navigation';

type Item = {sessionId:string;courseId:string;courseTitle:string;sessionNumber:number;sessionTitle:string|null;
  startsAt:string;endsAt:string;courseTimezone:string;status:string};

test('live populated Calendar ownership, boundaries and Dashboard', async ({page, context, playwright}, info) => {
  test.setTimeout(180_000);
  const stage = (description:string) => info.annotations.push({type:'task009-stage',description});
  const clients = await Promise.all([0,1,2,3].map(() => playwright.request.newContext()));
  try {
    stage('registration');
    const admin = await register(clients[0], false);
    fixture('fixture', {action:'admin',id:admin.id});
    const login = await mutation(clients[0], '/auth/login', {email:admin.email,password:admin.password});
    expect(login.status()).toBe(200);
    const adminHeaders = {Authorization:'Bearer '+(await login.json()).accessToken};
    const owner = await register(clients[1], true);
    const other = await register(clients[2], true);
    const rejected = await register(clients[3], true);
    stage('onboarding-review');
    for (const [index, account, decision] of [[1,owner,'approve'],[2,other,'approve'],[3,rejected,'reject']] as const) {
      const onboarding = await clients[index].get(api+'/teacher/onboarding',{headers:account.headers});
      expect(onboarding.status()).toBe(200);
      const result = await clients[0].post(`${api}/admin/teacher-applications/${(await onboarding.json()).applicationId}/${decision}`,
        {headers:adminHeaders,...(decision==='reject'?{data:{reason:'CI rejection'}}:{})});
      expect(result.status()).toBe(204);
    }
    stage('calendar-fixtures');
    const seeded = JSON.parse(fixture('calendar-fixture',{owner:owner.id,other:other.id}));
    expect(seeded.sessionCount).toBe(8);
    const midnight = Date.parse(seeded.day+'T00:00:00Z');
    const iso = (hours:number) => new Date(midnight+hours*3600000).toISOString();
    const query = (from=iso(-24),to=iso(72),page=0,size=100) => '/teacher/calendar/sessions?'+new URLSearchParams({from,to,page:String(page),size:String(size)});
    const get = async (path:string, account=owner, client=clients[1]) => {
      const response = await client.get(api+path,{headers:account.headers});
      expect(response.status()).toBe(200);
      expect(response.headers()['cache-control']).toBe('no-store');
      return response.json();
    };
    stage('calendar-api');
    const all:Item[] = (await get(query())).items;
    expect(all).toHaveLength(6);
    expect(all.every(s=>s.courseId===seeded.ownerCourse)).toBe(true);
    expect(all.filter(s=>s.status==='CANCELLED')).toHaveLength(2);
    expect(all.every(s=>s.courseTimezone==='Asia/Ho_Chi_Minh')).toBe(true);
    const sorted = [...all].sort((a,b)=>Date.parse(a.startsAt)-Date.parse(b.startsAt)||(BigInt(a.sessionId)<BigInt(b.sessionId)?-1:1));
    expect(all.map(s=>s.sessionId)).toEqual(sorted.map(s=>s.sessionId));
    expect((await get(query())).items).toEqual(all);
    for(const item of all) expect(Object.keys(item).sort()).toEqual(['sessionId','courseId','courseTitle','sessionNumber','sessionTitle','startsAt','endsAt','courseTimezone','status'].sort());
    const otherItems:Item[] = (await get(query(),other,clients[2])).items;
    expect(otherItems).toHaveLength(2);
    expect(otherItems.every(s=>s.courseId===seeded.otherCourse&&s.courseTimezone==='America/New_York')).toBe(true);
    expect(otherItems.some(s=>all.some(a=>a.sessionId===s.sessionId))).toBe(false);
    expect((await clients[3].get(api+query(),{headers:rejected.headers})).status()).toBe(403);
    expect((await clients[1].get(api+`/teacher/courses/${seeded.otherCourse}/sessions`,{headers:owner.headers})).status()).toBe(404);
    const paged:Item[]=[];
    for(let n=0;n<3;n++) {const p=await get(query(iso(-24),iso(72),n,2));expect(p.page).toBe(n);expect(p.size).toBe(2);expect(p.hasNext).toBe(n<2);paged.push(...p.items);}
    expect(paged).toEqual(all);
    expect((await get(query(iso(-24),iso(72),3,2))).items).toEqual([]);
    expect((await get(query(iso(13),iso(14)))).items).toEqual([]);
    expect((await get(query(iso(12.5),iso(12.75)))).items).toHaveLength(2);
    const crossing:Item[]=(await get(query(iso(24),iso(25)))).items;
    expect(crossing.map(s=>s.sessionNumber)).toEqual([5]);
    const offsetFrom=seeded.day+'T19:30:00+07:00',offsetTo=seeded.day+'T19:45:00+07:00';
    expect((await get(query(offsetFrom,offsetTo))).items).toEqual((await get(query(iso(12.5),iso(12.75)))).items);

    stage('calendar-browser');
    // Anchor display date to fixture day, including CI runs near a month boundary.
    await page.clock.setFixedTime(new Date(iso(12)));
    const calendarResponse = page.waitForResponse(response =>
      response.url().startsWith(api+'/teacher/calendar/sessions?') && response.request().method()==='GET');
    await page.goto('/login');
    await page.getByLabel('Email',{exact:true}).fill(owner.email);
    await page.getByLabel('Mật khẩu',{exact:true}).fill(owner.password);
    await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
    await expect(page).toHaveURL('http://127.0.0.1:3000/teacher');
    await teacherPanel(page,'Lịch dạy');
    await expect(page).toHaveURL('http://127.0.0.1:3000/teacher/calendar');
    stage('calendar-browser-response');
    const response = await calendarResponse;
    const status = response.status();
    info.annotations.push({type:'task009-stage',description:status===200?'calendar-http-200':
      [400,401,403,404,500,503].includes(status)?`calendar-http-${status}`:'calendar-http-other'});
    expect(status).toBe(200);
    const browserItems:Item[] = (await response.json()).items;
    expect(browserItems.length).toBeGreaterThan(0);
    expect(browserItems.every(item=>item.courseId===seeded.ownerCourse)).toBe(true);
    stage('calendar-browser-render');
    const calendar=page.getByRole('region',{name:'Lịch dạy của tôi'});
    await expect(calendar.getByLabel('Lịch tháng',{exact:true})).toBeVisible();
    await expect(calendar.getByText('CI Calendar A',{exact:true}).first()).toBeVisible();
    await expect(calendar.getByText('CI Calendar B',{exact:true})).toHaveCount(0);
    await expect(calendar.getByText('Asia/Ho_Chi_Minh',{exact:true}).first()).toBeVisible();
    const cancelled=calendar.locator('article').filter({hasText:'Buổi 2: CI Topic 2'});
    await expect(cancelled.getByText('Đã hủy',{exact:true})).toBeVisible();
    expect(await cancelled.getAttribute('class')).toContain('cancelled');
    const expectedTime=new Intl.DateTimeFormat('vi-VN',{timeZone:'Asia/Ho_Chi_Minh',dateStyle:'short',timeStyle:'short'}).format(new Date(iso(12)));
    await expect(calendar.locator('article').filter({hasText:'Buổi 1: CI Topic 1'})).toContainText(expectedTime);
    await calendar.getByRole('button',{name:'Tuần',exact:true}).click();
    await expect(calendar.getByLabel('Lịch tuần',{exact:true})).toBeVisible();
    await expect(calendar.getByText('CI Calendar A',{exact:true}).first()).toBeVisible();
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    const nav=page.locator('#dashboard-navigation');await nav.evaluate(e=>e.dataset.calendarTest='mounted');
    stage('calendar-navigation');
    await calendar.getByRole('link',{name:'Xem và quản lý buổi học'}).first().click();
    await expect(page).toHaveURL(`http://127.0.0.1:3000/teacher/courses/${seeded.ownerCourse}?section=sessions`);
    const sessions=page.getByRole('region',{name:'Lịch và buổi học',exact:true});
    await expect(sessions).toBeVisible();
    await sessions.getByRole('button',{name:'Tải lại lịch và buổi học',exact:true}).click();
    await expect(page.getByText('Buổi 1: CI Topic 1',{exact:true}).first()).toBeVisible();
    await page.getByRole('button',{name:'Xem buổi 1',exact:true}).click();
    await expect(page.getByRole('heading',{name:'Chi tiết buổi 1',exact:true})).toBeVisible();
    await expect(sessions.getByRole('paragraph').filter({hasText:/^CI plain text$/})).toBeVisible();
    expect(await nav.getAttribute('data-calendar-test')).toBe('mounted');
    await teacherPanel(page,'Lịch dạy');await expect(calendar).toBeVisible();
    stage('calendar-other-timezone');
    expect((await mutation(context.request,'/auth/logout')).status()).toBe(204);
    await page.goto('/login');
    await page.getByLabel('Email',{exact:true}).fill(other.email);
    await page.getByLabel('Mật khẩu',{exact:true}).fill(other.password);
    await page.getByRole('button',{name:'Đăng nhập',exact:true}).click();
    await expect(page).toHaveURL('http://127.0.0.1:3000/teacher');
    await teacherPanel(page,'Lịch dạy');
    await expect(page).toHaveURL('http://127.0.0.1:3000/teacher/calendar');
    await expect(calendar.getByText('CI Calendar B',{exact:true}).first()).toBeVisible();
    await expect(calendar.getByText('CI Calendar A',{exact:true})).toHaveCount(0);
    await expect(calendar.getByText('America/New_York',{exact:true}).first()).toBeVisible();
    const otherTime=new Intl.DateTimeFormat('vi-VN',{timeZone:'America/New_York',dateStyle:'short',timeStyle:'short'}).format(new Date(iso(12)));
    await expect(calendar.locator('article').filter({hasText:'Buổi 1: CI Topic 1'})).toContainText(otherTime);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  } finally {
    for(const client of [...clients,context.request]) {
      try {expect((await mutation(client,'/auth/logout')).status()).toBe(204);} finally {if(client!==context.request)await client.dispose();}
    }
  }
});
