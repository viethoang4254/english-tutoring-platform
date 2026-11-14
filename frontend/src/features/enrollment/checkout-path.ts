export function checkoutReturnPath(value: unknown): string | null {
  return typeof value === "string" && /^\/student\/dashboard\/courses\/[1-9][0-9]*\/checkout$/.test(value) ? value : null;
}
export function checkoutPath(courseId: string) { return `/student/dashboard/courses/${encodeURIComponent(courseId)}/checkout`; }
