"use client";
import { useEffect, useState } from "react";
import { authorized, safeMessage, type Session } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import { useSession } from "../auth/session-provider";
import styles from "./teacher-courses.module.css";
type Row={enrollmentId:string;studentDisplayName:string;status:string;paymentStatus:string|null;attendanceEligible:boolean};
type Page={items:Row[];page:number;size:number;hasNext:boolean};
const enrollment:Record<string,string>={PENDING:"Chờ xác nhận",ACTIVE:"Đang học",COMPLETED:"Hoàn thành",CANCELLED:"Đã hủy"};
const payment:Record<string,string>={PENDING:"Chờ thanh toán",CONFIRMED:"Đã xác nhận",EXPIRED:"Hết hạn",CANCELLED:"Đã hủy"};
export function CourseRoster({courseId,session}:{courseId:string;session:Session}) {
  const {clearSession}=useSession();
  const [page,setPage]=useState(0),[attempt,retry]=useState(0);
  const [result,setResult]=useState<Page>(),[error,setError]=useState("");
  useEffect(()=>{
    let active=true;
    authorized<Page>(session.accessToken,`/teacher/courses/${courseId}/enrollments?page=${page}&size=20`).then(data=>{
      if(!data||data.page!==page||data.size!==20||typeof data.hasNext!=="boolean"||!Array.isArray(data.items)||!data.items.every(r=>
        typeof r.enrollmentId==="string"&&typeof r.studentDisplayName==="string"&&Object.hasOwn(enrollment,r.status)&&
        (r.paymentStatus===null||Object.hasOwn(payment,r.paymentStatus))&&typeof r.attendanceEligible==="boolean"))throw new Error("Invalid roster");
      if(active)setResult(data);
    }).catch(e=>{if(active){setError(safeMessage(e));if(e instanceof ApiRequestError&&e.status===401)clearSession();}});
    return ()=>{active=false;};
  },[courseId,session.accessToken,page,attempt,clearSession]);
  function load(next:number){setResult(undefined);setError("");setPage(next);retry(v=>v+1);}
  return <section aria-label="Học viên khóa học">
    <h3>Học viên</h3><p>Điều kiện tham gia hiện tại; quyền vào từng buổi học vẫn được kiểm tra khi tham gia.</p>
    {error&&<p role="alert">{error}</p>}
    {!result&&!error&&<p role="status">Đang tải học viên…</p>}
    <button className="ui-button ui-button-secondary" onClick={()=>load(page)} disabled={!result&&!error}>Tải lại học viên</button>
    {result&&<>
      {result.items.length===0?<p>Chưa có học viên trên trang này.</p>:<ul className={styles.list}>{result.items.map(r=><li key={r.enrollmentId}><div style={{overflowWrap:"anywhere",minWidth:0}}>
        <strong>{r.studentDisplayName}</strong><dl>
          <div><dt>Ghi danh</dt><dd>{enrollment[r.status]}</dd></div>
          <div><dt>Thanh toán</dt><dd>{r.paymentStatus?payment[r.paymentStatus]:"Chưa có thanh toán"}</dd></div>
          <div><dt>Tham gia</dt><dd>{r.attendanceEligible?"Đủ điều kiện":"Chưa đủ điều kiện"}</dd></div>
        </dl></div></li>)}</ul>}
      <div className={styles.toolbar}><button className="ui-button ui-button-secondary" disabled={page===0} onClick={()=>load(page-1)}>Học viên: trang trước</button>
        <span>Trang {page+1}</span><button className="ui-button ui-button-secondary" disabled={!result.hasNext} onClick={()=>load(page+1)}>Học viên: trang sau</button></div>
    </>}
  </section>;
}
