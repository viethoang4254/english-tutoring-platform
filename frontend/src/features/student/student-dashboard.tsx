"use client";

import { checkoutReturnPath } from "../enrollment/checkout-path";
import { AccountWorkspace } from "../auth/account-workspace";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState, useRef, type ReactNode } from "react";
import { StudentCalendar } from "./student-calendar";
import { useSession } from "../auth/session-provider";
import { authorized, signIn, authMutation, safeMessage, type Session } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import styles from "./student-dashboard.module.css";
import { DashboardLayout } from "../../components/dashboard-layout";

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
  const hadSession=useRef(false);
  const restoration=useRef<ReturnType<typeof signIn>|null>(null);
  const [needsLogin,setNeedsLogin]=useState(false);
  useEffect(() => {
    if (session) {hadSession.current=true;restoration.current=null;return;}
    // A rejected established schedule session must not silently refresh itself.
    if (pathname==="/student/dashboard/schedule" && hadSession.current) {router.replace("/login?next=/student/dashboard");return;}
    if (checkout) {router.replace(`/login?next=${encodeURIComponent(checkout)}`);return;}
    if (!embedded) {router.replace("/login?next=/student/dashboard");return;}
    let active=true;
    restoration.current??=signIn();
    restoration.current.then(current=>{if(active)setSession(current);}).catch(()=>{if(active)setNeedsLogin(true);});
    return ()=>{active=false;};
  }, [session,router,embedded,setSession,checkout,pathname]);
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
  const inSchedule = pathname === "/student/dashboard/schedule";
  const inProfile = pathname === profilePath;
  const inCatalog = pathname === catalogPath || pathname.startsWith(catalogPath + "/");
  const {clearSession}=useSession();const router=useRouter();
  useEffect(()=>{if(pathname==="/student/dashboard" && (window.location.hash==="#lich-hoc" || new URLSearchParams(window.location.search).has("day")))router.replace("/student/dashboard/schedule"+window.location.search+window.location.hash);},[pathname,router]);
  const [logoutMessage,setLogoutMessage]=useState("");const [loggingOut,setLoggingOut]=useState(false);const logoutFlight=useRef(false);
  async function logout(){if(logoutFlight.current)return;logoutFlight.current=true;setLoggingOut(true);try{await authMutation("/auth/logout");clearSession();router.replace("/login");}catch(error){setLogoutMessage(safeMessage(error));}finally{logoutFlight.current=false;setLoggingOut(false);}}
  const inEnrollments=pathname==="/student/enrollments";
  return <DashboardLayout showAccountMenu={false} title={inSchedule?"Lịch học":inProfile?"Hồ sơ và cài đặt":inCatalog?"Khám phá khóa học":inEnrollments?"Khóa học của tôi":"Tổng quan"} roleLabel="Không gian học viên" accountHref={profilePath} navigationLabel="Điều hướng học viên" busy={loggingOut} onLogout={()=>void logout()} navigation={<>
    <Link href="/student/dashboard" aria-current={!children?"page":undefined}>Tổng quan</Link>
    <Link href="/student/enrollments" aria-current={inEnrollments?"page":undefined}>Khóa học của tôi</Link>
    <Link href={catalogPath} aria-current={inCatalog?"page":undefined}>Khám phá khóa học</Link>
    <Link href="/student/dashboard/schedule" aria-current={inSchedule?"page":undefined}>Lịch học</Link>
    <Link href={profilePath} aria-current={inProfile?"page":undefined}>Hồ sơ và cài đặt</Link>
  </>}>
    {logoutMessage&&<p role="alert">{logoutMessage}</p>}
    <div className={styles.workspaceContent}>
      {children ?? <>
      <header className={styles.welcome}><div><p className={styles.eyebrow}>KHÔNG GIAN HỌC VIÊN</p><h1>Xin chào, {profile.fullName} <span aria-hidden="true">✦</span></h1><p>Sắp xếp lịch học. Sẵn sàng cho buổi học tiếp theo.</p></div><Link className={styles.profileLink} href={profilePath}>Hồ sơ của tôi ↗</Link></header>
      <StudentCalendar overview />
      </>}
    </div>
  </DashboardLayout>;
}
