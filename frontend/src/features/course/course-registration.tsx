"use client";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { AccountWorkspace } from "../auth/account-workspace";
import { authorized } from "../auth/browser-api";
import { useSession } from "../auth/session-provider";
import { enrollmentError, statusLabel, type Enrollment } from "../enrollment/enrollment";
import { ApiRequestError } from "../../lib/api";

export function CourseRegistration({ courseId }: { courseId: string }) {
  const { session } = useSession();
  return <Registration key={`${courseId}:${session?.accessToken ?? "anonymous"}`} courseId={courseId} />;
}

function Registration({ courseId }: { courseId: string }) {
  const { session, clearSession } = useSession();
  const [result, setResult] = useState<Enrollment | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false), [denied, setDenied] = useState(false);
  const flight = useRef(false), alive = useRef(true);
  const dialog = useRef<HTMLDialogElement>(null), login = useRef<HTMLButtonElement>(null);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  async function reserve() {
    if (!session || session.account.role !== "STUDENT" || flight.current || result || denied) return;
    flight.current = true; setBusy(true); setError("");
    try {
      const row = await authorized<Enrollment>(session.accessToken, `/student/courses/${encodeURIComponent(courseId)}/enrollment`, undefined, "POST");
      if (!row || row.courseId !== courseId || !/^[1-9][0-9]*$/.test(row.id) || !["PENDING", "ACTIVE"].includes(row.status)) throw new Error("Invalid enrollment response");
      if (alive.current) setResult(row);
    } catch (failure) {
      if (!alive.current) return;
      if (failure instanceof ApiRequestError && failure.status === 401) { clearSession(); return; }
      setDenied(failure instanceof ApiRequestError && failure.status === 403);
      setError(!(failure instanceof ApiRequestError) || failure.status >= 500
        ? "Chưa xác nhận được kết quả. Kiểm tra Đăng ký của tôi hoặc thử lại; yêu cầu trùng không kéo dài thời hạn giữ chỗ."
        : failure.code === "COURSE_FULL" ? "Khóa học đã hết chỗ."
        : failure.status === 404 ? "Khóa học không tồn tại hoặc không còn khả dụng."
        : enrollmentError(failure));
    } finally { flight.current = false; if (alive.current) setBusy(false); }
  }
  return <section aria-label="Đăng ký khóa học">
    {!session ? <>
      <p>Đăng nhập bằng tài khoản học viên để đăng ký. Nếu phiên đã hết hạn, vui lòng đăng nhập lại.</p>
      <button ref={login} className="ui-button ui-button-primary" onClick={() => dialog.current?.showModal()}>Đăng nhập để đăng ký</button>
      <dialog ref={dialog} className="login-dialog" aria-label="Đăng nhập" onClose={() => login.current?.focus()}>
        <button className="dialog-close" aria-label="Đóng đăng nhập" onClick={() => dialog.current?.close()}>×</button>
        <AccountWorkspace modal stayOnPage onAuthenticated={() => dialog.current?.close()} />
      </dialog>
    </> : session.account.role !== "STUDENT" ? <p>Chỉ tài khoản học viên được đăng ký khóa học.</p> : <>
      {error && <p role="alert">{error}</p>}
      {result ? <p role="status">{result.status === "PENDING" ? "Đã xác nhận đăng ký giữ chỗ: " : "Bạn đã có đăng ký: "}{statusLabel[result.status]}.</p>
        : <button className="ui-button ui-button-primary" disabled={busy || denied} onClick={() => void reserve()}>{busy ? "Đang gửi đăng ký…" : "Đăng ký giữ chỗ"}</button>}
      <Link href={result ? `/student/enrollments?id=${encodeURIComponent(result.id)}` : "/student/enrollments"}>Đăng ký của tôi</Link>
    </>}
    <p>Mở chi tiết đăng ký để thanh toán bằng VietQR. Giữ chỗ không có nghĩa là đã thanh toán và không mở nội dung khóa học.</p>
  </section>;
}
