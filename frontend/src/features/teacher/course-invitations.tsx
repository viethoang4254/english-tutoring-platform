"use client";
import { useEffect, useRef, useState } from "react";
import { apiRequest, ApiRequestError } from "../../lib/api";
import { useSession } from "../auth/session-provider";
import type { Session } from "../auth/browser-api";
import { ConnectionPanel } from "../google-calendar/google-connection";
import type { Course } from "./teacher-courses";
import styles from "./teacher-courses.module.css";

type Props = {course:Course;session:Session};
type Summary = {eligibleStudents:number;invitationsEnabled:boolean;remindersEnabled:boolean;sessions:{
  number:number;start:string;end:string;status:"SCHEDULED"|"CANCELLED";syncState:"SYNCED"|"PENDING"|"FAILED"|null
}[]};
type Counts = {pending:number;synchronizedSessions:number;failed:number};
const integration="/integrations/google-calendar";

export function CourseInvitations({course,session}:Props) {
  const [open,setOpen]=useState(false);
  if(session.account.role!=="TEACHER")return null;
  return <details className={styles.invitationSection} onToggle={e=>setOpen(e.currentTarget.open)}>
    <summary>Google Calendar của khóa học</summary>
    {open&&<ConnectionPanel token={session.accessToken} accountId={session.account.id}
      courseContent={connected=><InvitationPanel key={course.status+":"+connected} course={course} session={session} connected={connected}/>}/>}
  </details>;
}

function InvitationPanel({course,session,connected}:Props & {connected:boolean}) {
  const {clearSession}=useSession();
  const [summary,setSummary]=useState<Summary>();
  const [counts,setCounts]=useState<Counts>();
  const [error,setError]=useState(""),[notice,setNotice]=useState("");
  const [loading,setLoading]=useState(true),[sending,setSending]=useState(false);
  const [revision,refresh]=useState(0);
  const active=useRef(false),flight=useRef(false);
  const root=integration+"/courses/"+course.id;
  const token=session.accessToken;
  useEffect(()=>{active.current=true;return()=>{active.current=false;};},[]);
  useEffect(()=>{
    let current=true;
    const options={headers:{Authorization:"Bearer "+token},credentials:"include",cache:"no-store"} as const;
    async function read(){
      const info=await apiRequest<Summary>(root+"/overview",options);
      if(!current)return;
      if(!info || !Array.isArray(info.sessions))throw new Error();
      setSummary(info);
      if(connected && info.invitationsEnabled && course.status==="PUBLISHED"){
        const progress=await apiRequest<Counts>(root+"/invitations",options);
        if(current)setCounts(progress);
      }
    }
    void read().catch(e=>{
      if(current){if(e instanceof ApiRequestError && e.status===401)clearSession();
        else setError(e instanceof ApiRequestError && e.status===403?"Bạn không có quyền quản lý khóa học này.":"Không thể tải trạng thái lời mời.");}
    }).finally(()=>{if(current)setLoading(false);});
    return ()=>{current=false;};
  },[token,root,connected,course.status,revision,clearSession]);

  async function queue(){
    if(flight.current||!counts)return;
    flight.current=true;setSending(true);setError("");setNotice("");
    const headers={Authorization:"Bearer "+token};
    try{
      const auth=await apiRequest<{csrfToken:string}>(integration,{headers,credentials:"include",cache:"no-store"});
      if(!active.current)return;
      if(!auth?.csrfToken)throw new Error();
      const next=await apiRequest<Counts>(root+"/invitations",{method:"POST",credentials:"include",cache:"no-store",
        headers:{...headers,"X-XSRF-TOKEN":auth.csrfToken}});
      if(active.current){setCounts(next);setNotice("Đã xếp hàng đồng bộ. Tải lại trạng thái để xem tiến độ.");}
    }catch(e){if(active.current){if(e instanceof ApiRequestError && e.status===401)clearSession();
      else {setCounts(undefined);setError("Chưa xác nhận được yêu cầu đồng bộ. Hãy tải lại trạng thái.");}}}
    finally{flight.current=false;if(active.current)setSending(false);}
  }

  const format=new Intl.DateTimeFormat("vi-VN",{timeZone:course.timezone,dateStyle:"short",timeStyle:"short"});
  return <section aria-label="Lời mời theo khóa học">
    <h3>Lời mời · {course.name}</h3>
    {error&&<p role="alert">{error}</p>}
    {notice&&<p role="status">{notice}</p>}
    {loading&&<p role="status">Đang tải trạng thái lời mời…</p>}
    {summary&&<>
      <dl className={styles.invitationMetrics}><div><dt>Học viên đủ điều kiện</dt><dd>{summary.eligibleStudents}</dd></div>
        <div><dt>Buổi sắp tới đã lên lịch</dt><dd>{summary.sessions.filter(s=>s.status==="SCHEDULED").length}</dd></div>
        {counts&&Object.entries({"Chờ đồng bộ":counts.pending,"Đã đồng bộ":counts.synchronizedSessions,"Đồng bộ lỗi":counts.failed})
          .map(([label,value])=><div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>
      {!summary.invitationsEnabled?<p>Tính năng gửi lời mời đang tắt.</p>:course.status!=="PUBLISHED"?<p>Chỉ có thể đồng bộ khóa học đã công bố.</p>:!connected&&<p>Cần kết nối Google và bật đồng bộ để quản lý lời mời.</p>}
    </>}
    <div className={styles.toolbar}>
      {counts&&<button className="ui-button ui-button-primary" disabled={sending||loading} onClick={()=>void queue()}>
        {sending?"Đang gửi yêu cầu…":counts.failed>0?"Thử đồng bộ lại lời mời":"Đồng bộ lời mời"}</button>}
      <button className="ui-button ui-button-secondary" disabled={loading||sending} onClick={()=>{
        setLoading(true);setError("");setNotice("");setSummary(undefined);setCounts(undefined);
        refresh(v=>v+1);
      }}>Tải lại trạng thái lời mời</button>
    </div>
    {summary&&<><h4>Buổi học sắp tới</h4>{summary.sessions.length===0?<p>Chưa có buổi học sắp tới.</p>:
      <ul className={styles.list}>{summary.sessions.map(s=><li key={s.number}><div><strong>Buổi {s.number} · {s.status==="SCHEDULED"?"Đã lên lịch":"Đã hủy"}</strong>
      <p>{format.format(new Date(s.start))} – {format.format(new Date(s.end))} ({course.timezone})</p>
      {s.syncState&&<span>{{SYNCED:"Đã đồng bộ",PENDING:"Chờ đồng bộ",FAILED:"Đồng bộ lỗi"}[s.syncState]}</span>}</div></li>)}</ul>}</>}
    {summary&&<><h4>Nhắc lịch qua email</h4><p>{summary.remindersEnabled?"Đã bật nhắc lịch qua email.":"Nhắc lịch qua email đang tắt."} Khi bật, hệ thống lên lịch nhắc học viên đủ điều kiện khoảng 15 phút trước buổi học.</p></>}
  </section>;
}
