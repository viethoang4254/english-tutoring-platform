"use client";
import { StudentProfile } from "../user/student-profile";

import { ChangePassword } from "./change-password";
import { useEffect, useRef, useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSession } from "./session-provider";
import { PasswordField } from "./password-field";
import { ApiRequestError } from "../../lib/api";
import { authMutation, authorized, safeMessage, signIn, validPassword, type Onboarding, type Session } from "./browser-api";
import { roleLabels, statusLabels, formatDate } from "../../lib/vi";
import { AdminApplications } from "../teacher/admin-applications";

type View = "login" | "register" | "student" | "verify";
const applicationFields = ["specialization", "teachingExperience", "introduction"] as const;
const labels = { specialization: "Chuyên môn", teachingExperience: "Kinh nghiệm giảng dạy", introduction: "Giới thiệu" };

function ApplicationFields() {
  return applicationFields.map(name => <label key={name}>{labels[name]}<textarea name={name} required rows={3} /></label>);
}

export function AccountWorkspace({ initialView = "login", modal = false, dedicatedLogin = false, initialMessage = "", onAuthenticated }: {
  initialView?: View; modal?: boolean; dedicatedLogin?: boolean; initialMessage?: string; onAuthenticated?: () => void;
}) {
  const { session, setSession, clearSession: clearSharedSession } = useSession();
  const router = useRouter();
  const [view, setView] = useState<View>(initialView);
  const [status, setStatus] = useState<Onboarding | null>(null);
  const [ready, setReady] = useState(false);
  const [busy, setBusy] = useState(false);
  const inFlight = useRef(false);
  const [message, setMessage] = useState(initialMessage);

  function clearSession() {
    clearSharedSession(); setStatus(null); setReady(false);
  }
  async function run(work: () => Promise<void>) {
    if (inFlight.current) return;
    inFlight.current = true; setBusy(true); setMessage("");
    try { await work(); }
    catch (error) {
      if (error instanceof ApiRequestError && error.status === 401) clearSession();
      setMessage(safeMessage(error));
    } finally { inFlight.current = false; setBusy(false); }
  }
  async function loadStatus(current: Session) {
    setReady(false);
    const latest = await authorized<Onboarding>(current.accessToken, "/teacher/onboarding");
    if (!latest || !/^[1-9][0-9]*$/.test(latest.applicationId) || !["PENDING", "APPROVED", "REJECTED"].includes(latest.status)) throw new Error("Invalid status response");
    setStatus(latest); setReady(true);
  }
  async function authenticate(body?: Record<string, string>) {
    clearSession();
    const current = await signIn(body);
    setSession(current);
    onAuthenticated?.();
  }
  useEffect(() => {
    if (modal || !session || session.account.role !== "TEACHER" || !session.account.emailVerified) return;
    let active = true;
    authorized<Onboarding>(session.accessToken, "/teacher/onboarding").then(latest => {
      if (!latest || !/^[1-9][0-9]*$/.test(latest.applicationId) || !["PENDING", "APPROVED", "REJECTED"].includes(latest.status)) throw new Error("Invalid status response");
      if (active) { setStatus(latest); setReady(true); }
    }).catch(error => {
      if (!active) return;
      if (error instanceof ApiRequestError && error.status === 401) clearSharedSession();
      setMessage(safeMessage(error));
    });
    return () => { active = false; };
  }, [session, modal, clearSharedSession]);
  function values(event: FormEvent<HTMLFormElement>): [HTMLFormElement, Record<string, string>] {
    event.preventDefault();
    const form = event.currentTarget;
    return [form, Object.fromEntries(Array.from(new FormData(form), ([key, value]) => [key, String(value)]))];
  }
  function validate(data: Record<string, string>) {
    if (Object.entries(data).some(([key, value]) => key !== "password" && (!value.trim() || value.includes("\0")))) throw new ApiRequestError(400, "INVALID_REQUEST", "Invalid input");
    if (data.password !== undefined && !validPassword(data.password)) throw new ApiRequestError(400, "INVALID_REQUEST", "Invalid input");
  }

  return <div className={`account-workspace auth-panel ${modal ? "auth-modal-panel" : ""}`} onInvalid={event => {
    const field = event.target;
    if (field instanceof HTMLInputElement || field instanceof HTMLTextAreaElement) {
      field.setCustomValidity(field.validity.valueMissing ? "Vui lòng điền thông tin này." :
        field.validity.typeMismatch ? "Vui lòng nhập địa chỉ email hợp lệ." : "Vui lòng kiểm tra thông tin đã nhập.");
    }
  }} onInput={event => {
    const field = event.target;
    if (field instanceof HTMLInputElement || field instanceof HTMLTextAreaElement) field.setCustomValidity("");
  }}>
    {!modal && !dedicatedLogin && initialView === "login" && <h1>Tài khoản</h1>}
    {!modal && !dedicatedLogin && session && initialView === "login" && <details key={session ? "signed-in" : "signed-out"} className="account-menu">
      <summary>Tài khoản</summary>
      {(
      <button disabled={busy} onClick={() => void run(async () => {
        clearSession();
        try { await authMutation("/auth/logout"); setMessage("Đã đăng xuất và kết thúc phiên đăng nhập hiện tại."); }
        catch (error) { setMessage(`${safeMessage(error)} Đã xóa phiên đăng nhập trên trình duyệt; chưa xác nhận được việc đăng xuất trên máy chủ.`); }
      })}>Đăng xuất</button>
      )}
    </details>}
    <p role="status" aria-live="polite">{message}</p>
    {!session ? <>
      {view === "login" && <section>
        <h2>Đăng nhập</h2>
        <form onSubmit={event => { const [form, data] = values(event); void run(async () => { validate(data); await authenticate(data); form.reset(); }); }}>
          <fieldset disabled={busy}><label>Email<input name="email" type="email" autoComplete="username" required /></label>
            <PasswordField />
            <Link className="recovery-note" href="/forgot-password">Quên mật khẩu?</Link>
            <button className="auth-primary" type="submit">{busy ? "Đang đăng nhập…" : "Đăng nhập"}</button></fieldset>
        </form>
        <p className="auth-switch">Bạn chưa có tài khoản? <Link href="/register">Đăng ký ngay</Link></p>
      </section>}
      {(view === "register" || view === "student") && <section key={view}><h2>Đăng ký {view === "student" ? "học viên" : "gia sư"}</h2>
        <form onSubmit={event => { const [form, data] = values(event); void run(async () => {
          const { confirmPassword, ...request } = data;
          if (view === "student" && request.password !== confirmPassword) { setMessage("Mật khẩu xác nhận không khớp."); return; }
          if (view === "student" && !/^[A-Za-z][A-Za-z0-9_]{2,29}$/.test(request.username.replace(/^ +| +$/g, ""))) {
            setMessage("Tên người dùng không hợp lệ: 3–30 ký tự, bắt đầu bằng chữ cái; chỉ gồm chữ không dấu, số hoặc dấu gạch dưới."); return;
          }
          if (view === "student" && !/^(?:0|\+84)[35789][0-9]{8}$/.test(request.phone.replace(/^ +| +$/g, ""))) {
            setMessage("Số điện thoại không hợp lệ. Ví dụ: 0912345678 hoặc +84912345678."); return;
          }
          if (!validPassword(request.password)) { setMessage("Vui lòng kiểm tra thông tin đã nhập: mật khẩu cần 8–64 ký tự, tối đa 72 byte UTF-8."); return; }
          validate(request);
          await authMutation(view === "student" ? "/auth/register/student" : "/auth/register/teacher", request);
          form.reset(); router.push(view === "student" ? "/login?registered=1" : "/verify-email?registered=1");
        }); }}><fieldset className="registration-grid" disabled={busy}>
          <label>Họ và tên<input name="fullName" autoComplete="name" placeholder="Nguyễn Văn An" required /></label>
          {view === "student" && <div><label>Tên người dùng<input name="username" autoComplete="username" required placeholder="nguyenvanan" /></label></div>}
          <label>Email<input name="email" type="email" autoComplete="email" placeholder="ban@example.com" required /></label>
          {view === "student" && <div><label>Số điện thoại<input name="phone" type="tel" autoComplete="tel" required placeholder="0912345678" /></label></div>}
          <PasswordField registration />
          {view === "student" && <PasswordField registration confirm />}
          {view === "register" && <ApplicationFields />}<button className="auth-primary" type="submit">Tạo tài khoản {view === "student" ? "học viên" : "gia sư"}</button>
        </fieldset></form><p className="auth-switch">Bạn đã có tài khoản? <Link href="/login">Đăng nhập</Link></p></section>}
      {view === "verify" && <section><h2>Xác minh địa chỉ email</h2>
        <form onSubmit={event => { const [form, data] = values(event); void run(async () => {
          validate(data); await authMutation("/auth/email-verification", data); form.reset();
          setView("login"); setMessage("Email đã được xác minh. Bạn có thể đăng nhập; hồ sơ gia sư vẫn cần được xét duyệt riêng.");
        }); }}><fieldset disabled={busy}><label>Mã xác minh<input name="token" autoComplete="off" required /></label><button type="submit">Xác minh mã</button></fieldset></form>
        <form onSubmit={event => { const [, data] = values(event); void run(async () => {
          validate(data); await authMutation("/auth/email-verification/resend", data);
          setMessage("Nếu đủ điều kiện, email xác minh đã được gửi. Vui lòng chờ ít nhất 60 giây trước khi yêu cầu gửi lại.");
        }); }}><fieldset disabled={busy}><label>Địa chỉ email nhận lại mã<input name="email" type="email" autoComplete="email" placeholder="ban@example.com" required /></label><button type="submit">Gửi lại email xác minh</button></fieldset></form>
      </section>}
    </> : <>
      <p>Đã đăng nhập với vai trò {roleLabels[session.account.role]}.</p>
      {session.account.role === "STUDENT" && <StudentProfile session={session} />}
      <ChangePassword session={session} />

      {session.account.role === "ADMIN" && session.account.emailVerified &&
        <AdminApplications session={session} busy={busy} run={run} />}
      {session.account.role === "TEACHER" && session.account.emailVerified ? <section>
        <h2>Hồ sơ đăng ký gia sư</h2>
        <button disabled={busy} onClick={() => void run(() => loadStatus(session))}>Cập nhật trạng thái hồ sơ</button>
        {!ready && <p>Vui lòng kiểm tra trạng thái mới nhất trước khi gửi. Chỉ có thể gửi lại sau khi kiểm tra thành công.</p>}
        {status && <>
          <p>Trạng thái hồ sơ: <strong>{statusLabels[status.status]}</strong>{!ready && " (lần cập nhật gần nhất)"}</p>
          <p>Ngày gửi: <time dateTime={status.submittedAt}>{formatDate(status.submittedAt)}</time></p>
          {status.status === "PENDING" && <p>Hồ sơ của bạn đang chờ xét duyệt.</p>}
          {status.status === "APPROVED" && <p>Hồ sơ của bạn đã được phê duyệt. Quyền giảng dạy vẫn phụ thuộc vào điều kiện hiện tại của tài khoản.</p>}
          {status.status === "REJECTED" && <>
            <p className="review-reason">Lý do từ chối: {status.rejectionReason ?? "Chưa có lý do."}</p>
            <form onSubmit={event => { const [form, data] = values(event);
              if (!ready) return;
              void run(async () => {
                validate(data); setReady(false);
                try {
                  const created = await authorized<Onboarding>(session.accessToken, `/teacher/applications/${status.applicationId}/resubmit`, data);
                  if (!created || created.status !== "PENDING") throw new Error("Invalid submission response");
                  setStatus(created); setReady(true); form.reset(); setMessage("Hồ sơ đã được gửi lại để xét duyệt.");
                } catch (error) {
                  // Never replay the mutation: reconcile against the authoritative latest snapshot.
                  if (error instanceof ApiRequestError && error.status === 401) throw error;
                  try { await loadStatus(session); } catch (lookupError) {
                    setReady(false);
                    if (lookupError instanceof ApiRequestError && lookupError.status === 401) { clearSession(); throw lookupError; }
                  }
                  setMessage(`${safeMessage(error)} Đã yêu cầu cập nhật trạng thái mới nhất; vui lòng kiểm tra trước khi thử lại.`);
                }
              });
            }}><fieldset disabled={busy || !ready}><legend>Gửi lại hồ sơ</legend><ApplicationFields /><button type="submit">Gửi lại hồ sơ</button></fieldset></form>
          </>}
        </>}
      </section> : <p>Chỉ tài khoản gia sư đã xác minh email mới có thể truy cập hồ sơ đăng ký gia sư.</p>}
    </>}
  </div>;
}
