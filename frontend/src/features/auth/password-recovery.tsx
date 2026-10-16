"use client";

import Link from "next/link";
import { useRef, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { SiteHeader, SiteFooter } from "../../components/site-shell";
import { PasswordField } from "./password-field";
import { authMutation, safeMessage, validPassword } from "./browser-api";
import { useSession } from "./session-provider";

export function PasswordRecovery({ reset = false }: { reset?: boolean }) {
  const router = useRouter(); const { clearSession } = useSession();
  const [busy, setBusy] = useState(false); const [message, setMessage] = useState("");
  const [sent, setSent] = useState(false); const inFlight = useRef(false);
  const title = reset ? "Đặt lại mật khẩu" : "Quên mật khẩu";
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (inFlight.current) return;
    const form = event.currentTarget; const data = new FormData(form);
    if (reset && !validPassword(String(data.get("password")))) { setMessage("Mật khẩu cần 8–64 ký tự, tối đa 72 byte UTF-8."); return; }
    if (reset && data.get("password") !== data.get("confirmPassword")) { setMessage("Mật khẩu xác nhận không khớp."); return; }
    inFlight.current = true; setBusy(true); setMessage("");
    try {
      await authMutation(reset ? "/auth/password/reset" : "/auth/password/forgot", reset
        ? { email: String(data.get("email")), code: String(data.get("code")), password: String(data.get("password")) }
        : { email: String(data.get("email")) });
      form.reset();
      if (reset) { clearSession(); router.push("/login?reset=1"); }
      else { setSent(true); setMessage("Nếu tài khoản đủ điều kiện, bạn có thể nhận được email hướng dẫn. Vui lòng kiểm tra hộp thư và thư rác."); }
    } catch (error) { setMessage(safeMessage(error)); }
    finally { inFlight.current = false; setBusy(false); }
  }
  return <div className="auth-page"><SiteHeader />
    <nav className="auth-breadcrumb" aria-label="Đường dẫn"><Link href="/">Trang chủ</Link><span aria-hidden="true">›</span><span aria-current="page">{title}</span></nav>
    <main><section className="account-workspace auth-panel"><h1>{title}</h1>
      <p role="status" aria-live="polite">{message}</p>
      <form onSubmit={submit} onInvalid={event => { const field = event.target; if (field instanceof HTMLInputElement) field.setCustomValidity(field.validity.valueMissing ? "Vui lòng điền thông tin này." : "Vui lòng kiểm tra thông tin đã nhập."); }} onInput={event => { if (event.target instanceof HTMLInputElement) event.target.setCustomValidity(""); }}>
        <fieldset disabled={busy}>
          <label>Email<input name="email" type="email" autoComplete="email" required placeholder="ban@example.com" /></label>
          {reset ? <><label>Mã đặt lại mật khẩu<input name="code" inputMode="numeric" pattern="[0-9]{6}" maxLength={6} autoComplete="one-time-code" required placeholder="Nhập mã gồm 6 chữ số" /></label><PasswordField registration /><PasswordField registration confirm /></> : null}
          <button className="auth-primary" type="submit">{busy ? "Đang xử lý…" : reset ? "Lưu mật khẩu mới" : "Gửi hướng dẫn"}</button>
        </fieldset>
      </form>
      {!reset && <p><Link href="/reset-password">{sent ? "Nhập mã và đặt lại mật khẩu" : "Tôi đã có mã đặt lại mật khẩu"}</Link></p>}
      <p className="auth-switch"><Link href="/login">Quay lại đăng nhập</Link></p>
    </section></main><SiteFooter /></div>;
}
