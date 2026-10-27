"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useSession } from "../auth/session-provider";
import { authMutation, safeMessage } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import { AdminApplications } from "../teacher/admin-applications";
import { AdminCategories } from "./admin-categories";
import styles from "./admin-dashboard.module.css";

const navigation = [
  ["overview", "Tổng quan"], ["users", "Quản lý người dùng"],
  ["reviews", "Xét duyệt gia sư"], ["categories", "Quản lý danh mục"],
  ["courses", "Tổng quan khóa học"], ["statistics", "Doanh thu / Thống kê"],
] as const;
type Section = typeof navigation[number][0];

export function AdminDashboard() {
  const { session } = useSession();
  const router = useRouter();
  useEffect(() => { if (!session) router.replace("/login?next=/admin/dashboard"); }, [session, router]);
  if (!session) return <main className={styles.gate} role="status">Đang chuyển đến trang đăng nhập…</main>;
  if (session.account.role !== "ADMIN" || !session.account.emailVerified) return <main className={styles.gate}><h1>Không có quyền truy cập</h1><p>Trang này dành cho quản trị viên đủ điều kiện.</p><Link href="/account">Về tài khoản</Link></main>;
  return <AdminView key={session.accessToken} />;
}

function AdminView() {
  const { session, clearSession } = useSession();
  const [section, setSection] = useState<Section>("overview");
  const [mobileOpen, setMobileOpen] = useState(false);
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
  return <main className={styles.dashboard}>
    <aside className={styles.sidebar}>
      <div className={styles.identity}><span aria-hidden="true">QT</span><div><strong>Không gian quản trị</strong><p>Quản trị viên</p></div></div>
      <button className={styles.mobileToggle} aria-expanded={mobileOpen} aria-controls="admin-navigation" onClick={() => setMobileOpen(!mobileOpen)}>Điều hướng quản trị</button>
      <nav id="admin-navigation" aria-label="Điều hướng quản trị" className={mobileOpen ? styles.open : styles.navigation}>
        {navigation.map(([key, label]) => <button key={key} disabled={busy} aria-current={section === key ? "page" : undefined} onClick={() => { setSection(key); setMobileOpen(false); setMessage(""); }}>{label}</button>)}
        <Link href="/account">Tài khoản & bảo mật</Link>
        <button disabled={busy} onClick={() => void run(async () => { await authMutation("/auth/logout"); clearSession(); })}>Đăng xuất</button>
      </nav>
    </aside>
    <div className={styles.content}>
      <header className={styles.welcome}><p>KHÔNG GIAN QUẢN TRỊ</p><h1>{navigation.find(([key]) => key === section)?.[1]}</h1><span>Quản lý nền tảng, đồng hành cùng gia sư và học viên.</span></header>
      {message && <p role="alert" className={styles.notice}>{message}</p>}
      {busy && <p role="status">Đang xử lý…</p>}
      {section === "overview" && <>
        <section className={styles.metrics} aria-label="Thống kê nền tảng">
          {["Tổng người dùng", "Tổng học viên", "Tổng gia sư", "Tổng khóa học", "Hồ sơ gia sư chờ duyệt"].map(label => <article key={label}><h2>{label}</h2><strong aria-label="Chưa có dữ liệu">—</strong><p>Chưa có số liệu tổng hợp</p></article>)}
        </section>
        <section className={styles.card}><span className={styles.badge}>Đã kết nối chức năng</span><h2>Xét duyệt hồ sơ gia sư</h2><p>Xem hồ sơ, phê duyệt hoặc từ chối qua hệ thống xét duyệt hiện có.</p><button className="auth-primary" onClick={() => setSection("reviews")}>Mở danh sách chờ xét duyệt</button></section>
        <p className={styles.note}>Chưa có API thống kê tổng hợp. Các dấu gạch ngang không biểu thị số lượng bằng không.</p>
      </>}
      {section === "reviews" && <div className={`${styles.card} account-workspace`}><AdminApplications session={session} busy={busy} run={run} /></div>}
      {section === "categories" && <AdminCategories session={session} busy={busy} run={run} />}
      {section !== "overview" && section !== "reviews" && section !== "categories" && <section className={styles.card}>
        <span className={styles.badge}>Chưa khả dụng</span><h2>Chức năng đang được chuẩn bị</h2>
        <p>{section === "users" ? "Chưa kết nối API quản lý người dùng." : section === "courses" ? "Chưa kết nối API tổng quan khóa học. Gia sư sở hữu và tạo khóa học; quản trị viên không tạo khóa học." : "Chưa kết nối API thống kê giao dịch. Học phí được thanh toán trực tiếp cho gia sư; nền tảng không giữ tiền hoặc thực hiện chi trả."}</p>
        <p>Không hiển thị dữ liệu giả hoặc mô phỏng thao tác thành công.</p>
      </section>}
    </div>
  </main>;
}
