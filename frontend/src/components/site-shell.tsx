import Link from "next/link";
import { AccountControl } from "../features/auth/account-control";
import styles from "../app/landing.module.css";

export function Icon({ kind }: { kind: "book" | "person" | "video" | "check" }) {
  return <svg viewBox="0 0 48 48" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {kind === "book" && <><path d="M24 12C17 7 10 8 5 10v28c7-3 13-2 19 2 6-4 12-5 19-2V10c-5-2-12-3-19 2v28"/><path d="M11 17l7 1m-7 7 7 1m12-8 7-1m-7 9 7-1"/></>}
    {kind === "person" && <><circle cx="24" cy="15" r="8"/><path d="M9 41v-5a15 15 0 0 1 30 0v5M34 12l3 3 6-7"/></>}
    {kind === "video" && <><rect x="4" y="11" width="27" height="26" rx="5"/><path d="m31 19 12-6v22l-12-6"/><circle cx="17" cy="21" r="4"/><path d="M10 31a7 7 0 0 1 14 0"/></>}
    {kind === "check" && <><rect x="9" y="7" width="30" height="36" rx="4"/><path d="M18 7V4h12v3M16 24l5 5 11-12M17 36h14"/></>}
  </svg>;
}

function Brand() {
  return <Link className={styles.brand} href="/" aria-label="Nền tảng gia sư tiếng Anh — Trang chủ">
    <span className={styles.brandMark}><Icon kind="book" /></span>
    <span>TIẾNG ANH<span className={styles.brandSub}>CÙNG GIA SƯ</span></span>
  </Link>;
}

function Navigation({ home }: { home: boolean }) {
  return <><Link href={home ? "#trang-chu" : "/#trang-chu"} aria-current={home ? "page" : undefined}>Trang chủ</Link><Link href="/courses">Khóa học</Link><Link href={home ? "#gioi-thieu" : "/#gioi-thieu"}>Giới thiệu</Link><Link href={home ? "#lien-he" : "/#lien-he"}>Liên hệ</Link></>;
}

export function SiteHeader({ home = false }: { home?: boolean }) {
  return <header className={`${styles.landing} ${styles.header}`}><div className={styles.headerInner}>
      <Brand />
      <nav className={styles.desktopNav} aria-label="Điều hướng chính"><Navigation home={home} /></nav>
      <AccountControl />
      <details className={styles.mobileNav}><summary>Menu <span aria-hidden="true">☰</span></summary><nav aria-label="Điều hướng di động"><Navigation home={home} /></nav></details>
    </div></header>;
}

export function SiteFooter() {
  return <footer className={`${styles.landing} ${styles.footer}`} id="lien-he"><div className={styles.container}>
      <div className={styles.footerGrid}><div><h2>THÔNG TIN</h2><Brand /><p>Nền tảng kết nối học viên với gia sư tiếng Anh qua các khóa học trực tuyến.</p><Link className={styles.teacherLink} href="/become-a-teacher">Đăng ký giảng viên <span aria-hidden="true">↗</span></Link></div>
        <div><h2>Khám phá</h2><Link href="/#trang-chu">Trang chủ</Link><Link href="/courses">Khóa học</Link><Link href="/#gioi-thieu">Giới thiệu</Link></div>
        <div><h2>Tài khoản</h2><Link href="/login">Đăng nhập</Link><Link href="/register">Đăng ký học viên</Link></div>
        <div><h2>Liên hệ</h2><p>Thông tin liên hệ chính thức sẽ được cập nhật tại đây.</p></div>
      </div><div className={styles.footerBottom}>Nền tảng gia sư tiếng Anh<span>Kết nối để cùng tiến bộ.</span></div>
    </div></footer>;
}
