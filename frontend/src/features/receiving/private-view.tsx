"use client";

import Image from "next/image";
import {useCallback, useEffect, useRef, useState} from "react";
import {apiRequest, ApiRequestError} from "../../lib/api";
import {safeMessage} from "../auth/browser-api";
import {useSession} from "../auth/session-provider";
import {privateRequest} from "./receiving";
import styles from "./receiving.module.css";

export function message(error: unknown) {
  if (error instanceof ApiRequestError) {
    if (error.code==="PENDING_SUBMISSION_EXISTS") return "Bạn đã có hồ sơ chờ xét duyệt.";
    if (error.code==="IDEMPOTENCY_CONFLICT") return "Thông tin gửi lại khác với lần gửi trước. Kiểm tra danh sách trước khi gửi tiếp.";
    if (error.status===409) return "Trạng thái hồ sơ đã thay đổi. Vui lòng xem lại quyết định mới nhất.";
    if (error.status===404) return "Không tìm thấy tài khoản nhận tiền.";
  }
  return safeMessage(error);
}
export function useAccess() {
  const {clearSession} = useSession();
  const [denied,setDenied] = useState(false);
  const mounted = useRef(false);
  useEffect(()=>{mounted.current=true; return ()=>{mounted.current=false;};},[]);
  const handle = useCallback((error: unknown)=>{
    if (!mounted.current || !(error instanceof ApiRequestError)) return;
    if (error.status===401) clearSession();
    if (error.status===403) setDenied(true);
  },[clearSession]);
  return {denied,handle};
}
export function useLoad<T>(load:(signal:AbortSignal)=>Promise<T>, handle:(error:unknown)=>void, revision=0) {
  const [result,setResult] = useState<{load:typeof load; revision:number; value?:T; error?:string}>();
  useEffect(()=>{
    const controller = new AbortController();
    load(controller.signal).then(value=>{
      if (!controller.signal.aborted) setResult({load,revision,value});
    },error=>{
      if (!controller.signal.aborted) {handle(error);setResult({load,revision,error:message(error)});}
    });
    return ()=>controller.abort();
  },[load,handle,revision]);
  return result?.load===load && result.revision===revision ? result : undefined;
}
export function PrivateQr({token,path,handle,label}:{label?:string;token:string;path:string;handle:(error:unknown)=>void}) {
  const [visible,setVisible]=useState(false);
  return <div><button type="button" aria-expanded={visible} onClick={()=>setVisible(!visible)}>{visible?"Ẩn ảnh QR":label??"Xem ảnh QR riêng tư"}</button>
    {visible && <QrPreview label={label} token={token} path={path} handle={handle}/>}</div>;
}
function QrPreview({token,path,handle,label}:{label?:string;token:string;path:string;handle:(error:unknown)=>void}) {
  const [src,setSrc]=useState(""), [error,setError]=useState("");
  useEffect(()=>{
    const controller=new AbortController(); let url="";
    apiRequest(path,privateRequest(token,controller.signal),"image/png").then(blob=>{
      if (controller.signal.aborted) return;
      url=URL.createObjectURL(blob);setSrc(url);
    },failure=>{if(!controller.signal.aborted){handle(failure);setError(message(failure));}});
    return ()=>{controller.abort();if(url) URL.revokeObjectURL(url);};
  },[token,path,handle]);
  if(error) return <p role="alert">{error}</p>;
  return src?<Image className={styles.qr} src={src} alt={label??"Ảnh QR ngân hàng đã nộp"} width={320} height={320} unoptimized/>:<p role="status">Đang tải ảnh QR…</p>;
}
