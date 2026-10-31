"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { useSession } from "../auth/session-provider";
import { authMutation, authorized, safeMessage, type Account, type Onboarding } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import { AccountWorkspace } from "../auth/account-workspace";
import { StudentEnrollments } from "../enrollment/student-enrollments";
import { TeacherCalendar } from "./teacher-calendar";
import { TeacherCourses } from "./teacher-courses";
import styles from "../admin/admin-dashboard.module.css";
import layout from "../auth/account-layout.module.css";

export function TeacherDashboard({ children }: { children: ReactNode }) {
  const { session, clearSession } = useSession();
  const path = usePathname(), search = useSearchParams(), router = useRouter();
  const [eligible, setEligible] = useState(false), [checkedToken, setCheckedToken] = useState("");
  const [attempt, setAttempt] = useState(0), [message, setMessage] = useState("");
  const [open, setOpen] = useState(false), [busy, setBusy] = useState(false);
  const logoutFlight = useRef(false);
  const toggle = useRef<HTMLButtonElement>(null);
  const heading = useRef<HTMLHeadingElement>(null);
  const [selectedCourse, setSelectedCourse] = useState<string>();
  const [hadSession, setHadSession] = useState(false);
  if (session && !hadSession) setHadSession(true);
  const courseId = /^\/teacher\/courses\/([1-9][0-9]*)$/.exec(path)?.[1];
  if (courseId && courseId !== selectedCourse) setSelectedCourse(courseId);
  const checked = !!session && checkedToken === session.accessToken;
  useEffect(() => { if(!session && (path.startsWith("/teacher")||path.startsWith("/student/enrollments"))&&!message&&!hadSession)router.replace(`/login?next=${encodeURIComponent(path)}`); }, [session,path,router,message,hadSession]);
  useEffect(() => {
    if (!session) return;
    let active = true;
    async function check() {
      try {
        const account = await authorized<Account>(session!.accessToken, "/users/me");
        if (account?.id !== session!.account.id || account.role !== session!.account.role) throw new ApiRequestError(403,"ACCESS_DENIED","Denied");
        let allowed = false;
        if(account.role === "TEACHER" && account.emailVerified) {
          const onboarding = await authorized<Onboarding>(session!.accessToken,"/teacher/onboarding");
          allowed = onboarding?.status === "APPROVED";
        }
        if(active) {setEligible(allowed); setMessage("");}
      } catch(error) {
        if(active) {setEligible(false);setMessage(safeMessage(error)); if(error instanceof ApiRequestError && error.status===401)clearSession();}
      } finally {if(active)setCheckedToken(session!.accessToken);}
    }
    void check(); return () => {active=false;};
  }, [session, attempt, clearSession]); // Eligibility is session-scoped; every protected API still rechecks current authority.
  if(!session) return <main className={styles.gate}>{(message||hadSession)&&<p role="status">{message || "Phiên đăng nhập đã kết thúc. Vui lòng đăng nhập lại."}</p>}{path==="/account" || message || hadSession ? <AccountWorkspace /> : <p role="status">Đang chuyển đến trang đăng nhập…</p>}</main>;
  const section=search.get("section");
  const coursePath=selectedCourse?`/teacher/courses/${selectedCourse}`:"/teacher/courses";
  const enrollmentPage = path === "/student/enrollments";
  const items = [
    {href:"/teacher",label:"Tổng quan",key:"overview"},
    ...(session.account.role==="STUDENT" ? [{href:"/student/enrollments",label:"Đăng ký của tôi",key:"enrollments"}] : []),
    ...(eligible && checked ? [
      {href:"/teacher/courses",label:"Khóa học của tôi",key:"courses"},
      {href:"/teacher/calendar",label:"Lịch dạy",key:"calendar"},
      {href:`${coursePath}?section=sessions`,label:"Buổi học",key:"sessions"},
    ] : []),
    ...(session.account.role==="TEACHER" ? [{href:"/teacher/profile",label:"Hồ sơ gia sư",key:"teacher"}] : []),
    {href:"/account",label:"Tài khoản & hồ sơ đăng ký",key:"account"},
    {href:"/teacher/settings",label:"Cài đặt tài khoản",key:"settings"},
  ];
  const active = enrollmentPage ? "enrollments" : path==="/teacher/calendar" ? "calendar" : path==="/account" ? "account" : path==="/teacher/settings" ? "settings" : path==="/teacher/profile" ? "teacher" : path.startsWith("/teacher/courses") ? section==="schedule"?"schedule":section==="sessions"?"sessions":"courses" : "overview";
  const teachingPage=["courses","schedule","sessions","calendar"].includes(active);
  const title=items.find(i=>i.key===active)?.label ?? "Không gian tài khoản";
  async function logout() {
    if(logoutFlight.current)return;logoutFlight.current=true;setBusy(true);
    try {await authMutation("/auth/logout");setMessage("Đã kết thúc phiên đăng nhập hiện tại.");clearSession();router.replace("/account");}
    catch(error){clearSession();setMessage(`${safeMessage(error)} chưa xác nhận được việc đăng xuất trên máy chủ.`);router.replace("/account");}
    finally{logoutFlight.current=false;setBusy(false);}
  }
  return <main className={`account-workspace ${layout.workspace}`}>
    <div className={styles.dashboard}>
      <aside className={`${styles.sidebar} ${layout.sidebar}`}>
        <div className={styles.identity}><span aria-hidden="true">TK</span><strong>Không gian của bạn</strong></div>
        <button ref={toggle} className={styles.mobileToggle} aria-controls="dashboard-navigation" aria-expanded={open} onClick={()=>setOpen(!open)}>Điều hướng</button>
        <nav id="dashboard-navigation" data-open={open} aria-label="Điều hướng Dashboard" className={open?styles.open:styles.navigation} onKeyDown={event=>{if(event.key==="Escape"){setOpen(false);toggle.current?.focus();}}}>
          {items.map(item=><Link key={item.key} href={item.href} aria-current={active===item.key?"page":undefined} onClick={()=>{setOpen(false);heading.current?.focus();}}>{item.label}</Link>)}
          <button disabled={busy} onClick={()=>void logout()}>Đăng xuất</button>
        </nav>
      </aside>
      <div className={styles.content}>
        <header className={styles.welcome}><p>DASHBOARD</p><h1 ref={heading} tabIndex={-1}>{title}</h1></header>
        {message&&<p role="alert">{message}</p>}
        {teachingPage&&!checked&&<p role="status">Đang kiểm tra quyền giảng dạy…</p>}
        {teachingPage&&checked&&!eligible&&<section><h2>Chưa thể mở không gian gia sư</h2><p>Quyền giảng dạy yêu cầu hồ sơ được phê duyệt và tài khoản đủ điều kiện.</p><button onClick={()=>setAttempt(attempt+1)}>Kiểm tra lại</button><Link href="/account">Xem hồ sơ đăng ký và tài khoản</Link></section>}
        {eligible&&checked&&active==="calendar"&&<TeacherCalendar session={session}/>}
        {eligible&&checked&&<div hidden={!teachingPage||active==="calendar"}>
          {teachingPage&&!courseId&&section&&<p>Chọn một khóa học để quản lý lịch dạy và các buổi học.</p>}
          <TeacherCourses session={session} courseId={courseId ?? (teachingPage&&active!=="calendar"?undefined:selectedCourse)} section={section === "schedule" || section === "sessions" ? section : undefined}/>
        </div>}
        {enrollmentPage && (session.account.role === "STUDENT" ? <StudentEnrollments key={`${session.accessToken}:${search.get("id") ?? "list"}`} session={session} enrollmentId={search.get("id")} /> : <p role="alert">Trang đăng ký chỉ dành cho học viên.</p>)}
        <div hidden={teachingPage || enrollmentPage}>
          <AccountWorkspace embeddedPanel="dashboard" dashboardPanel={active==="teacher"?"teacher":active==="settings"?"password":active==="account"?(session.account.role==="TEACHER"?"teacher":session.account.role==="ADMIN"?"reviews":"profile"):"overview"}/>
          {active==="account"&&session.account.role==="STUDENT"&&<p>Đăng ký gia sư sử dụng tài khoản riêng; tài khoản học viên không được chuyển vai trò.</p>}
        </div>
        <div hidden>{children}</div>
      </div>
    </div>
  </main>;
}
