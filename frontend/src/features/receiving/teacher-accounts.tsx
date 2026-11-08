"use client";

import {useCallback, useEffect, useRef, useState, type FormEvent} from "react";
import {apiRequest} from "../../lib/api";
import type {Session} from "../auth/browser-api";
import banks from "./bank-options.json";
import {fieldsError, imageError, privateRequest, read, statuses, teacherPath, type Account, type Page} from "./receiving";
import {message, PrivateQr, useAccess, useLoad} from "./private-view";
import styles from "./receiving.module.css";

const bins=banks.map(bank=>bank.bin);
export function TeacherAccounts({session}:{session:Session}) {
  const token=session.accessToken, {denied,handle}=useAccess();
  const [version,setVersion]=useState(0),[show,setShow]=useState<string>(),[notice,setNotice]=useState("");
  const [page,setPage]=useState(0);
  const load=useCallback(async (signal:AbortSignal)=>{
    let items:Account[]=[], hasNext=false, pending=false, active:Account|undefined;
    // An older selected account can be on a different page from the newest submission.
    for(let index=0;;index++){
      const batch=await read<Page<Account>>(token,`${teacherPath}?page=${index}&size=20`,signal);
      if(batch.page!==index || !Array.isArray(batch.items) || (batch.hasNext && !batch.items.length)) throw new Error("Invalid page");
      if(index===page){items=batch.items;hasNext=batch.hasNext;}
      active ??= batch.items.find(a=>a.selectionStatus==="ACTIVE" && a.reviewStatus==="APPROVED");
      pending ||= batch.items.some(a=>a.reviewStatus==="PENDING");
      if(!batch.hasNext || (index>=page && active))return {items,hasNext,pending,active};
    }
  },[token,page]);
  const result=useLoad(load,handle,version);
  if(denied) return <p role="alert">Bạn không có quyền xem tài khoản nhận tiền.</p>;
  const active=result?.value?.active;
  return <section className={styles.panel}>
    {notice && <p role="status">{notice}</p>}
    <button onClick={()=>{setShow(undefined);setVersion(v=>v+1);}}>Cập nhật danh sách</button>
    {!result?<p role="status">Đang tải tài khoản…</p>:result.error?<p role="alert">{result.error}</p>:<>
      <h2>Tài khoản đang nhận tiền</h2>
      <p>{active?`${active.bankName ?? active.bankCode} · ${active.accountNumber}`:"Chưa có tài khoản đang nhận tiền."}</p>
      <h2>Hồ sơ đã gửi</h2>
      {!result.value?.items.length && <p>Chưa có hồ sơ tài khoản nhận tiền.</p>}
      {result.value?.items.map(a=><article key={a.id}><strong>{a.bankName ?? a.bankCode} · {a.accountNumber}</strong>
        <span>{statuses[a.reviewStatus]} · {a.selectionStatus==="ACTIVE"?"Đang sử dụng":"Không được chọn"}</span>
        <button aria-expanded={show===a.id} onClick={()=>setShow(show===a.id?undefined:a.id)}>Chi tiết hồ sơ {a.id}</button>
        {show===a.id && <OwnDetail key={a.id} id={a.id} token={token} handle={handle}/>}</article>)}
      <div className={styles.actions}><button disabled={!page} onClick={()=>{setShow(undefined);setPage(page-1);}}>Trang trước</button>
        <span>Trang {page+1}</span><button disabled={!result.value?.hasNext} onClick={()=>{setShow(undefined);setPage(page+1);}}>Trang sau</button></div>
    </>}
    <Submission token={token} disabled={!result?.value || result.value.pending} handle={handle}
      completed={()=>{setNotice("Hồ sơ đã được ghi nhận. Kiểm tra trạng thái trong danh sách.");setPage(0);setVersion(v=>v+1);}}/>
  </section>;
}
function OwnDetail({token,id,handle}:{token:string;id:string;handle:(e:unknown)=>void}) {
  const load=useCallback((signal:AbortSignal)=>read<Account>(token,`${teacherPath}/${id}`,signal),[token,id]);
  const result=useLoad(load,handle);
  if(!result) return <p role="status">Đang tải hồ sơ…</p>;
  if(!result.value) return <p role="alert">{result.error}</p>;
  const a=result.value;
  return <div><p>Chủ tài khoản: {a.holderName}</p><p>Số tài khoản: {a.accountNumber}</p>
    {a.hasQrImage && <PrivateQr token={token} path={`${teacherPath}/${id}/qr-image`} handle={handle}/>}</div>;
}
function Submission({token,disabled,handle,completed}:{token:string;disabled:boolean;handle:(e:unknown)=>void;completed:()=>void}) {
  const [busy,setBusy]=useState(false),[error,setError]=useState("");
  const flight=useRef(false),key=useRef("");
  const request=useRef<AbortController | null>(null);
  useEffect(()=>()=>request.current?.abort(),[]);
  async function submit(event:FormEvent<HTMLFormElement>){
    event.preventDefault();if(flight.current || disabled) return;
    const form=event.currentTarget, data=new FormData(form);
    const file=data.get("qrImage");
    const controller=new AbortController();request.current=controller;
    flight.current=true;setBusy(true);setError("");
    try {
      const invalid=fieldsError(String(data.get("bankBin")),bins,String(data.get("holderName")),String(data.get("accountNumber"))) ??
        await imageError(file instanceof File?file:null);
      if(controller.signal.aborted)return;
      if(invalid){setError(invalid);return;}
      key.current ||= crypto.randomUUID();
      const headers=new Headers(privateRequest(token).headers);headers.set("Idempotency-Key",key.current);
      const account=await apiRequest<Account>(teacherPath,{...privateRequest(token,controller.signal),method:"POST",headers,body:data});
      if(!account?.id) throw new Error("Invalid submission response");
      key.current="";form.reset();completed();
    } catch(e){if(!controller.signal.aborted){handle(e);setError(message(e));}}
    finally{flight.current=false;if(!controller.signal.aborted)setBusy(false);}
  }
  return <form onSubmit={submit} onChange={()=>{key.current="";setError("");}} autoComplete="off">
    <h2>Gửi tài khoản để xét duyệt</h2>
    <fieldset disabled={disabled||busy}><legend>Thông tin nhận tiền</legend>
      <label htmlFor="receiving-bank">Ngân hàng</label><select id="receiving-bank" name="bankBin" required defaultValue=""><option value="">Chọn ngân hàng</option>{banks.map(b=><option key={b.bin} value={b.bin}>{b.code} — {b.name}</option>)}</select>
      <label>Tên chủ tài khoản<input name="holderName" required maxLength={150}/></label>
      <label>Số tài khoản<input name="accountNumber" inputMode="numeric" pattern="[0-9]{1,34}" maxLength={34} required/></label>
      <label>Ảnh QR ngân hàng<input name="qrImage" type="file" accept="image/png,image/jpeg" required/></label>
      <small>PNG hoặc JPEG, tối đa 5 MiB, 4096 px mỗi chiều và 16 megapixel.</small>
      <button type="submit">{busy?"Đang gửi…":"Gửi hồ sơ"}</button></fieldset>
    {error && <p role="alert">{error}</p>}
  </form>;
}
