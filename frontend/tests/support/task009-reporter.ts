import type {Reporter, TestCase, TestResult, FullResult, TestStep} from '@playwright/test/reporter';
import {appendFileSync} from 'node:fs';
import path from 'node:path';

const stages = new Set(['registration', 'onboarding-review', 'snapshot-before', 'browser-login',
  'refresh-cookie-before', 'profile-edit', 'profile-save', 'profile-persistence',
  'refresh-cookie-after', 'authorization-validation', 'session-refresh', 'locked-account',
  'calendar-fixtures', 'calendar-api', 'calendar-browser', 'calendar-navigation', 'calendar-other-timezone']);
function safeLocation(location: TestCase['location'] | undefined): string {
  if (!location || !Number.isSafeInteger(location.line) || location.line < 1) return 'unavailable';
  for (const file of ['tests/live/profile.spec.ts', 'tests/e2e/student-profile.spec.ts',
    'tests/live/calendar.spec.ts', 'tests/support/live-fixtures.ts', 'tests/e2e/teacher-calendar.spec.ts',
    'tests/e2e/unified-dashboard.spec.ts', 'tests/e2e/teacher-login.spec.ts',
    'tests/e2e/teacher-courses.spec.ts', 'tests/e2e/teacher-sessions.spec.ts']) {
    if (path.resolve(location.file) === path.resolve(__dirname, '../../', file)) return `${file}:${location.line}`;
  }
  return 'unavailable';
}

// Assertion diagnostics, request bodies and browser output can contain credentials.
export default class SafeReporter implements Reporter {
  private counts = {passed: 0, failed: 0, errors: 0, skipped: 0};
  private failureLocations = new WeakMap<TestResult, string>();
  onStepEnd(_test: TestCase, result: TestResult, step: TestStep) {
    const location = safeLocation(step.location);
    if (step.error && location !== 'unavailable' && !this.failureLocations.has(result)) {
      this.failureLocations.set(result, location);
    }
  }
  onTestEnd(test: TestCase, result: TestResult) {
    if (result.status === 'passed') this.counts.passed++;
    else if (result.status === 'skipped') this.counts.skipped++;
    else if (result.status === 'failed') this.counts.failed++;
    else this.counts.errors++;
    const stage = test.annotations.filter(a => a.type === 'task009-stage' && stages.has(a.description ?? '')).at(-1)?.description ?? 'unavailable';
    const failure = result.errors.map(error => safeLocation(error.location)).find(location => location !== 'unavailable')
      ?? this.failureLocations.get(result) ?? 'unavailable';
    console.log(`${result.status}: declaration=${safeLocation(test.location)} stage=${stage} failure=${failure}`);
  }
  onError() { this.counts.errors++; }
  onStdOut() {}
  onStdErr() {}
  async onEnd(result: FullResult): Promise<{status: 'passed' | 'failed'}> {
    const summary = `TASK-009 browser counts: ${JSON.stringify(this.counts)}`;
    console.log(summary);
    if (process.env.GITHUB_STEP_SUMMARY) appendFileSync(process.env.GITHUB_STEP_SUMMARY, summary + '\n');
    const expected = Number(process.env.TASK009_EXPECTED_TESTS);
    return {status: result.status === 'passed' && expected > 0 && this.counts.passed === expected &&
      !this.counts.failed && !this.counts.errors && !this.counts.skipped ? 'passed' : 'failed'};
  }
}
