import {expect, type APIRequestContext} from '@playwright/test';
import {execFileSync} from 'node:child_process';
import {randomUUID} from 'node:crypto';
import path from 'node:path';
export const api = 'http://127.0.0.1:8081/api/v1';
const support = path.resolve(__dirname, '../../../ci/task009/run.py');
export function fixture(action: string, data: object): string {
  try {
    return execFileSync('python', [support, action], {input: JSON.stringify(data), encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe'], timeout: 15_000}).trim();
  } catch { throw new Error('Isolated fixture operation failed; private diagnostics withheld'); }
}
export async function mutation(request: APIRequestContext, route: string, data?: object) {
  const csrf = await request.get(api + '/auth/csrf');
  expect(csrf.status()).toBe(200);
  return request.post(api + route, {headers: {'X-XSRF-TOKEN': (await csrf.json()).token}, data});
}
export async function register(request: APIRequestContext, teacher: boolean) {
  const uuid = randomUUID();
  const account = {email: `ci-${uuid}@example.invalid`, password: 'Test-' + randomUUID(), fullName: 'Hồ sơ kiểm thử'};
  const response = await mutation(request, '/auth/register/' + (teacher ? 'teacher' : 'student'), {
    ...account, ...(teacher ? {specialization: 'English', teachingExperience: 'Teaching snapshot', introduction: 'Original snapshot'} :
      {username: 'u' + uuid.replaceAll('-', '').slice(0, 20), phone: '0912345678'}),
  });
  expect(response.status()).toBe(201);
  expect((await response.json()).verificationRequired).toBe(teacher);
  if (teacher) {
    expect((await mutation(request, '/auth/login', {email: account.email, password: account.password})).status()).toBe(403);
    const token = fixture('verification-code', {email: account.email});
    expect((await mutation(request, '/auth/email-verification', {token})).status()).toBe(204);
  }
  const login = await mutation(request, '/auth/login', {email: account.email, password: account.password});
  expect(login.status()).toBe(200);
  const token: string = (await login.json()).accessToken;
  const headers = {Authorization: 'Bearer ' + token};
  const me = await request.get(api + '/users/me', {headers});
  expect(me.status()).toBe(200);
  return {...account, token, headers, id: (await me.json()).id as string};
}
