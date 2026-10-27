"use client";
import {useRef,useState,type FormEvent} from "react";
import {PasswordField} from "./password-field";
import {authMutation,safeMessage,validPassword,type Session} from "./browser-api";
import {useSession} from "./session-provider";
import {ApiRequestError} from "../../lib/api";
export function ChangePassword({session}:{session:Session}) {
  const {clearSession}=useSession();const [busy,setBusy]=useState(false);const [message,setMessage]=useState("");const pending=useRef(false);
  async function submit(event:FormEvent<HTMLFormElement>) {
    event.preventDefault();if(pending.current)return;
    const form=event.currentTarget;const data=new FormData(form);const currentPassword=String(data.get("currentPassword"));const newPassword=String(data.get("newPassword"));
    if(!validPassword(newPassword)){setMessage("Mật khẩu mới cần 8–64 ký tự, tối đa 72 byte UTF-8.");return;}
    if(newPassword===currentPassword){setMessage("Mật khẩu mới phải khác mật khẩu hiện tại.");return;}
    if(newPassword!==data.get("confirmPassword")){setMessage("Mật khẩu xác nhận không khớp.");return;}
    pending.current=true;setBusy(true);setMessage("");
    try {await authMutation("/auth/password/change",{currentPassword,newPassword},session.accessToken);form.reset();setMessage("Đổi mật khẩu thành công. Các phiên đăng nhập khác đã được đăng xuất.");}
    catch(error){if(error instanceof ApiRequestError && error.status===401)clearSession();setMessage(error instanceof ApiRequestError && error.status===403?"Không thể đổi mật khẩu. Vui lòng kiểm tra mật khẩu hiện tại và thử lại.":safeMessage(error));}
    finally{pending.current=false;setBusy(false);}
  }
  return <details id="change-password"><summary>Đổi mật khẩu</summary>{message && <p role="status" aria-live="polite">{message}</p>}
    <form onSubmit={submit} onInvalid={event=>{if(event.target instanceof HTMLInputElement)event.target.setCustomValidity("Vui lòng điền thông tin này.");}} onInput={event=>{if(event.target instanceof HTMLInputElement)event.target.setCustomValidity("");}}><fieldset disabled={busy}>
      <PasswordField name="currentPassword" label="Mật khẩu hiện tại" />
      <PasswordField registration name="newPassword" label="Mật khẩu mới" />
      <PasswordField registration confirm />
      <button className="ui-button ui-button-primary" type="submit">{busy?"Đang cập nhật…":"Lưu mật khẩu"}</button>
    </fieldset></form></details>;
}
