"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { authorized, safeMessage, type Session } from "../auth/browser-api";
import { useSession } from "../auth/session-provider";
import { ApiRequestError } from "../../lib/api";
import { states, type Course } from "./teacher-courses";
import type { CalendarSession } from "./teacher-calendar";
import styles from "./teacher-overview.module.css";

type Slice<T> = {items:T[]; hasNext:boolean};
function OverviewList<T>({token,path,title,href,empty,children}:{
  token:string; path:string; title:string; href:string; empty:string;
  children:(item:T)=>ReactNode;
}) {
  const {clearSession}=useSession();
  const [attempt,retry]=useState(0);
  const [result,setResult]=useState<{attempt:number;data?:Slice<T>;error?:string}>();
  useEffect(()=>{
    let active=true;
    authorized<Slice<T>>(token,path).then(data=>{
      if(!data || !Array.isArray(data.items))throw new Error("Invalid page");
      if(active)setResult({attempt,data});
    }).catch(error=>{
      if(!active)return;
      if(error instanceof ApiRequestError && error.status===401)clearSession();
      else setResult({attempt,error:safeMessage(error)});
    });
    return ()=>{active=false};
  },[token,path,attempt,clearSession]);
  const current=result?.attempt===attempt ? result : undefined;
  return <section className={styles.section}><header><h2>{title}</h2><Link href={href}>Xem tất cả →</Link></header>
    {!current && <p role="status">Đang tải…</p>}
    {current?.error && <p role="alert">{current.error} <button onClick={()=>retry(attempt+1)}>Thử lại</button></p>}
    {current?.data && (current.data.items.length ? <ul>{current.data.items.map(children)}</ul> : <p>{empty}</p>)}
  </section>;
}

export function TeacherOverview({session}:{session:Session}) {
  const [from]=useState(()=>new Date());
  const to=new Date(from.getTime()+7*86400000);
  const params=new URLSearchParams({from:from.toISOString(),to:to.toISOString(),page:"0",size:"100"});
  const calendarPath="/teacher/calendar/sessions?"+params;
  return <div className={styles.overview}>
    <div className={styles.actions}><Link href="/teacher/courses">Quản lý khóa học →</Link><Link href="/teacher/calendar">Mở lịch dạy</Link><Link href="/teacher/payment-accounts">Tài khoản nhận tiền</Link></div>
    <OverviewList<Course> token={session.accessToken} path="/teacher/courses?page=0&size=5" title="Khóa học của tôi" href="/teacher/courses" empty="Bạn chưa có khóa học. Mở Quản lý khóa học để tạo bản nháp đầu tiên.">{course=><li key={course.id}><Link href={`/teacher/courses/${course.id}`}>{course.name}</Link><small>{course.category.name} · {course.sessionCount} buổi</small><span>{states[course.status]}</span></li>}</OverviewList>
    <UpcomingSessions token={session.accessToken} path={calendarPath} from={from}/>
  </div>;
}

function UpcomingSessions({token,path,from}:{token:string;path:string;from:Date}){
  return <OverviewList<CalendarSession> token={token} path={path} title="Lịch dạy trong 7 ngày tới" href="/teacher/calendar" empty="Chưa có buổi học trong khoảng thời gian này.">{item=><li key={item.sessionId}><Link href={`/teacher/courses/${item.courseId}?section=sessions`}>{item.sessionTitle || `Buổi ${item.sessionNumber}`} · {item.courseTitle}</Link><small>{new Intl.DateTimeFormat("vi-VN",{timeZone:item.courseTimezone,day:"2-digit",month:"2-digit",year:"numeric",hour:"2-digit",minute:"2-digit",hourCycle:"h23",timeZoneName:"long"}).format(new Date(item.startsAt))}</small><span>{item.status==="CANCELLED"?"Đã hủy":Date.parse(item.startsAt)<from.getTime()?"Đang diễn ra":"Sắp diễn ra"}</span></li>}</OverviewList>;
}
