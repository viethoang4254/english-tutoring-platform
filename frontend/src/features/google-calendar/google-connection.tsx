"use client";

import { useEffect, useRef, useState } from "react";
import styles from "./google-connection.module.css";
import { prepareGoogleReturn } from "./google-return";
import { useSession } from "../auth/session-provider";
import { apiRequest, ApiRequestError } from "../../lib/api";

type Connection={status:"REVOCATION_PENDING"|"UNAVAILABLE"|"DISCONNECTED"|"CONNECTED"|"RECONNECT_REQUIRED";email:string|null;reconnectRequired:boolean;connectedAt:string|null;synchronizationAvailable:boolean};
type Sync={state:"PENDING"|"SYNCED"|"FAILED"|"EMPTY"|"REVIEW_REQUIRED";pending:number;failed:number;lastSuccess:string|null};
type Status={connection:Connection;csrfToken:string;sync?:Sync};
const path="/integrations/google-calendar";
const messages:Record<string,string>={DISCONNECT_FIRST:"Vui lòng ngắt kết nối tài khoản Google hiện tại trước khi đổi tài khoản.",REVOCATION_PENDING:"Đã ngắt quyền truy cập trong ứng dụng. Đang chờ xác nhận thu hồi quyền từ Google.",CONNECTED:"Đã kết nối Google Calendar.",CONSENT_DENIED:"Bạn đã từ chối quyền truy cập Google.",EXPIRED_OR_INVALID:"Phiên kết nối đã hết hạn hoặc không hợp lệ. Vui lòng kết nối lại.",INVALID_RESPONSE:"Không thể xác minh phản hồi Google.",MISSING_PERMISSIONS:"Vui lòng cấp đầy đủ quyền Google Calendar.",RECONNECT_REQUIRED:"Cần kết nối lại tài khoản Google.",CONNECTION_CONFLICT:"Tài khoản Google đã được kết nối với tài khoản khác.",DENIED:"Tài khoản không có quyền kết nối.",UNAVAILABLE:"Kết nối Google Calendar hiện chưa khả dụng."};
export function GoogleConnection(){
  const {session}=useSession();
  const [expanded,setExpanded]=useState(false);
  if(!session||!["STUDENT","TEACHER"].includes(session.account.role))return null;
  if(session.account.role==="STUDENT")return <ConnectionPanel key={session.accessToken} accountId={session.account.id} token={session.accessToken}/>;
  return <details onToggle={event=>setExpanded(event.currentTarget.open)}><summary>Kết nối Google Calendar</summary>{expanded&&<ConnectionPanel key={session.accessToken} accountId={session.account.id} token={session.accessToken}/>}</details>;
}
function ConnectionPanel({token,accountId}:{token:string;accountId:string}){
  const {session,clearSession,googleOutcome}=useSession();
  const [result,setResult]=useState<Status>();const [error,setError]=useState("");
  const [attempt,setAttempt]=useState(0),[busy,setBusy]=useState(false);
  const flight=useRef(false),alive=useRef(true);
  const callback=googleOutcome ? messages[googleOutcome] : "";
  async function status(){return apiRequest<Status>(path,{credentials:"include",cache:"no-store",headers:{Authorization:`Bearer ${token}`}});}
  useEffect(()=>{alive.current=true;return()=>{alive.current=false;};},[]);
  useEffect(()=>{
    let active=true;
    apiRequest<Status>(path,{credentials:"include",cache:"no-store",headers:{Authorization:`Bearer ${token}`}})
      .then(data=>{if(!data?.connection||!data.csrfToken)throw new Error("Invalid connection response");if(active){setResult(data);setError("");}})
      .catch(e=>{if(active){if(e instanceof ApiRequestError&&e.status===401)clearSession();else setError(e instanceof ApiRequestError&&e.status===403?messages.DENIED:"Không thể tải kết nối Google. Vui lòng thử lại.");}});
    return()=>{active=false;};
  },[token,attempt,clearSession]);
  async function retrySync(){
    if(flight.current)return;
    flight.current=true;setBusy(true);setError("");
    try{
      const current=await status();
      if(!alive.current||!current?.csrfToken)return;
      await apiRequest(path+"/sync",{method:"POST",credentials:"include",headers:{Authorization:`Bearer ${token}`,"X-XSRF-TOKEN":current.csrfToken}});
      if(alive.current)setAttempt(a=>a+1);
    }catch(e){if(alive.current){if(e instanceof ApiRequestError&&e.status===401)clearSession();else setError("Không thể yêu cầu đồng bộ. Vui lòng thử lại.");}}
    finally{flight.current=false;if(alive.current)setBusy(false);}
  }
  async function mutate(disconnect:boolean){
    if(flight.current)return;flight.current=true;setBusy(true);setError("");
    try{
      const fresh=await status();if(!alive.current)return;
      if(!fresh?.csrfToken)throw new Error("Unavailable");
      const response=await apiRequest<{authorizationUrl?:string;remoteRevoked?:boolean}>(path+(disconnect?"":"/connect"),{
        method:disconnect?"DELETE":"POST",credentials:"include",cache:"no-store",headers:{Authorization:`Bearer ${token}`,"X-XSRF-TOKEN":fresh.csrfToken}});
      if(!alive.current)return;
      if(disconnect){const latest=await status();if(!alive.current)return;setResult(latest);if(response?.remoteRevoked===false)setError("Đã ngắt kết nối trong ứng dụng. Bạn có thể thu hồi quyền còn lại trong tài khoản Google.");}
      else{
        const url=new URL(response?.authorizationUrl??"");
        if(url.origin!=="https://accounts.google.com"||url.pathname!=="/o/oauth2/v2/auth"||url.username||url.password||url.hash)throw new Error("Invalid destination");
        prepareGoogleReturn(accountId);
        window.location.assign(url.href);
      }
    }catch(e){if(alive.current){if(e instanceof ApiRequestError&&e.status===401)clearSession();else setError("Chưa xác nhận được kết quả kết nối. Tải lại trạng thái trước khi thử lại.");}}
    finally{flight.current=false;if(alive.current)setBusy(false);}
  }
  return <section aria-label="Kết nối Google Calendar" className={session?.account.role==="STUDENT"?`ui-card ${styles.card}`:"ui-card"} style={{padding:"1rem",marginBlock:"1rem",overflowWrap:"anywhere"}}>
    <h2>Google Calendar</h2><p>{result?.connection.synchronizationAvailable?"Các buổi học đủ điều kiện được đồng bộ vào lịch riêng của ứng dụng.":"Đồng bộ buổi học chưa hoạt động."}</p>
    {callback&&<p role="status">{callback}</p>}
    {error&&<p role="alert">{error}</p>}
    {!result&&!error&&<p role="status">Đang tải kết nối…</p>}
    {result?.connection.status==="UNAVAILABLE"?<p>{messages.UNAVAILABLE}</p>:result?.connection.status==="REVOCATION_PENDING"?<p role="status">{messages.REVOCATION_PENDING}</p>:result&&<>
      <p>{result.connection.status==="CONNECTED"?"Đã kết nối":result.connection.reconnectRequired?"Cần kết nối lại":"Chưa kết nối"}</p>
      {result.sync&&<div role="status">
        <p>{{PENDING:"Đang đồng bộ lịch học",SYNCED:"Đã đồng bộ Google Calendar",FAILED:"Đồng bộ thất bại, vui lòng thử lại",EMPTY:"Chưa có buổi học đủ điều kiện",REVIEW_REQUIRED:"Chưa xác định được lịch Google. Vui lòng liên hệ quản trị viên."}[result.sync.state]}</p>
        {result.sync.lastSuccess&&<p>Lần đồng bộ gần nhất: {new Intl.DateTimeFormat("vi-VN",{dateStyle:"short",timeStyle:"short"}).format(new Date(result.sync.lastSuccess))}</p>}
        {(result.sync.pending>0||result.sync.failed>0)&&<p>Đang chờ: {result.sync.pending} · Lỗi: {result.sync.failed}</p>}
        {result.sync.state==="FAILED"&&<button className="ui-button ui-button-secondary" disabled={busy} onClick={()=>void retrySync()}>Thử đồng bộ lại</button>}
      </div>}
      {result.connection.email&&<p>Tài khoản Google: {result.connection.email}</p>}
      <button className="ui-button ui-button-primary" disabled={busy} onClick={()=>void mutate(false)}>{result.connection.status==="DISCONNECTED"?"Kết nối Google Calendar":"Kết nối lại Google Calendar"}</button>
      {result.connection.status!=="DISCONNECTED"&&<button className="ui-button ui-button-secondary" disabled={busy} onClick={()=>void mutate(true)}>Ngắt kết nối Google Calendar</button>}
    </>}
    <button className="ui-button ui-button-secondary" disabled={busy} onClick={()=>setAttempt(a=>a+1)}>Tải lại kết nối</button>
  </section>;
}
