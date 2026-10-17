import type {Reporter, TestCase, TestResult, FullResult} from '@playwright/test/reporter';
import {appendFileSync} from 'node:fs';

// Assertion diagnostics, request bodies and browser output can contain credentials.
export default class SafeReporter implements Reporter {
  private counts = {passed: 0, failed: 0, errors: 0, skipped: 0};
  onTestEnd(test: TestCase, result: TestResult) {
    if (result.status === 'passed') this.counts.passed++;
    else if (result.status === 'skipped') this.counts.skipped++;
    else if (result.status === 'failed') this.counts.failed++;
    else this.counts.errors++;
    console.log(`${result.status}: ${test.location.file.split(/[\\/]/).pop()}:${test.location.line}`);
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
