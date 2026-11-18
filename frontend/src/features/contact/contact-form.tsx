"use client";
import { useState, type FormEvent } from "react";
import { authMutation } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import styles from "../../app/public-pages.module.css";

export function ContactForm(){
  const [busy,setBusy]=useState(false),[error,setError]=useState(""),[success,setSuccess]=useState(false);
  async function submit(event:FormEvent<HTMLFormElement>){
    event.preventDefault();if(busy)return;
    const form=event.currentTarget,data=new FormData(form);
    const body=Object.fromEntries(["fullName","email","phone","subject","message"].map(key=>[key,String(data.get(key)??"").trim()]));
    setError("");setSuccess(false);
    if(["fullName","email","subject","message"].some(key=>!body[key])){setError("Vui lòng điền đầy đủ thông tin.");return;}
    setBusy(true);
    try{
      const result=await authMutation<{status:string}>("/auth/contact",body);
      if(result?.status!=="ACCEPTED_BY_MAIL_SERVER")throw new Error("Unconfirmed contact result");
      setSuccess(true);form.reset();
    }catch(failure){
      setError(failure instanceof ApiRequestError && failure.code==="CONTACT_RATE_LIMITED" ? "Đã đạt giới hạn gửi. Vui lòng thử lại sau một giờ." :
        failure instanceof ApiRequestError && failure.code==="CONTACT_UNAVAILABLE" ? "Kênh liên hệ chưa được cấu hình hoặc tạm thời không khả dụng. Chưa gửi lời nhắn." :
        failure instanceof ApiRequestError && failure.status===400 ? "Thông tin chưa hợp lệ. Vui lòng kiểm tra độ dài và địa chỉ email." :
        "Chưa xác nhận được kết quả gửi. Hệ thống không tự động gửi lại để tránh trùng lời nhắn.");
    }finally{setBusy(false);}
  }
  return <form className={styles.form} onSubmit={submit} aria-busy={busy}>
    <div className={styles.formRow}><label>Họ và tên <span aria-hidden="true">*</span><input aria-label="Họ và tên" name="fullName" autoComplete="name" required maxLength={120}/></label>
    <label>Email <span aria-hidden="true">*</span><input aria-label="Email" name="email" type="email" autoComplete="email" required maxLength={254}/></label></div>
    <label>Số điện thoại (không bắt buộc)<input name="phone" type="tel" autoComplete="tel" maxLength={24} pattern="[+]?[0-9 ]{7,23}"/></label>
    <label>Tiêu đề <span aria-hidden="true">*</span><input aria-label="Tiêu đề" name="subject" required maxLength={160}/></label>
    <label>Nội dung <span aria-hidden="true">*</span><textarea aria-label="Nội dung" name="message" rows={7} required maxLength={5000}/></label>
    <p>Thông tin được dùng để tiếp nhận và phản hồi yêu cầu của bạn.</p>
    {error&&<p role="alert">{error}</p>}{success&&<p role="status">Máy chủ thư đã tiếp nhận lời nhắn. Điều này chưa xác nhận thư đã đến hộp thư hỗ trợ.</p>}
    <button type="submit" disabled={busy}>{busy?"Đang gửi…":"Gửi tin nhắn"}</button>
  </form>;
}
