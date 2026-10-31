"use client";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { authorized, type Session } from "../auth/browser-api";
import { useSession } from "../auth/session-provider";
import { ApiRequestError } from "../../lib/api";
import { enrollmentError, remainingSeconds, statusLabel, type Enrollment, type EnrollmentPage } from "./enrollment";
import styles from "./enrollments.module.css";

const time = new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", dateStyle: "medium", timeStyle: "medium" });
export function StudentEnrollments({ session, enrollmentId }: { session: Session; enrollmentId: string | null }) {
  const { clearSession } = useSession();
  const [page, setPage] = useState(0), [attempt, setAttempt] = useState(0);
  const [data, setData] = useState<EnrollmentPage | null>(null);
  const [loading, setLoading] = useState(true), [busy, setBusy] = useState(false);
  const [error, setError] = useState(""), [success, setSuccess] = useState("");
  const [blocked, setBlocked] = useState(false), [now, setNow] = useState(0);
  const [cancel, setCancel] = useState<Enrollment | null>(null);
  const flight = useRef(false), alive = useRef(true);
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  useEffect(() => { const tick = () => setNow(Date.now()); tick(); const timer = setInterval(tick, 1000); return () => clearInterval(timer); }, []);
  useEffect(() => { if (cancel) dialog.current?.showModal(); else dialog.current?.close(); }, [cancel]);
  useEffect(() => {
    let current = true;
    async function load() {
      setLoading(true); setData(null); setError("");
      try {
        if (enrollmentId !== null && !/^[1-9][0-9]*$/.test(enrollmentId)) throw new ApiRequestError(404, "ENROLLMENT_NOT_FOUND", "Not found");
        const path = "/student/enrollments";
        const result = enrollmentId ? await authorized<Enrollment>(session.accessToken, `${path}/${enrollmentId}`)
          : await authorized<EnrollmentPage>(session.accessToken, `${path}?page=${page}&size=20`);
        if (!result) throw new Error("Missing enrollment response");
        if (current) {
          setData(enrollmentId ? { items: [result as Enrollment], page: 0, size: 20, hasNext: false } : result as EnrollmentPage);
          setBlocked(false);
        }
      } catch (failure) {
        if (current) { setError(enrollmentError(failure)); setBlocked(true); if (failure instanceof ApiRequestError && failure.status === 401) clearSession(); }
      } finally { if (current) setLoading(false); }
    }
    void load(); return () => { current = false; };
  }, [session.accessToken, enrollmentId, page, attempt, clearSession]);
  async function mutate(row: Enrollment, action: "renew" | "cancel") {
    if (flight.current || blocked || loading) return;
    flight.current = true; setBusy(true); setError(""); setSuccess(""); setCancel(null);
    try {
      const result = await authorized<Enrollment>(session.accessToken, `/student/enrollments/${row.id}/${action}`, undefined, "POST");
      if (!result || result.id !== row.id) throw new Error("Invalid mutation response");
      if (alive.current) { setSuccess(action === "renew" ? "Đã cập nhật thời hạn giữ chỗ." : "Đã hủy đăng ký."); setAttempt(value => value + 1); }
    } catch (failure) {
      if (alive.current) {
        setError(enrollmentError(failure)); setBlocked(true); setData(null);
        if (failure instanceof ApiRequestError && failure.status === 401) clearSession();
      }
    } finally { flight.current = false; if (alive.current) setBusy(false); }
  }
  return <section className={styles.workspace} aria-label="Quản lý đăng ký">
    <div className={styles.toolbar}>
      <Link href="/courses">Khám phá khóa học</Link>
      {enrollmentId && <Link href="/student/enrollments">Tất cả đăng ký</Link>}
      <button className="ui-button ui-button-secondary" disabled={loading || busy} onClick={() => setAttempt(value => value + 1)}>Tải lại đăng ký</button>
    </div>
    <p className={styles.note}>Thời gian hiển thị theo giờ Việt Nam (Asia/Ho_Chi_Minh). Đồng hồ đếm ngược chỉ để tham khảo; máy chủ quyết định thời hạn giữ chỗ.</p>
    {success && <p role="status">{success}</p>}
    {error && <p role="alert">{error}</p>}
    {blocked && <p>Vui lòng tải lại đăng ký thành công trước khi tiếp tục. Yêu cầu không được tự động gửi lại.</p>}
    {loading && <p role="status">Đang tải đăng ký…</p>}
    {!loading && data && <>
      {!data.items.length && <p>Bạn chưa có đăng ký nào trên trang này.</p>}
      <div className={styles.cards}>{data.items.map(row => {
        const seconds = remainingSeconds(row, now);
        const expired = row.reservationExpired || (now > 0 && seconds === 0);
        return <article key={row.id} className={styles.card}>
          <h2>{row.courseTitle}</h2><p>Mã khóa học: {row.courseId} · Mã đăng ký: {row.id}</p>
          <p>Trạng thái: <strong>{statusLabel[row.status]}</strong></p>
          {row.status === "PENDING" && <>
            <p>Thanh toán chưa được tích hợp. Giữ chỗ không có nghĩa là đã thanh toán và không mở nội dung khóa học.</p>
            <p>Hạn giữ chỗ: {row.seatReservedUntil ? <time dateTime={row.seatReservedUntil}>{time.format(new Date(row.seatReservedUntil))}</time> : "Không còn thời hạn hợp lệ"}</p>
            <p>{expired ? "Giữ chỗ đã hết hạn." : now > 0 ? `Còn ${Math.floor(seconds / 60)} phút ${seconds % 60} giây` : "Đang tính thời gian còn lại…"}</p>
          </>}
          {row.cancelledAt && <p>Đã hủy lúc: <time dateTime={row.cancelledAt}>{time.format(new Date(row.cancelledAt))}</time></p>}
          {row.cancellationReason && <p>Lý do: {row.cancellationReason}</p>}
          <div className={styles.actions}>
            {!enrollmentId && <Link href={`/student/enrollments?id=${row.id}`}>Xem đăng ký</Link>}
            {row.status === "PENDING" && <>
              {expired && <button className="ui-button ui-button-primary" disabled={busy || blocked} onClick={() => void mutate(row, "renew")}>Gia hạn giữ chỗ</button>}
              <button className="ui-button ui-button-secondary" disabled={busy || blocked} onClick={() => setCancel(row)}>Hủy đăng ký</button>
            </>}
          </div>
        </article>;
      })}</div>
      {!enrollmentId && <div className={styles.actions} aria-label="Phân trang đăng ký">
        <button disabled={page === 0 || busy} onClick={() => setPage(value => value - 1)}>Trang trước</button>
        <span>Trang {page + 1}</span><button disabled={!data.hasNext || busy} onClick={() => setPage(value => value + 1)}>Trang sau</button>
      </div>}
    </>}
    <dialog ref={dialog} className={styles.dialog} aria-labelledby="cancel-enrollment-title" onCancel={() => setCancel(null)}>
      <h2 id="cancel-enrollment-title">Hủy giữ chỗ?</h2><p>Bạn sẽ mất chỗ đã giữ. Chưa hỗ trợ đăng ký lại sau khi hủy.</p>
      <div className={styles.actions}><button autoFocus onClick={() => setCancel(null)}>Giữ đăng ký</button><button disabled={busy} onClick={() => { if (cancel) void mutate(cancel, "cancel"); }}>Xác nhận hủy</button></div>
    </dialog>
  </section>;
}
