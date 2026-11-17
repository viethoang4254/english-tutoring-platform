import { expect, type Page } from "@playwright/test";
import { teacherPanel } from "./teacher-navigation";
import { calendarPage } from "./student-calendar";

export async function googleFixture(page: Page, role: "STUDENT" | "TEACHER" = "TEACHER") {
  await page.clock.setFixedTime(new Date("2026-10-18T03:00:00Z"));
  const state = { sync: undefined as {state:string;pending:number;failed:number;lastSuccess:string|null}|undefined, syncRetries:0, status: "DISCONNECTED", http: 200, gets: 0, connects: 0, disconnects: 0, refreshes: 0, remoteRevoked: true };
  await page.route("**/api/v1/**", route => {
    const request = route.request(), url = new URL(request.url()), path = url.pathname;
    if (path.endsWith("/csrf")) return route.fulfill({ json: { token: "synthetic-auth-csrf" } });
    if (path.endsWith("/login") || path.endsWith("/refresh")) {
      if (path.endsWith("/refresh")) state.refreshes++;
      return route.fulfill({ json: { accessToken: "synthetic-access", tokenType: "Bearer" } });
    }
    if (path.endsWith("/users/me")) return route.fulfill({ json: { id: "3", role, emailVerified: false } });
    if (path.endsWith("/profile")) return route.fulfill({ json: { fullName: "Người dùng kiểm thử", email: "synthetic@example.test", emailVerified: false } });
    if (path.endsWith("/teacher/onboarding")) return route.fulfill({ json: { applicationId: "1", status: "APPROVED" } });
    if (path.endsWith("/student/calendar/sessions")) return route.fulfill({ json: calendarPage(url) });
    if (path.endsWith("/teacher/calendar/sessions") || path.endsWith("/teacher/courses")) return route.fulfill({ json: { items: [], page: 0, size: 100, hasNext: false } });
    if (path.startsWith("/api/v1/integrations/google-calendar")) {
      expect(request.headers().authorization).toBe("Bearer synthetic-access");
      if (request.method() !== "GET") expect(request.headers()["x-xsrf-token"]).toBe("synthetic-google-csrf");
      if (state.http !== 200) return route.fulfill({ status: state.http, json: { code: "UNAVAILABLE", message: "PRIVATE" } });
      if (path.endsWith("/sync") && request.method() === "POST") {state.syncRetries++;state.sync={state:"PENDING",pending:1,failed:0,lastSuccess:null};return route.fulfill({status:202});}
      if (request.method() === "POST") {
        state.connects++;expect(request.postData()).toBeNull();
        return route.fulfill({ json: { authorizationUrl: "https://accounts.google.com/o/oauth2/v2/auth?state=synthetic-intent" } });
      }
      if (request.method() === "DELETE") { state.disconnects++;state.status = "DISCONNECTED";return route.fulfill({ json: { disconnected: true, remoteRevoked: state.remoteRevoked } }); }
      state.gets++;
      return route.fulfill({ json: { sync:state.sync, csrfToken: "synthetic-google-csrf", connection: { status: state.status, email: state.status === "DISCONNECTED" || state.status === "UNAVAILABLE" ? null : "calendar.fixture@example.test", reconnectRequired: state.status === "RECONNECT_REQUIRED", connectedAt: "2026-10-18T03:00:00Z", synchronizationAvailable: !!state.sync } } });
    }
    return route.fulfill({ status: 404, json: { code: "NOT_FOUND", message: "Unavailable" } });
  });
  await page.route("https://accounts.google.com/o/oauth2/v2/auth?**", route => {
    state.status = "CONNECTED";
    return route.fulfill({ status: 302, headers: { location: new URL("/account?googleCalendar=CONNECTED", process.env.NEXT_PUBLIC_SITE_ORIGIN ?? "http://127.0.0.1:3000").href } });
  });
  return state;
}
export async function openGoogle(page: Page, role: "STUDENT" | "TEACHER" = "TEACHER") {
  await page.goto("/login");
  await page.getByLabel("Email", { exact: true }).fill("synthetic@example.test");
  await page.getByLabel("Mật khẩu", { exact: true }).fill("synthetic-password");
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(page).toHaveURL(role === "STUDENT" ? /\/student\/dashboard$/ : /\/teacher$/);
  if (role === "TEACHER") await teacherPanel(page, "Lịch dạy");
  const calendar = page.getByRole("region", { name: role === "TEACHER" ? "Lịch dạy của tôi" : "Lịch học đã đăng ký", exact: true });
  if(role==="TEACHER") await calendar.locator("summary").filter({ hasText: "Kết nối Google Calendar" }).click();
  return calendar.getByRole("region", { name: "Kết nối Google Calendar", exact: true });
}
