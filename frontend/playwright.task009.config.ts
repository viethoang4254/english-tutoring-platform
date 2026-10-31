import {defineConfig} from '@playwright/test';
import path from 'node:path';

if (process.env.GITHUB_ACTIONS !== 'true' || process.env.RUNNER_ENVIRONMENT !== 'github-hosted' || !process.env.RUNNER_TEMP) {
  throw new Error('TASK-009 acceptance requires an isolated GitHub-hosted runner');
}
export default defineConfig({
  testDir: './tests',
  testMatch: ['e2e/student-enrollments.spec.ts', 'e2e/student-profile.spec.ts', 'live/profile.spec.ts', 'live/calendar.spec.ts',
    'e2e/teacher-calendar.spec.ts', 'e2e/unified-dashboard.spec.ts', 'e2e/teacher-login.spec.ts',
    'e2e/teacher-courses.spec.ts', 'e2e/teacher-sessions.spec.ts',
    'e2e/admin-categories.spec.ts', 'e2e/admin-dashboard.spec.ts', 'e2e/course-discovery.spec.ts',
    'e2e/student-dashboard.spec.ts', 'e2e/teacher-dashboard.spec.ts'],
  workers: 1, retries: 0, forbidOnly: true, timeout: 60_000,
  reporter: './tests/support/task009-reporter.ts',
  outputDir: path.join(process.env.RUNNER_TEMP, 'task009', 'browser-results'),
  use: {baseURL: 'http://127.0.0.1:3000', browserName: 'chromium', trace: 'off', screenshot: 'off', video: 'off'},
  projects: [
    {name: 'mobile', use: {viewport: {width: 390, height: 844}}},
    {name: 'desktop', use: {viewport: {width: 1440, height: 900}}},
  ],
  webServer: {command: 'npm run start -- --hostname 127.0.0.1 --port 3000', url: 'http://127.0.0.1:3000',
    reuseExistingServer: false, timeout: 60_000, stdout: 'ignore', stderr: 'ignore'},
});
