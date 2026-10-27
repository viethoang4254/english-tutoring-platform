"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { AccountWorkspace } from "./account-workspace";
import { useSession } from "./session-provider";
import { authMutation, safeMessage } from "./browser-api";

export function AccountControl() {
  const { session, clearSession } = useSession();
  const dialog = useRef<HTMLDialogElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const pending = useRef(false);
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  useEffect(() => {
    if (!open) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    dialog.current?.showModal();
    return () => { document.body.style.overflow = previous; };
  }, [open]);
  function close() {
    dialog.current?.close();
    setOpen(false);
    trigger.current?.focus();
  }
  async function logout() {
    if (pending.current) return;
    pending.current = true; setBusy(true); setMessage("");
    clearSession();
    try { await authMutation("/auth/logout"); setMessage("Đã đăng xuất và kết thúc phiên đăng nhập hiện tại."); }
    catch (error) { setMessage(`${safeMessage(error)} Đã xóa phiên đăng nhập trên trình duyệt; chưa xác nhận được việc đăng xuất trên máy chủ.`); }
    finally { pending.current = false; setBusy(false); }
  }
  return <div className="header-account">
    {!session ? <button ref={trigger} className="account-trigger" aria-label="Tài khoản" disabled={busy} onClick={() => setOpen(true)}>
      <svg viewBox="0 0 24 24" width="23" height="23" fill="none" stroke="currentColor" strokeWidth="1.6" aria-hidden="true"><circle cx="12" cy="7" r="4"/><path d="M3 23v-3a9 9 0 0 1 18 0v3"/></svg><span>Tài khoản</span>
    </button> : <details className="header-account-menu"><summary>Tài khoản</summary><div>{session.account.role === "STUDENT" && <Link href="/student/dashboard">Bảng học tập</Link>}{session.account.role === "ADMIN" && <Link href="/admin/dashboard">Bảng quản trị</Link>}{session.account.role === "TEACHER" && <Link href="/teacher">Không gian gia sư</Link>}<Link href="/account">Tài khoản của tôi</Link><button disabled={busy} onClick={() => void logout()}>Đăng xuất</button></div></details>}
    {message && <p role="status" className="header-account-notice">{message}</p>}
    {open && <dialog className="login-dialog" ref={dialog} aria-label="Đăng nhập" onKeyDown={event => {
      if (event.key !== "Tab") return;
      const elements = Array.from(event.currentTarget.querySelectorAll<HTMLElement>(
        'button:not(:disabled), input:not(:disabled), a[href], summary, textarea:not(:disabled), [tabindex="0"]',
      )).filter(element => element.getClientRects().length > 0);
      const first = elements[0]; const last = elements[elements.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    }} onCancel={close} onClick={event => {
      if (event.target === event.currentTarget) {
        const rect = event.currentTarget.getBoundingClientRect();
        if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) close();
      }
    }}>
      <button className="dialog-close" aria-label="Đóng đăng nhập" onClick={close}>×</button>
      <AccountWorkspace modal onAuthenticated={close} />
    </dialog>}
  </div>;
}
