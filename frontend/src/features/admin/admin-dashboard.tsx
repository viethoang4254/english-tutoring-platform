"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useSession } from "../auth/session-provider";
import { authMutation, safeMessage } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import { AdminApplications } from "../teacher/admin-applications";
import { AdminReviews } from "../receiving/admin-reviews";
import { AdminCategories } from "./admin-categories";
import styles from "./admin-dashboard.module.css";
import { DashboardLayout } from "../../components/dashboard-layout";
import { AccountWorkspace } from "../auth/account-workspace";

const navigation = [
  ["overview", "Tổng quan"], ["users", "Quản lý người dùng"],
  ["reviews", "Xét duyệt gia sư"], ["categories", "Quản lý danh mục"],
  ["receiving", "Xét duyệt tài khoản nhận tiền"], ["courses", "Tổng quan khóa học"], ["statistics", "Thống kê giao dịch"], ["account", "Tài khoản & bảo mật"],
] as const;
type Section = typeof navigation[number][0];

export function AdminDashboard({sectionName,reviewId}:{sectionName?:string;reviewId?:string}) {
  const { session } = useSession();
  const router = useRouter();
  useEffect(() => { if (!session) router.replace("/login?next=/admin/dashboard"); }, [session, router]);
  if (!session) return <main className={styles.gate} role="status">Đang chuyển đến trang đăng nhập…</main>;
  if (session.account.role !== "ADMIN" || !session.account.emailVerified) return <main className={styles.gate}><h1>Không có quyền truy cập</h1><p>Trang này dành cho quản trị viên đủ điều kiện.</p><Link href="/account">Về tài khoản</Link></main>;
  return <AdminView key={`${session.accessToken}:${sectionName ?? ""}:${reviewId ?? ""}`} initialSection={navigation.find(([key])=>key===sectionName)?.[0] ?? "overview"} reviewId={reviewId} />;
}

function AdminView({initialSection,reviewId}:{initialSection:Section;reviewId?:string}) {
  const { session, clearSession } = useSession();
  const router=useRouter();
  const section=initialSection;
  function setSection(next:Section) {
    router.push(next==="overview"?"/admin/dashboard":`/admin/dashboard?section=${next}`);
  }

  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const pending = useRef(false);
  async function run(work: () => Promise<void>) {
    if (pending.current) return;
    pending.current = true; setBusy(true); setMessage("");
    try { await work(); }
    catch (error) {
      if (error instanceof ApiRequestError && error.status === 401) clearSession();
      setMessage(safeMessage(error));
    } finally { pending.current = false; setBusy(false); }
  }
  if (!session) return null;
  return <DashboardLayout title={navigation.find(([key])=>key===section)?.[1]??"Quản trị"} roleLabel="Không gian quản trị" accountHref="/admin/dashboard?section=account" navigationLabel="Điều hướng quản trị" busy={busy} onLogout={()=>void run(async()=>{await authMutation("/auth/logout");clearSession();})} navigation={<>{navigation.map(([key,label])=><Link key={key} href={key==="overview"?"/admin/dashboard":`/admin/dashboard?section=${key}`} aria-current={section===key?"page":undefined}>{label}</Link>)}</>}>
      {message && <p role="alert" className={styles.notice}>{message}</p>}
      {busy && <p role="status">Đang xử lý…</p>}
      {section === "overview" && <>
        <section className={styles.card}><span className={styles.badge}>Đã kết nối chức năng</span><h2>Xét duyệt hồ sơ gia sư</h2><p>Xem hồ sơ, phê duyệt hoặc từ chối qua hệ thống xét duyệt hiện có.</p><button className="auth-primary" onClick={() => setSection("reviews")}>Mở danh sách chờ xét duyệt</button></section>
        <p className={styles.note}>Danh mục và xét duyệt đã có API. Quản lý người dùng thuộc nhánh Admin CRUD riêng, chưa tích hợp tại đây. Chưa có API thống kê tổng hợp.</p>
      </>}
      {section === "account" && <AccountWorkspace embeddedPanel="settings" dashboardPanel="password"/>}
      {section === "reviews" && <div className={`${styles.card} account-workspace`}><AdminApplications session={session} busy={busy} run={run} /></div>}
      {section === "categories" && <AdminCategories session={session} busy={busy} run={run} />}
      {section === "receiving" && <div className={styles.card}><AdminReviews key={reviewId ?? "queue"} session={session} id={reviewId}/></div>}
      {section !== "account" && section !== "overview" && section !== "reviews" && section !== "categories" && section !== "receiving" && <section className={styles.card}>
        <span className={styles.badge}>Chưa khả dụng</span><h2>Chức năng đang được chuẩn bị</h2>
        <p>{section === "users" ? "Chưa kết nối API quản lý người dùng." : section === "courses" ? "Chưa kết nối API tổng quan khóa học. Gia sư sở hữu và tạo khóa học; quản trị viên không tạo khóa học." : "Chưa kết nối API thống kê giao dịch. Học phí được thanh toán trực tiếp cho gia sư; nền tảng không giữ tiền hoặc thực hiện chi trả."}</p>
        <p>Không hiển thị dữ liệu giả hoặc mô phỏng thao tác thành công.</p>
      </section>}
  </DashboardLayout>;
}
