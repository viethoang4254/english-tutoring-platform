"use client";
import Link from "next/link";
import {useEffect,useState} from "react";
import {authorized,safeMessage,type Session} from "../auth/browser-api";
import {ApiRequestError} from "../../lib/api";
import {useSession} from "../auth/session-provider";
import styles from "./teacher-calendar.module.css";
type Item={sessionId:string;courseId:string;courseTitle:string;sessionNumber:number;sessionTitle:string|null;startsAt:string;endsAt:string;courseTimezone:string;status:string};
type Page={items:Item[];page:number;size:number;hasNext:boolean};
const zone="Asia/Ho_Chi_Minh";
const dateKey=(d:Date)=>d.toISOString().slice(0,10);
const dayMs=86400000;
function today(){return new Intl.DateTimeFormat("en-CA",{timeZone:zone,year:"numeric",month:"2-digit",day:"2-digit"}).format(new Date());}
export function TeacherCalendar({session}:{session:Session}) {
 const {clearSession}=useSession();
 const [anchor,setAnchor]=useState(today),[view,setView]=useState<"month"|"week">("month");
 const [page,setPage]=useState(0),[attempt,setAttempt]=useState(0);
 const [state,setState]=useState<{key:string;data?:Page;error?:string}>({key:""});
 const d=new Date(anchor+"T00:00:00Z");
 const first=view==="month"?new Date(Date.UTC(d.getUTCFullYear(),d.getUTCMonth(),1)):new Date(d.getTime()-((d.getUTCDay()+6)%7)*dayMs);
 const last=view==="month"?new Date(Date.UTC(d.getUTCFullYear(),d.getUTCMonth()+1,1)):new Date(first.getTime()+7*dayMs);
 const from=dateKey(first)+"T00:00:00+07:00",to=dateKey(last)+"T00:00:00+07:00";
 const key=`${session.accessToken}:${from}:${to}:${page}:${attempt}`;
 useEffect(()=>{let active=true;authorized<Page>(session.accessToken,`/teacher/calendar/sessions?${new URLSearchParams({from,to,page:String(page),size:"100"})}`)
 .then(data=>{if(active){if(!data)throw Error();setState({key,data});}})
 .catch(e=>{if(active){setState({key,error:safeMessage(e)});if(e instanceof ApiRequestError&&e.status===401)clearSession();}});
 return()=>{active=false;};},[session.accessToken,from,to,page,key,clearSession]);
 const data=state.key===key?state.data:undefined,error=state.key===key?state.error:undefined;
 function move(delta:number){const next=view==="month"?new Date(Date.UTC(d.getUTCFullYear(),d.getUTCMonth()+delta,1)):new Date(d.getTime()+delta*7*dayMs);setAnchor(dateKey(next));setPage(0);}
 const days=Array.from({length:Math.round((last.getTime()-first.getTime())/dayMs)},(_,i)=>dateKey(new Date(first.getTime()+i*dayMs)));
 return <section aria-label="Lịch dạy của tôi" className={styles.calendar}>
 <p>Lưới lịch theo giờ Việt Nam (Asia/Ho_Chi_Minh). Mỗi buổi học hiển thị giờ và múi giờ của khóa học.</p>
 <div className={styles.toolbar}><button onClick={()=>{setView("month");setPage(0);}} aria-pressed={view==="month"}>Tháng</button><button onClick={()=>{setView("week");setPage(0);}} aria-pressed={view==="week"}>Tuần</button><button onClick={()=>move(-1)} aria-label="Kỳ trước">←</button><button onClick={()=>{setAnchor(today());setPage(0);}}>Hôm nay</button><button onClick={()=>move(1)} aria-label="Kỳ sau">→</button><strong>{days[0]} – {days.at(-1)}</strong></div>
 {!data&&!error&&<p role="status">Đang tải lịch dạy…</p>}
 {error&&<div role="alert">{error}<button onClick={()=>setAttempt(attempt+1)}>Tải lại</button></div>}
 {data&&<><p role="status">{data.items.length===0?"Không có buổi học trong trang lịch này.":`Trang ${page+1}: ${data.items.length} buổi học.`}</p>
 <div className={styles.grid} aria-label={view==="month"?"Lịch tháng":"Lịch tuần"}>
 {days.map((day,index)=><section key={day} className={styles.day} style={index===0?{gridColumnStart:((first.getUTCDay()+6)%7)+1}:undefined}><h2>{day}</h2>
 {data.items.filter(s=>new Date(s.endsAt).getTime()>new Date(day+"T00:00:00+07:00").getTime()).filter(s=>new Date(s.startsAt).getTime()<new Date(day+"T00:00:00+07:00").getTime()+dayMs).map(s=><article key={s.sessionId} className={s.status==="CANCELLED"?styles.cancelled:styles.session}>
 <strong>{s.courseTitle}</strong><p>Buổi {s.sessionNumber}{s.sessionTitle?`: ${s.sessionTitle}`:""}</p><p>{new Intl.DateTimeFormat("vi-VN",{timeZone:s.courseTimezone,dateStyle:"short",timeStyle:"short"}).format(new Date(s.startsAt))} – {new Intl.DateTimeFormat("vi-VN",{timeZone:s.courseTimezone,dateStyle:"short",timeStyle:"short"}).format(new Date(s.endsAt))}</p><small>{s.courseTimezone}</small><p>{s.status==="CANCELLED"?"Đã hủy":"Đã lên lịch"}</p><Link href={`/teacher/courses/${s.courseId}?section=sessions`}>Xem và quản lý buổi học</Link></article>)}
 </section>)}
 </div><div className={styles.toolbar}><button disabled={page===0} onClick={()=>setPage(page-1)}>Trang trước</button><span>Trang {page+1}</span><button disabled={!data.hasNext} onClick={()=>setPage(page+1)}>Trang sau</button></div><p>Lịch hiển thị một trang dữ liệu; dùng Trang sau để xem thêm khi có nhiều buổi học.</p></>}
 </section>;
}
