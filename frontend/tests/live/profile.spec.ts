import { accountPanel } from "../support/account-navigation";
import {expect, test} from '@playwright/test';
import {api, fixture, mutation, register} from '../support/live-fixtures';

for (const state of ['STUDENT', 'PENDING', 'REJECTED', 'APPROVED']) {
  test(`live own profile ${state}`, async ({page, context, playwright}, testInfo) => {
    const stage = (description: string) => { testInfo.annotations.push({type: 'task009-stage', description}); };
    stage('registration');
    const teacher = state !== 'STUDENT';
    const account = await register(context.request, teacher);
    stage('onboarding-review');
    if (state === 'REJECTED' || state === 'APPROVED') {
      const adminRequest = await playwright.request.newContext();
      try {
        const admin = await register(adminRequest, false);
        fixture('fixture', {action: 'admin', id: admin.id});
        const login = await mutation(adminRequest, '/auth/login', {email: admin.email, password: admin.password});
        expect(login.status()).toBe(200);
        const adminHeaders = {Authorization: 'Bearer ' + (await login.json()).accessToken};
        expect((await adminRequest.get(api + '/users/me/profile', {headers: adminHeaders})).status()).toBe(403);
        const onboarding = await context.request.get(api + '/teacher/onboarding', {headers: account.headers});
        expect(onboarding.status()).toBe(200);
        const id = (await onboarding.json()).applicationId;
        const result = await adminRequest.post(`${api}/admin/teacher-applications/${id}/${state === 'APPROVED' ? 'approve' : 'reject'}`,
          {headers: adminHeaders, ...(state === 'REJECTED' ? {data: {reason: 'Cần bổ sung thông tin'}} : {})});
        expect(result.status()).toBe(204);
      } finally { await adminRequest.dispose(); }
    }
    stage('snapshot-before');
    const before = JSON.parse(fixture('fixture', {action: 'snapshot', id: account.id}));
    if (!teacher) expect(before.verificationCount).toBe(0);
    stage('browser-login');
    await page.goto('/account');
    await page.getByLabel('Email', {exact: true}).fill(account.email);
    await page.getByLabel('Mật khẩu', {exact: true}).fill(account.password);
    await page.getByRole('button', {name: 'Đăng nhập', exact: true}).click();
    if (!teacher) {
      await expect(page).toHaveURL(/\/student\/dashboard$/);
      await page.locator('.header-account-menu summary').click();
      await page.getByRole('link', {name: 'Tài khoản của tôi', exact: true}).click();
    }
    await accountPanel(page, 'Hồ sơ cá nhân');
    await expect(page.getByRole('button', {name: 'Chỉnh sửa thông tin'})).toBeVisible();
    stage('refresh-cookie-before');
    const cookiesBefore = (await context.cookies()).filter(c => c.name === 'refresh_token');
    expect(cookiesBefore.length).toBe(1);
    expect(cookiesBefore[0].httpOnly).toBe(true);
    expect(cookiesBefore[0].sameSite).toBe('Lax');
    expect(cookiesBefore[0].path).toBe('/api/v1/auth');
    expect(cookiesBefore[0].secure).toBe(false); // Approved loopback HTTP CI transport.
    stage('profile-edit');
    let writes = 0;
    page.on('request', request => {
      if (request.method() === 'PATCH' && request.url().endsWith('/users/me/profile')) writes++;
    });
    await page.getByRole('button', {name: 'Chỉnh sửa thông tin'}).click();
    await page.getByLabel('Họ và tên', {exact: true}).fill('Discard');
    await page.getByRole('button', {name: 'Hủy', exact: true}).click();
    expect(writes).toBe(0);
    await page.getByRole('button', {name: 'Chỉnh sửa thông tin'}).click();
    const positions = await page.locator('.student-profile-grid label').evaluateAll(labels => labels.map(l => l.getBoundingClientRect().top));
    expect(positions.length).toBe(2);
    expect(page.viewportSize()!.width < 600 ? positions[1] > positions[0] : positions[1] === positions[0]).toBe(true);
    await page.getByLabel('Số điện thoại', {exact: true}).fill('bad');
    await page.getByRole('button', {name: 'Lưu thay đổi'}).click();
    await expect(page.getByText('Số điện thoại không hợp lệ. Ví dụ: 0912345678.')).toBeVisible();
    expect(writes).toBe(0);
    await page.getByLabel('Họ và tên', {exact: true}).fill(' Hồ sơ mới ');
    await page.getByLabel('Số điện thoại', {exact: true}).fill('0987654321');
    stage('profile-save');
    const saved = page.waitForResponse(r => r.request().method() === 'PATCH' && r.url().endsWith('/users/me/profile'));
    await page.getByRole('button', {name: 'Lưu thay đổi'}).evaluate((button: HTMLButtonElement) => {button.click(); button.click();});
    const response = await saved;
    expect(response.status()).toBe(200);
    expect(response.headers()['cache-control']).toContain('no-store');
    expect(Boolean((await response.request().allHeaders()).cookie)).toBe(false);
    await expect(page.getByText('Đã cập nhật thông tin cá nhân.')).toBeVisible();
    expect(writes).toBe(1);
    stage('profile-persistence');
    const profile = await context.request.get(api + '/users/me/profile', {headers: account.headers});
    expect(profile.status()).toBe(200);
    const value = await profile.json();
    expect(Object.keys(value).sort()).toEqual(['email', 'emailVerified', 'fullName', 'id', 'phone', 'username']);
    expect(value.id === account.id && value.email === account.email).toBe(true);
    expect(value.fullName).toBe(' Hồ sơ mới ');
    expect(value.phone).toBe('+84987654321');
    expect(value.emailVerified).toBe(teacher);
    expect(JSON.parse(fixture('fixture', {action: 'snapshot', id: account.id}))).toEqual(before);
    stage('refresh-cookie-after');
    const cookiesAfter = (await context.cookies()).filter(c => c.name === 'refresh_token');
    expect(cookiesAfter.length).toBe(1);
    expect(cookiesAfter[0].value === cookiesBefore[0].value).toBe(true);
    expect(cookiesAfter[0].expires).toBe(cookiesBefore[0].expires);
    expect(JSON.stringify(cookiesAfter[0]) === JSON.stringify(cookiesBefore[0])).toBe(true);
    stage('authorization-validation');
    for (const data of [{role: 'ADMIN'}, {emailVerified: true}, {fullName: null}, {}]) {
      expect((await context.request.patch(api + '/users/me/profile', {headers: account.headers, data})).status()).toBe(400);
    }
    expect((await context.request.get(api + '/users/me/profile?userId=1', {headers: account.headers})).status()).toBe(400);
    expect((await context.request.get(api + '/users/me/profile')).status()).toBe(401);
    const me = await context.request.get(api + '/users/me', {headers: account.headers});
    expect(Object.keys(await me.json()).sort()).toEqual(['emailVerified', 'id', 'role']);
    if (teacher) {
      const onboarding = await context.request.get(api + '/teacher/onboarding', {headers: account.headers});
      expect((await onboarding.json()).status).toBe(state);
    }
    stage('session-refresh');
    expect((await mutation(context.request, '/auth/refresh')).status()).toBe(200);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    stage('locked-account');
    fixture('fixture', {action: 'lock', id: account.id});
    expect((await context.request.get(api + '/users/me/profile', {headers: account.headers})).status()).toBe(401);
  });
}
