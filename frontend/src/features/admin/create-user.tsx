"use client";
import { useEffect, useRef, useState, type ReactNode, type FormEvent } from "react";
import { authorized, safeMessage, validPassword, type Session } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import { useSession } from "../auth/session-provider";
import ui from "./admin-users.module.css";

export function UserDialog({title, close, busy = false, children}:{
  title:string; close:()=>void; busy?:boolean; children:ReactNode
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current!;
    dialog.showModal();
    return () => dialog.close();
  }, []);
  return <dialog className={ui.dialog} ref={ref} aria-label={title}
    onCancel={event => {event.preventDefault(); if (!busy) close();}}>
    <h2>{title}</h2>{children}
  </dialog>;
}

export function CreateUser({session, close, created}:{session:Session;close:()=>void;created:(message:string)=>void}) {
  const [role,setRole] = useState("STUDENT");
  const [busy,setBusy] = useState(false);
  const [error,setError] = useState("");
  const pending = useRef(false);
  const {clearSession} = useSession();
  async function submit(event:FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending.current) return;
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form)) as Record<string,string>;
    const password = form.elements.namedItem("password") as HTMLInputElement;
    const confirm = form.elements.namedItem("confirmPassword") as HTMLInputElement;
    password.setCustomValidity(validPassword(data.password) ? "" : "Mật khẩu cần 8–64 ký tự, tối đa 72 byte UTF-8.");
    confirm.setCustomValidity(data.password === data.confirmPassword ? "" : "Mật khẩu xác nhận không khớp.");
    if (!form.reportValidity()) return;
    pending.current = true; setBusy(true); setError("");
    try {
      const response = await authorized<{verificationRequired:boolean}>(session.accessToken,"/admin/users",data);
      if (!response || typeof response.verificationRequired !== "boolean") throw new Error("Invalid response");
      created(data.role === "TEACHER" ? "Đã tạo tài khoản gia sư. Hồ sơ đang chờ xét duyệt." : "Đã tạo tài khoản học viên.");
    } catch (e) {
      if (e instanceof ApiRequestError && e.status === 401) clearSession();
      setError(safeMessage(e));
    } finally {pending.current=false;setBusy(false);}
  }
  return <UserDialog title="Thêm người dùng" close={close} busy={busy}>
    <form method="post" className={ui.form} onSubmit={submit} onInput={e=>{
      if(e.target instanceof HTMLInputElement)e.target.setCustomValidity("");
      const confirm=e.currentTarget.elements.namedItem("confirmPassword") as HTMLInputElement;
      confirm.setCustomValidity("");
    }}>
      <fieldset disabled={busy}>
        <label>Họ và tên<input name="fullName" required pattern=".*\S.*" /></label>
        <label>Email<input type="email" name="email" required /></label>
        <label>Vai trò<select name="role" value={role} onChange={e=>setRole(e.target.value)}><option value="STUDENT">Học viên</option><option value="TEACHER">Gia sư</option></select></label>
        {role==="STUDENT"?<>
          <label>Tên người dùng<input name="username" required pattern="[A-Za-z][A-Za-z0-9_]{2,29}" title="3–30 ký tự, bắt đầu bằng chữ cái" /></label>
          <label>Số điện thoại<input type="tel" name="phone" required pattern="(0|\+84)[35789][0-9]{8}" /></label>
        </>:<>
          <label>Chuyên môn<input name="specialization" required /></label>
          <label>Kinh nghiệm giảng dạy<textarea name="teachingExperience" required /></label>
          <label>Giới thiệu<textarea name="introduction" required /></label>
        </>}
        <label>Mật khẩu<input type="password" autoComplete="new-password" name="password" required /></label>
        <label>Xác nhận mật khẩu<input type="password" autoComplete="new-password" name="confirmPassword" required /></label>
        {error&&<p role="alert">{error}</p>}
        <footer><button type="button" onClick={close}>Hủy</button><button className="auth-primary" type="submit">{busy?"Đang tạo…":"Tạo tài khoản"}</button></footer>
      </fieldset>
    </form>
  </UserDialog>;
}
