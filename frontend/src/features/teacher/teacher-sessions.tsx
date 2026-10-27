"use client";

import { useRef, useState, type FormEvent } from "react";
import { authorized, safeMessage, type Session } from "../auth/browser-api";
import { useSession } from "../auth/session-provider";
import { ApiRequestError } from "../../lib/api";
import { localInput, localToInstant, nullableText, validateRules, type WeeklyRule } from "./session-input";
import styles from "./teacher-courses.module.css";

type Course={id:string;status:string;timezone:string;plannedStartDate:string;sessionCount:number};
export type Schedule={courseId:string;plannedStartDate:string;timezone:string;sessionCount:number;generatedSessionCount:number;rules:WeeklyRule[]};
export type Lesson={id:string;courseId:string;sessionNumber:number;title:string|null;scheduledStartAt:string;scheduledEndAt:string;status:string;content?:string|null;cancellationReason?:string|null;updatedAt?:string};
type Change={id:string;sessionId:string;oldStartAt:string;oldEndAt:string;newStartAt:string;newEndAt:string;changedAt:string};
type Page<T>={items:T[];page:number;size:number;hasNext:boolean};
const messages:Record<string,string>={SESSION_CHANGED:"Buổi học đã thay đổi. Hãy tải lại trước khi chỉnh sửa.",SCHEDULE_OVERLAP:"Lịch bị chồng lấn với buổi học khác.",SCHEDULE_TIME_CONFLICT:"Giờ học không hợp lệ hoặc đã bắt đầu.",CALENDAR_SYNC_REQUIRED:"Buổi học đã liên kết Calendar; chưa thể thay đổi tại đây.",SESSION_NOT_EDITABLE:"Trạng thái hiện tại không cho phép chỉnh sửa. Hãy tải lại.",COURSE_SCHEDULE_LOCKED:"Đã có buổi học; không thể thay thế quy tắc.",SESSIONS_ALREADY_GENERATED:"Các buổi học đã được tạo. Hãy tải lại.",SCHEDULE_GENERATION_LIMIT:"Chỉ tạo tối đa 1.000 buổi trong một yêu cầu.",COURSE_NOT_DRAFT:"Khóa học không còn là bản nháp. Hãy tải lại.",SESSION_NOT_FOUND:"Không tìm thấy buổi học của khóa học này.",COURSE_NOT_FOUND:"Không tìm thấy khóa học của bạn."};
function checked<T>(value:T|undefined):T {if(!value)throw new Error("Invalid response");return value;}
export function TeacherSessions({session,course}:{session:Session;course:Course}) {
  const {clearSession}=useSession();const flight=useRef(false);
  const [schedule,setSchedule]=useState<Schedule|null>(null),[rules,setRules]=useState<WeeklyRule[]>([]);
  const [page,setPage]=useState<Page<Lesson>|null>(null),[filter,setFilter]=useState("");
  const [detail,setDetail]=useState<Lesson|null>(null),[history,setHistory]=useState<Page<Change>|null>(null);
  const [busy,setBusy]=useState(false),[blocked,setBlocked]=useState(true),[denied,setDenied]=useState(false),[message,setMessage]=useState("");
  const [confirm,setConfirm]=useState<"generate"|"cancel"|null>(null),[reason,setReason]=useState("");
  const [currentCourse,setCurrentCourse]=useState(course);
  const [checkedAt,setCheckedAt]=useState(0);
  const root=`/teacher/courses/${course.id}`;
  const editable=!!detail&&detail.status==="SCHEDULED"&&["DRAFT","PUBLISHED"].includes(currentCourse.status);
  const future=editable&&Date.parse(detail!.scheduledStartAt)>checkedAt;
  const ruleEditable=currentCourse.status==="DRAFT"&&schedule?.generatedSessionCount===0;
  const display=(value:string)=>new Intl.DateTimeFormat("vi-VN",{timeZone:currentCourse.timezone,dateStyle:"medium",timeStyle:"short"}).format(new Date(value));
  async function run(work:()=>Promise<void>,mutation=false) {
    if(flight.current||mutation&&blocked)return;
    flight.current=true;setBusy(true);setMessage("");setCheckedAt(Date.now());
    try{await work();}catch(error){
      const known=error instanceof ApiRequestError;
      if(known&&[401,403].includes(error.status)){setDenied(true);setBlocked(true);setSchedule(null);setPage(null);setDetail(null);setHistory(null);if(error.status===401)clearSession();}
      if(mutation&&(!known||error.status>=500)){setBlocked(true);setMessage("Chưa xác nhận được kết quả. Hãy tải lại dữ liệu trước khi ghi tiếp. Không tự động gửi lại yêu cầu.");}
      else{if(known&&[404,409].includes(error.status))setBlocked(true);setMessage(known?messages[error.code]??safeMessage(error):safeMessage(error));}
    }finally{flight.current=false;setBusy(false);setConfirm(null);}
  }
  async function list(number=0,status=filter) {
    const data=checked(await authorized<Page<Lesson>>(session.accessToken,`${root}/sessions?page=${number}&size=20${status?`&status=${status}`:""}`));
    if(!Array.isArray(data.items)||data.page!==number||data.items.some(r=>typeof r.id!=="string"||r.courseId!==course.id))throw new Error("Invalid session page");
    setPage(data);
  }
  async function readDetail(id:string) {
    const data=checked(await authorized<Lesson>(session.accessToken,`${root}/sessions/${id}`));
    if(data.id!==id||data.courseId!==course.id||!(typeof data.content==="string"||data.content===null))throw new Error("Invalid detail");
    setDetail(data);setHistory(null);setReason("");return data;
  }
  async function reload() {
    setBlocked(true);
    const parent=checked(await authorized<Course>(session.accessToken,root));
    if(parent.id!==course.id)throw new Error("Invalid course");setCurrentCourse(parent);
    const data=checked(await authorized<Schedule>(session.accessToken,`${root}/schedule`));
    if(data.courseId!==course.id||!Array.isArray(data.rules)||!Number.isInteger(data.generatedSessionCount))throw new Error("Invalid schedule");
    setSchedule(data);setRules(data.rules);await list(page?.page??0);if(detail)await readDetail(detail.id);
    setBlocked(false);setDenied(false);
  }
  async function changed(value:Lesson|undefined) {
    const data=checked(value);if(data.id!==detail?.id||data.courseId!==course.id)throw new Error("Invalid mutation response");
    setDetail(data);setHistory(null);await list(page?.page??0);setMessage("Đã cập nhật buổi học.");
  }
  function edit(event:FormEvent<HTMLFormElement>) {
    event.preventDefault();if(!detail||blocked)return;const f=new FormData(event.currentTarget);
    try{const body:Record<string,unknown>={};for(const key of ["title","content"] as const){const value=nullableText(String(f.get(key)??""),key==="title"?200:10000,key==="content");if(value!==detail[key])body[key]=value;}
      if(!Object.keys(body).length){setMessage("Không có thay đổi.");return;}void run(async()=>changed(await authorized<Lesson>(session.accessToken,`${root}/sessions/${detail.id}`,body,"PATCH")),true);
    }catch(e){setMessage((e as Error).message);}
  }
  function reschedule(event:FormEvent<HTMLFormElement>) {
    event.preventDefault();if(!detail||blocked)return;const f=new FormData(event.currentTarget),start=String(f.get("start")),end=String(f.get("end"));
    try{if(start.slice(0,10)!==end.slice(0,10))throw new Error("Giờ bắt đầu và kết thúc phải trong cùng ngày.");
      const scheduledStartAt=localToInstant(start,currentCourse.timezone),scheduledEndAt=localToInstant(end,currentCourse.timezone);
      if(Date.parse(scheduledStartAt)<=Date.now()||scheduledEndAt<=scheduledStartAt)throw new Error("Giờ bắt đầu phải ở tương lai, kết thúc phải sau bắt đầu.");
      void run(async()=>changed(await authorized<Lesson>(session.accessToken,`${root}/sessions/${detail.id}/reschedule`,{expectedStartAt:detail.scheduledStartAt,expectedEndAt:detail.scheduledEndAt,scheduledStartAt,scheduledEndAt})),true);
    }catch(e){setMessage((e as Error).message);}
  }
  function saveRules(){try{const body={rules:validateRules(rules)};void run(async()=>{const data=checked(await authorized<Schedule>(session.accessToken,`${root}/schedule`,body,"PUT"));if(data.courseId!==course.id)throw new Error("Invalid schedule");setSchedule(data);setRules(data.rules);setMessage("Đã lưu lịch tuần.");},true);}catch(e){setMessage((e as Error).message);}}
  async function confirmed() {
    if(confirm==="generate")await run(async()=>{const data=checked(await authorized<{courseId:string;generatedSessionCount:number}>(session.accessToken,`${root}/sessions/generate`,undefined,"POST"));if(data.courseId!==course.id||data.generatedSessionCount!==schedule?.sessionCount)throw new Error("Invalid generation");await reload();setMessage(`Đã tạo ${data.generatedSessionCount} buổi học.`);},true);
    if(confirm==="cancel"&&detail){try{const value=nullableText(reason,1000);await run(async()=>changed(await authorized<Lesson>(session.accessToken,`${root}/sessions/${detail.id}/cancel`,{reason:value})),true);}catch(e){setConfirm(null);setMessage((e as Error).message);}}
  }
  return <section className={styles.panel} aria-label="Lịch và buổi học" aria-busy={busy}>
    <h3>Lịch và buổi học</h3><button className="ui-button ui-button-secondary" disabled={busy} onClick={()=>void run(reload)}>Tải lại lịch và buổi học</button>
    {busy&&<p role="status">Đang xử lý…</p>}{message&&<p role="alert" className={styles.notice}>{message}</p>}
    {blocked&&!denied&&<p>Cần tải lại dữ liệu trước khi thay đổi.</p>}
    {!denied&&schedule&&<><p>Múi giờ: {currentCourse.timezone} · Ngày bắt đầu: {schedule.plannedStartDate} · Dự kiến: {schedule.sessionCount} buổi · Đã tạo: {schedule.generatedSessionCount}</p>
      <h4>3. Lịch giảng dạy — lịch học hằng tuần</h4><p>Tối đa 100 quy tắc; tối đa 1.000 buổi mỗi lần tạo. Không tự động tạo lại sau yêu cầu chưa xác định kết quả.</p>
      <fieldset disabled={busy||blocked||!ruleEditable}><legend>Quy tắc lịch tuần</legend>{rules.map((r,i)=><div className={styles.toolbar} key={i}>
        <label>Thứ {i+1}<select value={r.dayOfWeek} onChange={e=>setRules(rules.map((v,n)=>n===i?{...v,dayOfWeek:Number(e.target.value)}:v))}>{[1,2,3,4,5,6,7].map(d=><option value={d} key={d}>{d===7?"Chủ nhật":`Thứ ${d+1}`}</option>)}</select></label>
        <label>Bắt đầu {i+1}<input type="time" step="60" value={r.startTime} onChange={e=>setRules(rules.map((v,n)=>n===i?{...v,startTime:e.target.value}:v))}/></label>
        <label>Kết thúc {i+1}<input type="time" step="60" value={r.endTime} onChange={e=>setRules(rules.map((v,n)=>n===i?{...v,endTime:e.target.value}:v))}/></label>
        <button type="button" className="ui-button ui-button-secondary" onClick={()=>setRules(rules.filter((_,n)=>i!==n))}>Xóa quy tắc {i+1}</button></div>)}
        <div className={styles.toolbar}><button className="ui-button ui-button-secondary" disabled={rules.length>=100} onClick={()=>setRules([...rules,{dayOfWeek:1,startTime:"19:00",endTime:"20:00"}])}>Thêm quy tắc</button><button className="ui-button ui-button-primary" onClick={saveRules}>Lưu lịch tuần</button></div></fieldset>
      {!ruleEditable&&<p>Không thể thay thế lịch tuần sau khi có buổi học hoặc khóa học không còn là bản nháp.</p>}
      <button className="ui-button ui-button-primary" disabled={busy||blocked||!ruleEditable||!schedule.rules.length||schedule.sessionCount>1000} onClick={()=>setConfirm("generate")}>Tạo các buổi học</button>
    </>}
    {!denied&&page&&<><h4>4. Nội dung từng buổi</h4><p>Mỗi buổi là một cuộc gặp giảng dạy theo lịch. Chọn một buổi để chỉnh sửa tiêu đề và nội dung riêng; việc lưu nội dung không tạo lại buổi học.</p><h5>Danh sách buổi học</h5><label>Trạng thái buổi học<select disabled={busy} value={filter} onChange={e=>{setFilter(e.target.value);setDetail(null);setHistory(null);void run(()=>list(0,e.target.value));}}><option value="">Tất cả</option><option value="SCHEDULED">Đã lên lịch</option><option value="CANCELLED">Đã hủy</option></select></label>
      {!page.items.length&&<p>Không có buổi học trên trang này.</p>}<ul className={styles.list}>{page.items.map(s=><li key={s.id}><div><strong>Buổi {s.sessionNumber}: {s.title??"Chưa có tiêu đề"}</strong><p>{display(s.scheduledStartAt)} – {display(s.scheduledEndAt)}</p><span>{s.status==="CANCELLED"?"Đã hủy":"Đã lên lịch"}</span></div><button className="ui-button ui-button-secondary" disabled={busy} onClick={()=>void run(async()=>{await readDetail(s.id);})}>Xem buổi {s.sessionNumber}</button></li>)}</ul>
      <div className={styles.toolbar}><button disabled={busy||page.page===0} onClick={()=>void run(()=>list(page.page-1))}>Buổi học: trang trước</button><span>Trang {page.page+1}</span><button disabled={busy||!page.hasNext} onClick={()=>void run(()=>list(page.page+1))}>Buổi học: trang sau</button></div>
    </>}
    {!denied&&detail&&<article key={`${detail.id}-${detail.updatedAt}-${detail.scheduledStartAt}-${detail.status}`}><h4>Chi tiết buổi {detail.sessionNumber}</h4><p>{display(detail.scheduledStartAt)} – {display(detail.scheduledEndAt)} ({currentCourse.timezone})</p><p className={styles.plain}>{detail.content??"Chưa có nội dung."}</p>{detail.cancellationReason&&<p>Lý do hủy: {detail.cancellationReason}</p>}
      <form onSubmit={edit}><fieldset disabled={busy||blocked||!editable} className={styles.grid}><legend>Nội dung buổi học</legend><label>Tiêu đề buổi học<input name="title" defaultValue={detail.title??""}/></label><label className={styles.wide}>Nội dung buổi học<textarea name="content" rows={5} defaultValue={detail.content??""}/></label><p>Để trống để xóa trường. Nội dung được lưu dưới dạng văn bản thuần.</p><button className="ui-button ui-button-primary">Lưu nội dung</button></fieldset></form>
      <form onSubmit={reschedule}><fieldset disabled={busy||blocked||!future} className={styles.grid}><legend>Đổi lịch theo múi giờ khóa học</legend><label>Bắt đầu mới<input name="start" type="datetime-local" step="60" required defaultValue={localInput(detail.scheduledStartAt,currentCourse.timezone)}/></label><label>Kết thúc mới<input name="end" type="datetime-local" step="60" required defaultValue={localInput(detail.scheduledEndAt,currentCourse.timezone)}/></label><button className="ui-button ui-button-primary">Đổi lịch buổi học</button></fieldset></form>
      <label>Lý do hủy (không bắt buộc)<input disabled={busy||blocked||!future} value={reason} onChange={e=>setReason(e.target.value)}/></label><button className="ui-button ui-button-secondary" disabled={busy||blocked||!future} onClick={()=>setConfirm("cancel")}>Hủy buổi học</button>
      <div className={styles.toolbar}><button disabled={busy} onClick={()=>void run(async()=>setHistory(checked(await authorized<Page<Change>>(session.accessToken,`${root}/sessions/${detail.id}/schedule-changes?page=0&size=20`))))}>Xem lịch sử đổi lịch</button></div>
      {history&&<><h5>Lịch sử đổi lịch</h5>{!history.items.length&&<p>Chưa có thay đổi lịch.</p>}<ul className={styles.list}>{history.items.map(h=><li key={h.id}><div><p>Cũ: {display(h.oldStartAt)} – {display(h.oldEndAt)}</p><p>Mới: {display(h.newStartAt)} – {display(h.newEndAt)}</p><p>Thay đổi lúc: {display(h.changedAt)}</p></div></li>)}</ul><div className={styles.toolbar}>{[-1,1].map(delta=><button key={delta} disabled={busy||(delta<0?history.page===0:!history.hasNext)} onClick={()=>void run(async()=>setHistory(checked(await authorized<Page<Change>>(session.accessToken,`${root}/sessions/${detail.id}/schedule-changes?page=${history.page+delta}&size=20`))))}>Lịch sử: trang {delta<0?"trước":"sau"}</button>)}</div></>}
    </article>}
    {confirm&&<Confirmation busy={busy} title={confirm==="generate"?"Xác nhận tạo buổi học":"Xác nhận hủy buổi học"} close={()=>setConfirm(null)} accept={()=>void confirmed()}>
      {confirm==="generate"?"Tạo theo lịch tuần đã lưu. Chỉ tạo một lần; không tự động gửi lại yêu cầu.":"Hủy buổi học không thể khôi phục và không tạo buổi học thay thế."}
    </Confirmation>}
  </section>;
}
function Confirmation({busy,title,children,close,accept}:{busy:boolean;title:string;children:string;close:()=>void;accept:()=>void}) {
  return <dialog className={styles.confirm} ref={node=>{if(node&&!node.open)node.showModal();}} aria-label={title} onCancel={e=>{e.preventDefault();if(!busy)close();}}><h4>{title}</h4><p>{children}</p><div className={styles.toolbar}><button autoFocus disabled={busy} onClick={close}>Quay lại</button><button className="ui-button ui-button-primary" disabled={busy} onClick={accept}>Xác nhận</button></div></dialog>;
}
