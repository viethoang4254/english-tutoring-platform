"use client";
import {reviewedAmount,rememberReview} from "./payment-review";
import styles from "./enrollments.module.css";
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

export function EnrollmentPayment({session,row,expired,now,onRefresh}:{session:Session;row:Enrollment;expired:boolean;now:number;onRefresh:()=>void}) {
  const {clearSession}=useSession();
  const [busy,setBusy]=useState(true), [error,setError]=useState("");
  const [accepted,setAccepted]=useState<{id:string;amount:string}|null>(null);
  const [copied,setCopied]=useState("");
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
  },[root,session.accessToken,row.id,handle]);
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
  async function copy(label:string,value:string) {
    try {await navigator.clipboard.writeText(value);if(alive.current)setCopied(`Đã sao chép ${label}.`);}
    catch {if(alive.current)setCopied("Không thể sao chép tự động. Vui lòng chọn và sao chép thông tin trên màn hình.");}
  }
  const payable=row.status==="PENDING"&&!expired;
  const validQr=payable && payment?.status==="PENDING" && !!payment.accountNumber && !!payment.bankName && !!payment.transferReference && now>0 && Date.parse(payment.expiresAt)>now;
  const reviewed=payment && reviewedAmount(session.account.id,row.id,payment.id,payment.expiresAt);
  const acknowledged=payment && (reviewed===payment.finalAmount ||
    (accepted?.id===payment.id && accepted.amount===payment.finalAmount));
  return <section aria-label="Thanh toán khóa học"><h3>Thanh toán khóa học</h3>
    {error&&<p role="alert">{error}</p>}
    {busy&&<p role="status">Đang tải thanh toán…</p>}
    {payment&&<><p>Trạng thái thanh toán: <strong>{statuses[payment.status]}</strong></p>
      <p>Hết hạn thanh toán: <time dateTime={payment.expiresAt}>{new Intl.DateTimeFormat("vi-VN",{timeZone:"Asia/Ho_Chi_Minh",dateStyle:"medium",timeStyle:"short"}).format(new Date(payment.expiresAt))}</time> (giờ Việt Nam)</p>
      {payment.status==="PENDING"&&<p>Đang chờ gia sư kiểm tra giao dịch. Quét mã hoặc thông báo đã chuyển tiền không kích hoạt đăng ký.</p>}
      {payment.status==="EXPIRED"&&<p>Yêu cầu đã hết hạn. Nếu đã chuyển tiền, liên hệ gia sư để kiểm tra trước khi tạo yêu cầu mới.</p>}
      {payment.status==="CANCELLED"&&<p>Yêu cầu thanh toán đã hủy. Không chuyển tiền theo mã cũ.</p>}
      {payment.status==="CONFIRMED"&&row.status==="ACTIVE"&&<p role="status">Thanh toán đã được xác nhận. Đăng ký đang hoạt động.</p>}
    </>}
    {copied&&<p role="status">{copied}</p>}
    {validQr&&payment&&!acknowledged&&<div>
      <p role="status">{reviewed ? "Học phí đã thay đổi." : "Vui lòng kiểm tra số tiền trước khi chuyển khoản."} Số tiền thanh toán: <strong>{tuitionLabel(payment.finalAmount)}</strong>.</p>
      <button onClick={()=>{
        rememberReview(session.account.id,row.id,{amount:payment.finalAmount,paymentId:payment.id,deadline:null});
        setAccepted({id:payment.id,amount:payment.finalAmount});
      }}>Xác nhận số tiền thanh toán</button>
    </div>}
    {validQr&&payment&&acknowledged&&<><p>Số tiền: {tuitionLabel(payment.finalAmount)}</p><p>{payment.bankName} · {payment.accountNumber} · {payment.accountHolderName}</p>
      <p>Nội dung chuyển khoản: {payment.transferReference}</p>
      <div className={styles.actions}>
        <button onClick={()=>void copy("số tài khoản",payment.accountNumber)}>Sao chép số tài khoản</button>
        <button onClick={()=>void copy("số tiền",payment.finalAmount)}>Sao chép số tiền</button>
        <button onClick={()=>void copy("nội dung chuyển khoản",payment.transferReference)}>Sao chép nội dung</button>
      </div>
      <PrivateQr key={payment.id} token={session.accessToken} path={`/student/payments/${payment.id}/qr-image`} handle={handle} label="VietQR thanh toán khóa học"/>
      <p>Chuyển đúng số tiền và nội dung. Gia sư xác nhận sau khi kiểm tra giao dịch.</p></>}
    {payable&&!error&&(!payment||payment.status==="EXPIRED"||payment.status==="CANCELLED")&&<button disabled={busy} onClick={()=>void act(false)}>Tạo yêu cầu thanh toán</button>}
    <button disabled={busy} onClick={()=>{setBusy(true);setError("");setPayment(null);setMeet("");onRefresh();}}>Tải lại thanh toán</button>
    {payment?.status==="CONFIRMED"&&row.status!=="ACTIVE"&&<p>Tải lại đăng ký để xem quyền vào lớp.</p>}
    {row.status==="ACTIVE"&&<button disabled={busy} onClick={()=>void act(true)}>Mở khóa học</button>}
    {meet&&<a href={meet} target="_blank" rel="noopener noreferrer">Vào lớp qua Google Meet</a>}
  </section>;
}
