"use client";

import Link from "next/link";
import {useCallback, useEffect, useRef, useState} from "react";
import {authorized, type Session} from "../auth/browser-api";
import {adminPath, read, reasonError, statuses, type Page, type QueueItem, type Review} from "./receiving";
import {message, PrivateQr, useAccess, useLoad} from "./private-view";
import styles from "./receiving.module.css";

const queueUrl="/admin/dashboard?section=receiving";
export function AdminReviews({session,id}:{session:Session;id?:string}) {
  const {denied,handle}=useAccess();
  if(denied) return <p role="alert">Bạn không có quyền xét duyệt tài khoản nhận tiền.</p>;
  if(id && !/^[1-9][0-9]*$/.test(id)) return <p role="alert">Mã hồ sơ không hợp lệ. <Link href={queueUrl}>Danh sách chờ</Link></p>;
  return id?<ReviewDetail key={id} token={session.accessToken} id={id} handle={handle}/>:<Queue token={session.accessToken} handle={handle}/>;
}
function Queue({token,handle}:{token:string;handle:(e:unknown)=>void}) {
  const [page,setPage]=useState(0),[version,setVersion]=useState(0);
  const load=useCallback((signal:AbortSignal)=>{
    return read<Page<QueueItem>>(token,`${adminPath}/pending?page=${page}&size=20`,signal);
  },[token,page]);
  const result=useLoad(load,handle,version);
  return <section className={styles.panel}><h2>Hồ sơ chờ xét duyệt</h2>
    <button onClick={()=>setVersion(v=>v+1)}>Cập nhật danh sách</button>
    {!result?<p role="status">Đang tải danh sách…</p>:result.error?<p role="alert">{result.error}</p>:<>
      {!result.value?.items.length && <p>Không có hồ sơ chờ trên trang này.</p>}
      {result.value?.items.map(a=><article key={a.id}><strong>{a.bankName}</strong><span>Gia sư {a.teacherId}</span>
        <time dateTime={a.submittedAt}>{new Date(a.submittedAt).toLocaleString("vi-VN",{timeZone:"Asia/Ho_Chi_Minh"})} (GMT+7)</time>
        <Link href={`${queueUrl}&id=${a.id}`}>Xem hồ sơ {a.id}</Link></article>)}
    </>}
    <div className={styles.actions}><button disabled={!page} onClick={()=>setPage(page-1)}>Trang trước</button>
      <span>Trang {page+1}</span><button disabled={!result?.value?.hasNext} onClick={()=>setPage(page+1)}>Trang sau</button></div>
  </section>;
}
function ReviewDetail({token,id,handle}:{token:string;id:string;handle:(e:unknown)=>void}){
  const [version,setVersion]=useState(0),[mode,setMode]=useState<"approve"|"reject">(),[reason,setReason]=useState("");
  const [error,setError]=useState(""),[notice,setNotice]=useState(""),[busy,setBusy]=useState(false);
  const flight=useRef(false);
  const approveButton=useRef<HTMLButtonElement>(null),rejectButton=useRef<HTMLButtonElement>(null);
  const confirmButton=useRef<HTMLButtonElement>(null),reasonInput=useRef<HTMLTextAreaElement>(null);
  const restore=useRef<"approve"|"reject"|undefined>(undefined);
  useEffect(()=>{
    const target=mode==="reject"?reasonInput.current:mode==="approve"?confirmButton.current:
      restore.current==="approve"?approveButton.current:restore.current==="reject"?rejectButton.current:null;
    if(target?.isConnected && !target.disabled)target.focus();
    restore.current=undefined;
  },[mode]);
  const path=`${adminPath}/${id}`;
  const load=useCallback((signal:AbortSignal)=>read<Review>(token,path,signal),[token,path]);
  const result=useLoad(load,handle,version), detail=result?.value;
  async function decide(){
    if(flight.current || !mode || detail?.account.reviewStatus!=="PENDING")return;
    const invalid=mode==="reject" && reasonError(reason);
    if(invalid){setError(invalid);return;}
    flight.current=true;setBusy(true);setError("");setNotice("");
    try {
      await authorized(token,`${path}/${mode}`,mode==="reject"?{reason:reason.trim()}:undefined,"POST");
      setNotice("Đã ghi nhận quyết định xét duyệt.");
    } catch(e){handle(e);setError(message(e));}
    finally {
      setMode(undefined);setReason("");setVersion(v=>v+1);flight.current=false;setBusy(false);
    }
  }
  return <section className={styles.panel}><Link href={queueUrl}>Danh sách chờ</Link><h2>Hồ sơ nhận tiền {id}</h2>
    {error && <p role="alert">{error}</p>}{notice && <p role="status">{notice}</p>}
    {!result?<p role="status">Đang tải hồ sơ…</p>:!detail?<><p role="alert">{result.error}</p><button onClick={()=>setVersion(v=>v+1)}>Thử lại</button></>:<article>
      <p>Gia sư {detail.teacherId}</p><strong>{detail.account.bankName ?? detail.account.bankCode}</strong>
      <p>Chủ tài khoản: {detail.account.holderName}</p><p>Số tài khoản: {detail.account.accountNumber}</p>
      <p>{statuses[detail.account.reviewStatus]} · {detail.account.selectionStatus==="ACTIVE"?"Đang sử dụng":"Không được chọn"}</p>
      {detail.rejectionReason && <p>Lý do từ chối: {detail.rejectionReason}</p>}
      {detail.reviewedBy && detail.reviewedAt && <p>Quản trị viên {detail.reviewedBy} · {new Date(detail.reviewedAt).toLocaleString("vi-VN",{timeZone:"Asia/Ho_Chi_Minh"})} (GMT+7)</p>}
      {detail.account.hasQrImage && <PrivateQr token={token} path={`${path}/qr-image`} handle={handle}/>}
      {detail.account.reviewStatus==="PENDING" && (mode?<fieldset disabled={busy}>
        <legend>{mode==="approve"?"Xác nhận phê duyệt":"Từ chối hồ sơ"}</legend>
        {mode==="approve"?<p>Tài khoản này sẽ được chọn để nhận tiền và thay thế tài khoản đang sử dụng của gia sư. Việc xét duyệt không xác minh quyền sở hữu tại ngân hàng.</p>
          :<label>Lý do từ chối<textarea ref={reasonInput} required maxLength={2000} value={reason} onChange={e=>setReason(e.target.value)}/></label>}
        <div className={styles.actions}><button ref={confirmButton} onClick={()=>void decide()}>{mode==="approve"?"Xác nhận phê duyệt":"Xác nhận từ chối"}</button>
        <button onClick={()=>{restore.current=mode;setMode(undefined);setReason("");setError("");}}>Quay lại</button></div></fieldset>:<div className={styles.actions}>
        <button ref={approveButton} onClick={()=>setMode("approve")}>Phê duyệt</button><button ref={rejectButton} onClick={()=>setMode("reject")}>Từ chối</button></div>)}
    </article>}
  </section>;
}
