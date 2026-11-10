"use client";
import {useCallback, useEffect, useRef, useState} from "react";
import {authorized, type Session} from "../auth/browser-api";
import {useSession} from "../auth/session-provider";
import {ApiRequestError} from "../../lib/api";
import {PrivateQr} from "../receiving/private-view";
import {tuitionLabel} from "../course/public-courses";
import {enrollmentError, type Enrollment} from "./enrollment";

const statuses = {PENDING:"Chờ xác nhận", CONFIRMED:"Đã xác nhận", EXPIRED:"Đã hết hạn", CANCELLED:"Đã hủy"};
type Payment = {id:string; enrollmentId:string; status:keyof typeof statuses; finalAmount:string; currency:string;
  bankName:string; accountNumber:string; accountHolderName:string; transferReference:string; expiresAt:string};

function checked(value:Payment|undefined, enrollment:string){
  if (!value || value.enrollmentId!==enrollment || !/^[1-9]\d*$/.test(value.id) ||
      !Object.hasOwn(statuses,value.status) || value.currency!=="VND" || !/^\d+(\.\d+)?$/.test(value.finalAmount))
    throw new Error("Invalid payment response");
  return value;
}

export function EnrollmentPayment({session,row,expired,now}:{session:Session;row:Enrollment;expired:boolean;now:number}) {
  const {clearSession}=useSession();
  const [revision,reload]=useState(0), [busy,setBusy]=useState(true), [error,setError]=useState("");
  const [payment,setPayment]=useState<Payment|null>(null), [meet,setMeet]=useState("");
  const pending=useRef(false), alive=useRef(false);
  const root=`/student/enrollments/${row.id}`;
  const handle=useCallback((failure:unknown)=>{
    if(!alive.current)return;
    setPayment(null);setMeet("");setError(enrollmentError(failure));
    if(failure instanceof ApiRequestError && failure.status===401)clearSession();
  },[clearSession]);
  useEffect(()=>{alive.current=true;return()=>{alive.current=false;};},[]);
  useEffect(()=>{
    let active=true;
    async function load(){
      const latest=await authorized<{id:string}>(session.accessToken,root+"/payments/latest");
      if(!active || !latest)return;
      if(!/^[1-9]\d*$/.test(latest.id))throw new Error("Invalid payment ID");
      const data=checked(await authorized<Payment>(session.accessToken,`/student/payments/${latest.id}`),row.id);
      if(data.id!==latest.id)throw new Error("Invalid payment ID");
      if(active)setPayment(data);
    }
    load().catch(e=>{if(active)handle(e);}).finally(()=>{if(active)setBusy(false);});
    return ()=>{active=false;};
  },[root,session.accessToken,row.id,revision,handle]);
  async function act(access:boolean){
    if(pending.current||busy)return;
    pending.current=true;setBusy(true);setError("");
    try {
      if(access){
        const data=await authorized<{meetUrl:string}>(session.accessToken,root+"/course-access");
        const url=new URL(data?.meetUrl??"");
        if(url.origin!=="https://meet.google.com"||url.password||url.username)throw new Error("Invalid Meet URL");
        if(alive.current)setMeet(url.href);
      }else{
        const data=checked(await authorized<Payment>(session.accessToken,root+"/payments",undefined,"POST"),row.id);
        if(alive.current)setPayment(data);
      }
    }catch(e){handle(e);}finally{pending.current=false;if(alive.current)setBusy(false);}
  }
  const payable=row.status==="PENDING"&&!expired;
  const validQr=payable && payment?.status==="PENDING" && !!payment.accountNumber && !!payment.bankName && !!payment.transferReference && now>0 && Date.parse(payment.expiresAt)>now;
  return <section aria-label="Thanh toán khóa học"><h3>Thanh toán khóa học</h3>
    {error&&<p role="alert">{error}</p>}
    {busy&&<p role="status">Đang tải thanh toán…</p>}
    {payment&&<p>Trạng thái thanh toán: <strong>{statuses[payment.status]}</strong></p>}
    {validQr&&payment&&<><p>Số tiền: {tuitionLabel(payment.finalAmount)}</p><p>{payment.bankName} · {payment.accountNumber} · {payment.accountHolderName}</p>
      <p>Nội dung chuyển khoản: {payment.transferReference}</p>
      <PrivateQr key={payment.id} token={session.accessToken} path={`/student/payments/${payment.id}/qr-image`} handle={handle} label="VietQR thanh toán khóa học"/>
      <p>Chuyển đúng số tiền và nội dung. Gia sư xác nhận sau khi kiểm tra giao dịch.</p></>}
    {payable&&!error&&(!payment||payment.status==="EXPIRED"||payment.status==="CANCELLED")&&<button disabled={busy} onClick={()=>void act(false)}>Tạo yêu cầu thanh toán</button>}
    <button disabled={busy} onClick={()=>{setBusy(true);setError("");setPayment(null);setMeet("");reload(n=>n+1);}}>Tải lại thanh toán</button>
    {payment?.status==="CONFIRMED"&&row.status!=="ACTIVE"&&<p>Tải lại đăng ký để xem quyền vào lớp.</p>}
    {row.status==="ACTIVE"&&<button disabled={busy} onClick={()=>void act(true)}>Mở khóa học</button>}
    {meet&&<a href={meet} target="_blank" rel="noopener noreferrer">Vào lớp qua Google Meet</a>}
  </section>;
}
