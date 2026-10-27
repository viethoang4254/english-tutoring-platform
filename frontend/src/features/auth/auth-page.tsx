"use client";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "./session-provider";
import Link from "next/link";
import { SiteHeader, SiteFooter } from "../../components/site-shell";
import { AccountWorkspace } from "./account-workspace";

export function AuthPage({ view, registered = false, reset = false }: { view: "login" | "student" | "register" | "verify"; registered?: boolean; reset?: boolean }) {
  const { session } = useSession();
  const router = useRouter();
  const redirectStudent = session?.account.role === "STUDENT" && (view === "login" || view === "student");
  const redirectAdmin = session?.account.role === "ADMIN" && view === "login";
  const redirectTeacher = session?.account.role === "TEACHER" && view === "login";
  useEffect(() => { if (redirectStudent) router.replace("/student/dashboard"); else if (redirectAdmin) router.replace("/admin/dashboard"); else if (redirectTeacher) router.replace("/teacher"); }, [redirectStudent, redirectAdmin, redirectTeacher, router]);
  if (redirectTeacher) return <main role="status">Đang mở Dashboard…</main>;
  if (redirectAdmin) return <main role="status">Đang mở trang quản trị…</main>;
  if (redirectStudent) return <main role="status">Đang mở bảng học tập…</main>;
  const title = view === "login" ? "Đăng nhập" : view === "student" ? "Đăng ký học viên" : view === "register" ? "Đăng ký gia sư" : "Xác minh email";
  return <div className="auth-page">
    <SiteHeader />
    <nav className="auth-breadcrumb" aria-label="Đường dẫn"><Link href="/">Trang chủ</Link><span aria-hidden="true">›</span><span aria-current="page">{title}</span></nav>
    <main>
      <AccountWorkspace dedicatedLogin={view === "login"} initialView={view} initialMessage={reset ? "Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại." : registered && view === "login" ? "Đăng ký thành công. Bạn có thể đăng nhập bằng email và mật khẩu ngay bây giờ." : registered ? "Tài khoản đã được tạo. Nhập mã xác minh được gửi qua email. Bạn chưa đăng nhập." : ""} />
    </main>
    <SiteFooter />
  </div>;
}
