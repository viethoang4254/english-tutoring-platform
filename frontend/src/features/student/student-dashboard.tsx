"use client";

import { checkoutReturnPath } from "../enrollment/checkout-path";
import { AccountWorkspace } from "../auth/account-workspace";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, useRef, type ReactNode } from "react";
import { Icon } from "../../components/site-shell";
import { useSession } from "../auth/session-provider";
import { authorized, signIn, authMutation, safeMessage, type Session } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import { dashboardTimeZone, dayKey, monthDays, previewCourses, previewSessions, shiftMonth, type PreviewSession } from "./dashboard-preview";
import styles from "./student-dashboard.module.css";
import { DashboardLayout } from "../../components/dashboard-layout";

const dateFormat = new Intl.DateTimeFormat("vi-VN", { timeZone: dashboardTimeZone, weekday: "long", day: "numeric", month: "long", year: "numeric" });
const timeFormat = new Intl.DateTimeFormat("vi-VN", { timeZone: dashboardTimeZone, hour: "2-digit", minute: "2-digit", hour12: false });
const monthFormat = new Intl.DateTimeFormat("vi-VN", { timeZone: dashboardTimeZone, month: "long", year: "numeric" });
type Profile = { fullName: string; email: string };
type ProfileState = { status: "loading" } | { status: "ready"; profile: Profile } | { status: "error"; forbidden: boolean };

const catalogPath = "/student/dashboard/courses";
const profilePath = "/account";
type PanelProps = {children?: ReactNode};
export function StudentDashboard({children}:PanelProps) {
  const { session,setSession } = useSession();
  const router = useRouter();
  const embedded=children!=null;
  const pathname=usePathname();
  const checkout=checkoutReturnPath(pathname);
  const restoration=useRef<ReturnType<typeof signIn>|null>(null);
  const [needsLogin,setNeedsLogin]=useState(false);
  useEffect(() => {
    if (session) {restoration.current=null;return;}
    if (checkout) {router.replace(`/login?next=${encodeURIComponent(checkout)}`);return;}
    if (!embedded) {router.replace("/login?next=/student/dashboard");return;}
    let active=true;
    restoration.current??=signIn();
    restoration.current.then(current=>{if(active)setSession(current);}).catch(()=>{if(active)setNeedsLogin(true);});
    return ()=>{active=false;};
  }, [session,router,embedded,setSession,checkout]);
  if (!session) return <main className={styles.gate}>{embedded&&needsLogin
    ? <AccountWorkspace dedicatedLogin stayOnPage />
    : <p role="status">Đang kiểm tra phiên đăng nhập…</p>}</main>;
  if (session.account.role !== "STUDENT") return <main className={styles.gate}><h1>Không gian dành cho học viên</h1><p role="alert">Tài khoản hiện tại không có quyền truy cập trang này.</p><Link href="/account">Về tài khoản của tôi</Link></main>;
  return <DashboardAccount key={session.accessToken} session={session}>{children}</DashboardAccount>;
}

function DashboardAccount({ session, children }: { session: Session } & PanelProps) {
  const { clearSession } = useSession();
  const [state, setState] = useState<ProfileState>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);
  const profileRead = useRef<{ attempt: number; request: ReturnType<typeof authorized<Profile>> } | null>(null);
  useEffect(() => {
    let active = true;
    if (profileRead.current?.attempt !== attempt) {
      profileRead.current = { attempt, request: authorized<Profile>(session.accessToken, "/users/me/profile") };
    }
    profileRead.current.request.then(profile => {
      if (!profile || typeof profile.fullName !== "string" || typeof profile.email !== "string") throw new Error("Invalid profile");
      if (active) setState({ status: "ready", profile });
    }).catch(error => {
      if (!active) return;
      if (error instanceof ApiRequestError && error.status === 401) { clearSession(); return; }
      setState({ status: "error", forbidden: error instanceof ApiRequestError && error.status === 403 });
    });
    return () => { active = false; };
  }, [session.accessToken, attempt, clearSession]);
  if (state.status === "loading") return <main className={styles.gate} aria-busy="true"><h1>Bảng học tập</h1><p role="status">Đang tải thông tin học viên…</p></main>;
  if (state.status === "error") return <main className={styles.gate}><h1>Chưa thể mở bảng học tập</h1><p role="alert">{state.forbidden ? "Tài khoản hiện tại không được phép truy cập. Vui lòng kiểm tra lại tài khoản." : "Không thể tải thông tin học viên. Vui lòng thử lại."}</p>{!state.forbidden && <button className="ui-button ui-button-primary" onClick={() => { setState({ status: "loading" }); setAttempt(value => value + 1); }}>Thử lại</button>} <Link href="/account">Về tài khoản</Link></main>;
  return <DashboardView profile={state.profile}>{children}</DashboardView>;
}

function DashboardView({ profile, children }: { profile: Profile } & PanelProps) {
  const pathname = usePathname();
  const inProfile = pathname === profilePath;
  const inCatalog = pathname === catalogPath || pathname.startsWith(catalogPath + "/");
  const [now] = useState(() => new Date());
  const today = dayKey(now);
  const [month, setMonth] = useState(today.slice(0, 7));
  const [selected, setSelected] = useState(today);
  const [empty, setEmpty] = useState(false);
  const {clearSession}=useSession();const router=useRouter();
  const [logoutMessage,setLogoutMessage]=useState("");const [loggingOut,setLoggingOut]=useState(false);const logoutFlight=useRef(false);
  async function logout(){if(logoutFlight.current)return;logoutFlight.current=true;setLoggingOut(true);try{await authMutation("/auth/logout");clearSession();router.replace("/login");}catch(error){setLogoutMessage(safeMessage(error));}finally{logoutFlight.current=false;setLoggingOut(false);}}
  const courses = empty ? [] : previewCourses;
  const sessions = empty ? [] : previewSessions(today);
  const selectedSessions = sessions.filter(session => dayKey(session.startsAt) === selected);
  const upcoming = sessions.filter(session => session.status === "SCHEDULED" && new Date(session.startsAt) > now).slice(0, 3);
  function navigateMonth(offset: number) {
    const next = shiftMonth(month, offset);
    setMonth(next); setSelected(next + "-01");
  }
  const inEnrollments=pathname==="/student/enrollments";
  return <DashboardLayout title={inProfile?"Hồ sơ và cài đặt":inCatalog?"Khám phá khóa học":inEnrollments?"Khóa học của tôi":"Tổng quan"} roleLabel="Không gian học viên" accountHref={profilePath} navigationLabel="Điều hướng học viên" busy={loggingOut} onLogout={()=>void logout()} navigation={<>
    <Link href="/student/dashboard" aria-current={!children?"page":undefined}>Tổng quan</Link>
    <Link href="/student/enrollments" aria-current={inEnrollments?"page":undefined}>Khóa học của tôi</Link>
    <Link href={catalogPath} aria-current={inCatalog?"page":undefined}>Khám phá khóa học</Link>
    <Link href="/student/dashboard#lich-hoc">Lịch học</Link>
    <Link href={profilePath} aria-current={inProfile?"page":undefined}>Hồ sơ và cài đặt</Link>
  </>}>
    {logoutMessage&&<p role="alert">{logoutMessage}</p>}
    <div className={styles.workspaceContent}>
      {children ?? <>
      <header className={styles.welcome}><div><p className={styles.eyebrow}>KHÔNG GIAN HỌC VIÊN</p><h1>Xin chào, {profile.fullName} <span aria-hidden="true">✦</span></h1><p>Sắp xếp lịch học. Sẵn sàng cho buổi học tiếp theo.</p></div><Link className={styles.profileLink} href={profilePath}>Hồ sơ của tôi ↗</Link></header>
      <section className={styles.previewNotice} aria-label="Thông báo dữ liệu minh họa"><div><strong>Bản xem trước · Dữ liệu minh họa</strong><p>Lịch và khóa học bên dưới là dữ liệu mẫu, không phải đăng ký học thật của bạn. Lịch này chỉ dùng để xem thử. <Link href="/student/enrollments">Xem đăng ký thực tế →</Link></p></div><label>Chế độ xem trước<select value={empty ? "empty" : "sample"} onChange={event => setEmpty(event.target.value === "empty")}><option value="sample">Có dữ liệu mẫu</option><option value="empty">Trạng thái trống</option></select></label></section>
      <div className={styles.scheduleLayout}>
        <section className={styles.calendar} id="lich-hoc" aria-labelledby="calendar-title">
          <div className={styles.sectionHeading}><div><p className={styles.eyebrow}>CHỦ ĐỘNG THỜI GIAN</p><h2 id="calendar-title">Lịch minh họa</h2></div><span className={styles.timezone}>Giờ Việt Nam · UTC+7</span></div>
          <div className={styles.calendarToolbar}><h3 aria-live="polite">{monthFormat.format(new Date(month + "-01T12:00:00Z"))}</h3><div><button onClick={() => { setMonth(today.slice(0, 7)); setSelected(today); }}>Hôm nay</button><button aria-label="Tháng trước" onClick={() => navigateMonth(-1)}>‹</button><button aria-label="Tháng sau" onClick={() => navigateMonth(1)}>›</button></div></div>
          <div className={styles.weekdays} aria-hidden="true">{["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map(day => <span key={day}>{day}</span>)}</div>
          <div className={styles.calendarGrid} role="group" aria-label="Các ngày trong tháng">
            {monthDays(month).map(day => {
              const items = sessions.filter(session => dayKey(session.startsAt) === day);
              const outside = !day.startsWith(month);
              return <button key={day} className={[styles.day, outside ? styles.outside : "", selected === day ? styles.selected : "", day === today ? styles.today : ""].join(" ")} aria-pressed={selected === day} aria-current={day === today ? "date" : undefined} aria-label={dateFormat.format(new Date(day + "T12:00:00Z")) + (items.length ? ", " + items.length + " buổi học minh họa" : ", không có buổi học")} onClick={() => { setSelected(day); if (outside) setMonth(day.slice(0, 7)); }}>
                <span>{Number(day.slice(-2))}</span>
                <span className={styles.dayEvents} aria-hidden="true">{items.map(item => <span key={item.id} className={item.status === "CANCELLED" ? styles.cancelledIndicator : styles.sessionIndicator}>{timeFormat.format(new Date(item.startsAt))}</span>)}</span>
              </button>;
            })}
          </div>
          <div className={styles.legend}><span><i /> Có buổi học</span><span><i className={styles.cancelledDot} /> Đã hủy</span><span>Nhấn vào ngày để xem lịch</span></div>
          <div className={styles.dayDetails} aria-live="polite"><h3>{dateFormat.format(new Date(selected + "T12:00:00Z"))}</h3>{selectedSessions.length ? selectedSessions.map(session => <SessionCard key={session.id} session={session} />) : <div className={styles.empty}><Icon kind="check" /><strong>Không có buổi học trong ngày này</strong><p>Chọn ngày có dấu đánh dấu để xem lịch minh họa.</p></div>}</div>
        </section>
        <aside className={styles.upcoming} aria-labelledby="upcoming-title"><p className={styles.eyebrow}>LỊCH MINH HỌA</p><h2 id="upcoming-title">Sắp diễn ra</h2><p className={styles.muted}>Các buổi trong lịch mẫu</p>{upcoming.length ? upcoming.map(session => <div key={session.id} className={styles.upcomingItem}><span className={styles.dateBadge}>{new Date(session.startsAt).toLocaleDateString("vi-VN", { timeZone: dashboardTimeZone, day: "2-digit", month: "2-digit" })}</span><div><strong>{session.title}</strong><p>{timeFormat.format(new Date(session.startsAt))} · Google Meet</p><button className={styles.textButton} onClick={() => { const day = dayKey(session.startsAt); setSelected(day); setMonth(day.slice(0, 7)); document.getElementById("lich-hoc")?.scrollIntoView({ block: "start" }); }}>Xem trong lịch →</button></div></div>) : <p className={styles.empty}>Chưa có buổi học sắp tới.</p>}<div className={styles.meetNote}><Icon kind="video" /><p>Mỗi khóa học dùng một liên kết Google Meet do gia sư cung cấp. Bản xem trước không mở phòng học thật.</p></div></aside>
      </div>
      <section id="khoa-hoc-da-dang-ky" aria-labelledby="courses-title"><div className={styles.sectionHeading}><div><p className={styles.eyebrow}>HÀNH TRÌNH CỦA BẠN</p><h2 id="courses-title">Khóa học đã đăng ký</h2></div><span className={styles.pill}>Dữ liệu minh họa</span></div>
        {courses.length ? <div className={styles.courseGrid}>{courses.map(course => <article className={styles.courseCard} key={course.id}><div className={course.tone === "cyan" ? styles.courseCyan : styles.courseIndigo}><Icon kind="book" /><span>{course.category}</span></div><div className={styles.courseBody}><span className={styles.pill}>Khóa học mẫu</span><h3>{course.title}</h3><p>{course.teacher}</p><div className={styles.courseMeta}><span>{course.sessionCount} buổi học</span><span>Trực tuyến · Google Meet</span></div><a href="#lich-hoc">Xem lịch minh họa <span aria-hidden="true">↗</span></a></div></article>)}</div> : <div className={styles.empty}><Icon kind="book" /><h3>Chưa có khóa học trong bản xem trước</h3><p>Đây là trạng thái đăng ký trống trong bản xem trước, không phải ngày không có buổi học.</p><Link className="ui-button ui-button-primary" href={catalogPath}>Khám phá khóa học</Link></div>}
      </section>
      </>}
    </div>
  </DashboardLayout>;
}

function SessionCard({ session }: { session: PreviewSession }) {
  const course = previewCourses.find(course => course.id === session.courseId)!;
  return <article className={styles.sessionCard}><div className={styles.sessionTime}><strong>{timeFormat.format(new Date(session.startsAt))}</strong><span>{timeFormat.format(new Date(session.endsAt))}</span></div><div className={styles.sessionInfo}><span className={styles.muted}>{course.title}</span><h4>{session.title}</h4><p>{course.teacher} · Google Meet</p><span className={session.status === "CANCELLED" ? styles.cancelled : styles.pill}>{session.status === "CANCELLED" ? "Đã hủy · minh họa" : "Đã lên lịch · minh họa"}</span></div><button disabled title="Bản xem trước không cung cấp liên kết phòng học">Vào lớp</button></article>;
}
