"use client";

import { useId, useState } from "react";

export function PasswordField({ confirm = false, registration = false, name, label: fieldLabel }: { confirm?: boolean; registration?: boolean; name?: string; label?: string }) {
  const [visible, setVisible] = useState(false);
  const id = useId();
  const label = fieldLabel ?? (confirm ? "Xác nhận mật khẩu" : "Mật khẩu");
  return <div className="password-field"><label htmlFor={id}>{label}</label><div className="password-input">
    <span aria-hidden="true">♙</span><input id={id} name={name ?? (confirm ? "confirmPassword" : "password")}
      type={visible ? "text" : "password"} autoComplete={registration ? "new-password" : "current-password"}
      required placeholder={confirm ? "Nhập lại mật khẩu" : "Nhập mật khẩu"} />
    <button type="button" aria-label={visible ? `Ẩn ${label.toLowerCase()}` : `Hiện ${label.toLowerCase()}`}
      aria-pressed={visible} onClick={() => setVisible(!visible)}>
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
        {visible ? <><path d="m3 3 18 18M10.6 10.6a2 2 0 0 0 2.8 2.8M9.8 5.2A10 10 0 0 1 12 5c7 0 10 7 10 7a17 17 0 0 1-3 4M6.2 6.2A18 18 0 0 0 2 12s3 7 10 7a11 11 0 0 0 5.8-1.8" /></> : <><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></>}
      </svg>
    </button>
  </div></div>;
}
