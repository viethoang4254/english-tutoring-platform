"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { authorized, safeMessage, type Session } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import styles from "./teacher-courses.module.css";
import { useSession } from "../auth/session-provider";
import { useRouter } from "next/navigation";
import { TeacherSessions } from "./teacher-sessions";
import { TeacherCoursePreview } from "./teacher-course-preview";

type Category = { id: string; name: string; active: boolean };
export type Course = { id: string; name: string; status: string; category: Category; description?: string;
  tuition: { amount: string; currency: string }; minStudents: number; maxStudents: number;
  plannedStartDate: string; timezone: string; sessionCount: number;
  discount?: { percent: string; startAt: string; endAt: string } | null };
type Page<T> = { items: T[]; page: number; size: number; hasNext: boolean };
const states: Record<string,string> = { DRAFT:"Bản nháp", PUBLISHED:"Đã công bố", COMPLETED:"Hoàn thành", CANCELLED:"Đã hủy", ARCHIVED:"Lưu trữ" };
const root = "/teacher/courses";
function valid(c: Course | undefined): c is Course {
  return !!c && typeof c.id === "string" && /^[1-9][0-9]*$/.test(c.id) && typeof c.name === "string" &&
    Object.hasOwn(states,c.status) && typeof c.category?.id === "string" && typeof c.category.name === "string" &&
    typeof c.category.active === "boolean" && typeof c.tuition?.amount === "string" && c.tuition.currency === "VND" &&
    typeof c.plannedStartDate === "string" && typeof c.timezone === "string" &&
    Number.isInteger(c.minStudents) && Number.isInteger(c.maxStudents) && Number.isInteger(c.sessionCount);
}
export function TeacherCourses({ session, courseId, section }: { session: Session; courseId?: string; section?: "schedule" | "sessions" }) {
  const router = useRouter();
  const [sessionsFor, setSessionsFor] = useState("");
  const [previewFor, setPreviewFor] = useState("");
  const preview = !!courseId && previewFor === courseId;
  const schedulePanel = !!courseId && (sessionsFor === courseId || !!section);
  const onSchedule = () => { setPreviewFor(""); setSessionsFor(courseId ?? ""); };
  const selectedId = useRef<string | null>(null);
  const { clearSession } = useSession();
  const [page,setPage]=useState<Page<Course>|null>(null), [filter,setFilter]=useState("");
  const [selected,setSelected]=useState<Course|null>(null), [editing,setEditing]=useState(false);
  const [categories,setCategories]=useState<Category[]>([]), [categoryPage,setCategoryPage]=useState(-1), [more,setMore]=useState(false);
  const [busy,setBusy]=useState(false), [blocked,setBlocked]=useState(false), [message,setMessage]=useState("");
  const flight=useRef(false);
  useEffect(() => {
    if (!courseId || selectedId.current === courseId) return;
    let active = true;
    authorized<Course>(session.accessToken, `${root}/${courseId}`).then(data => {
      if (!valid(data) || data.id !== courseId || typeof data.description !== "string") throw new Error("Invalid course detail");
      if (active) { selectedId.current = data.id; setSelected(data); setEditing(false); }
    }).catch(error => {
      if (!active) return;
      setMessage(safeMessage(error)); setSelected(null); setBlocked(true);
      if (error instanceof ApiRequestError && error.status === 401) clearSession();
    });
    return () => { active = false; };
  }, [courseId, session.accessToken, clearSession]);
  async function run(work:()=>Promise<void>, mutation=false) {
    if(flight.current)return;
    flight.current=true;setBusy(true);setMessage("");
    try { await work(); } catch(error) {
      const known=error instanceof ApiRequestError;
      if(mutation && (!known || error.status>=500)) {
        setBlocked(true);setMessage("Chưa xác nhận được kết quả. Hãy tải lại khóa học trước khi tiếp tục. Yêu cầu không được tự động gửi lại.");
      } else {
        const messages: Record<string,string>={ CATEGORY_INACTIVE:"Danh mục đã ngừng hoạt động. Hãy tải lại danh mục và chọn mục khác.", CATEGORY_NOT_FOUND:"Không tìm thấy danh mục.", COURSE_NOT_DRAFT:"Khóa học không còn là bản nháp. Hãy tải lại để xem trạng thái mới.", COURSE_NOT_FOUND:"Không tìm thấy khóa học của bạn." };
        setMessage(known ? messages[error.code] ?? safeMessage(error) : safeMessage(error));
        if(known && [401,403].includes(error.status)) {setBlocked(true);setSelected(null);setPage(null);setEditing(false);if(error.status===401)clearSession();}
      }
    } finally {flight.current=false;setBusy(false);}
  }
  async function load(number=0,status=filter) {
    const data=await authorized<Page<Course>>(session.accessToken,`${root}?page=${number}&size=20${status?`&status=${status}`:""}`);
    if(!data || data.page!==number || !Array.isArray(data.items) || !data.items.every(valid) || typeof data.hasNext!=="boolean")throw new Error("Invalid course page");
    setPage(data);selectedId.current=null;setSelected(null);setEditing(false);setBlocked(false);
  }
  async function loadCategories(number=0) {
    const data=await authorized<Page<Category>>(session.accessToken,`/teacher/categories?page=${number}&size=100`);
    if(!data || data.page!==number || typeof data.hasNext!=="boolean" || !Array.isArray(data.items) || !data.items.every(c=>typeof c.id==="string" && c.active===true && typeof c.name==="string"))throw new Error("Invalid category page");
    setCategories(old=>number===0?data.items:[...old,...data.items.filter(c=>!old.some(o=>o.id===c.id))]);setCategoryPage(number);setMore(data.hasNext);
  }
  async function detail(id:string) {
    const data=await authorized<Course>(session.accessToken,`${root}/${id}`);
    if(!valid(data)||data.id!==id||typeof data.description!=="string")throw new Error("Invalid course detail");
    selectedId.current=data.id;setSelected(data);setEditing(false);
  }
  function submit(event:FormEvent<HTMLFormElement>) {
    event.preventDefault();if(blocked||flight.current)return;
    const f=new FormData(event.currentTarget), value=(key:string)=>String(f.get(key)??"");
    const body:Record<string,unknown>={categoryId:value("categoryId"),name:value("name"),description:value("description"),
      tuition:{amount:value("amount"),currency:"VND"},minStudents:Number(value("minStudents")),maxStudents:Number(value("maxStudents")),
      sessionCount:Number(value("sessionCount")),plannedStartDate:value("plannedStartDate"),timezone:value("timezone"),discount:null};
    if(Number(value("maxStudents"))<Number(value("minStudents"))) {setMessage("Số học viên tối đa phải lớn hơn hoặc bằng tối thiểu.");return;}
    if(value("percent")||value("startAt")||value("endAt")) {
      if(!value("percent")||!value("startAt")||!value("endAt")) {setMessage("Vui lòng nhập đủ phần trăm và thời gian giảm giá.");return;}
      body.discount={percent:value("percent"),startAt:value("startAt"),endAt:value("endAt")};
    }
    void run(async()=>{
      const saved=await authorized<Course>(session.accessToken,selected?`${root}/${selected.id}`:root,body,selected?"PATCH":"POST");
      if(!valid(saved)||typeof saved.description!=="string"||(selected&&saved.id!==selected.id))throw new Error("Invalid saved course");
      selectedId.current=saved.id;setSelected(saved);setEditing(false);setPage(null);router.push(`/teacher/courses/${saved.id}`);setMessage("Đã lưu bản nháp khóa học.");
    },true);
  }
  return <section className={styles.panel} aria-label="Khóa học của tôi" aria-busy={busy}>
    {busy&&<p role="status">Đang xử lý…</p>}{message&&<p className={styles.notice} role="alert">{message}</p>}
    {(editing || (selected && selected.id === courseId)) && <>
      <ol className={styles.steps} aria-label="Các bước xây dựng khóa học">
        {["Thông tin cơ bản", "Học phí & sĩ số", "Lịch giảng dạy", "Nội dung từng buổi", "Xem trước"].map((label, index) => <li key={label}><span>{index + 1}</span>{label}</li>)}
      </ol>
      <p>Tạo bản nháp → lưu lịch tuần → tạo buổi học → soạn nội dung từng buổi → xem trước thông tin đã lưu.</p>
    </>}
    {courseId && selected?.id === courseId && !editing && !blocked && <div className={styles.toolbar} aria-label="Các phần của khóa học">
      <button className="ui-button ui-button-secondary" disabled={busy} aria-pressed={!schedulePanel && !preview} onClick={() => { setPreviewFor(""); setSessionsFor(""); router.replace(`/teacher/courses/${courseId}`); }}>Thông tin & học phí</button>
      <button className="ui-button ui-button-secondary" disabled={busy} aria-pressed={schedulePanel && !preview} onClick={onSchedule}>Lịch giảng dạy & nội dung</button>
      <button className="ui-button ui-button-secondary" disabled={busy} aria-pressed={preview} onClick={() => setPreviewFor(courseId)}>Xem trước khóa học</button>
    </div>}
    {preview && courseId && selected?.id === courseId && !blocked && <TeacherCoursePreview key={courseId} session={session} courseId={courseId} />}
    <div hidden={schedulePanel || preview}>
    <div hidden={!!courseId}>
    <h2>Khóa học của tôi</h2>
    {selected && <button className="ui-button ui-button-secondary" onClick={() => router.push(`/teacher/courses/${selected.id}`)}>Tiếp tục khóa học đã chọn</button>}
    <div className={styles.toolbar}>
      <label>Trạng thái khóa học<select disabled={busy} value={filter} onChange={e=>{setFilter(e.target.value);void run(()=>load(0,e.target.value));}}><option value="">Tất cả</option>{Object.entries(states).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
      <button className="ui-button ui-button-secondary" disabled={busy} onClick={()=>void run(()=>load(page?.page??0))}>Tải lại khóa học</button>
      <button className="ui-button ui-button-primary" disabled={busy||blocked} onClick={()=>void run(async()=>{await loadCategories();selectedId.current=null;setSelected(null);setEditing(true);})}>Tạo bản nháp</button>
    </div>

    {!page&&!selected&&!editing&&!busy&&<p>Tải danh sách để xem khóa học của bạn.</p>}
    {page&&<><ul className={styles.list}>{page.items.map(c=><li key={c.id}><div><strong>{c.name}</strong><p>{states[c.status]} · {c.category.name} · {c.tuition.amount} {c.tuition.currency}</p></div><button className="ui-button ui-button-secondary" disabled={busy} onClick={()=>void run(async()=>{await detail(c.id);router.push(`/teacher/courses/${c.id}`);})}>Xem chi tiết</button></li>)}</ul>
      {!page.items.length&&<p>Không có khóa học trên trang này.</p>}<div className={styles.toolbar}><button className="ui-button ui-button-secondary" disabled={busy||page.page===0} onClick={()=>void run(()=>load(page.page-1))}>Trang trước</button><span>Trang {page.page+1}</span><button className="ui-button ui-button-secondary" disabled={busy||!page.hasNext} onClick={()=>void run(()=>load(page.page+1))}>Trang sau</button></div></>}
    </div>
    {courseId && <button className="ui-button ui-button-secondary" disabled={busy} onClick={()=>void run(async()=>{await detail(courseId);setBlocked(false);})}>Tải lại khóa học</button>}
    {courseId && selected?.id !== courseId && <p role="status">Chưa tải được chi tiết khóa học.</p>}
    {selected&&selected.id===courseId&&!editing&&<article><h3>{selected.name}</h3><dl>{Object.entries({"Trạng thái":states[selected.status],"Danh mục":selected.category.name+(selected.category.active?"":" (ngừng hoạt động)"),"Mô tả":selected.description,"Học phí":`${selected.tuition.amount} VND`,"Số học viên":`${selected.minStudents}–${selected.maxStudents}`,"Ngày bắt đầu dự kiến":selected.plannedStartDate,"Múi giờ":selected.timezone,"Số buổi dự kiến":selected.sessionCount,"Giảm giá":selected.discount?`${selected.discount.percent}% · ${selected.discount.startAt} → ${selected.discount.endAt}`:"Không"}).map(([label,text])=><div key={label}><dt>{label}</dt><dd>{text}</dd></div>)}</dl>
      {selected.status==="DRAFT"?<button className="ui-button ui-button-primary" disabled={busy||blocked} onClick={()=>void run(async()=>{await loadCategories();setEditing(true);})}>Chỉnh sửa bản nháp</button>:<p>Khóa học này chỉ được xem trong giao diện quản lý bản nháp.</p>}</article>}
    {editing&&(!courseId||selected?.id===courseId)&&<form key={selected?.id??"new"} onSubmit={submit}><h3>{selected?"Chỉnh sửa bản nháp":"Bản nháp mới"}</h3><fieldset disabled={busy||blocked}><legend>Thông tin bản nháp</legend>
<section className={styles.formSection}><h4>1. Thông tin cơ bản</h4><div className={styles.grid}>
      <label>Danh mục<select name="categoryId" required defaultValue={selected?.category.id??""}><option value="">Chọn danh mục</option>{selected&&!categories.some(c=>c.id===selected.category.id)&&<option value={selected.category.id}>{selected.category.name} (giữ danh mục hiện tại)</option>}{categories.map(c=><option key={c.id} value={c.id}>{c.name}</option>)}</select></label>
      <div className={styles.toolbar}><button type="button" onClick={()=>void run(()=>loadCategories())}>Tải lại danh mục</button>{more&&<button type="button" onClick={()=>void run(()=>loadCategories(categoryPage+1))}>Thêm danh mục</button>}</div>
      <label>Tên khóa học<input name="name" required defaultValue={selected?.name}/></label>
      <label className={styles.wide}>Mô tả<textarea name="description" required rows={4} defaultValue={selected?.description}/></label>
</div></section>
<section className={styles.formSection}><h4>2. Học phí & sĩ số</h4><div className={styles.grid}>
      <label>Học phí (VND)<input name="amount" inputMode="decimal" required pattern="[0-9]+(\.[0-9]{1,2})?" defaultValue={selected?.tuition.amount??"0.00"}/></label>
      {([['minStudents','Số học viên tối thiểu',selected?.minStudents??1],['maxStudents','Số học viên tối đa',selected?.maxStudents??1]] as const).map(([name,label,value])=><label key={name}>{label}<input name={name} type="number" min={1} max={2147483647} step={1} required defaultValue={value}/></label>)}
      <label>Giảm giá (%) — không bắt buộc<input name="percent" inputMode="decimal" pattern="[0-9]+(\.[0-9]{1,2})?" defaultValue={selected?.discount?.percent}/></label>
      <label>Bắt đầu giảm giá (ISO 8601)<input name="startAt" placeholder="2026-12-01T00:00:00+07:00" defaultValue={selected?.discount?.startAt}/></label>
      <label>Kết thúc giảm giá (ISO 8601)<input name="endAt" placeholder="2026-12-10T00:00:00+07:00" defaultValue={selected?.discount?.endAt}/></label>
</div></section>
<section className={styles.formSection}><h4>3. Lịch giảng dạy dự kiến</h4><div className={styles.grid}>
      <label>Số buổi dự kiến<input name="sessionCount" type="number" min={1} max={2147483647} step={1} required defaultValue={selected?.sessionCount??1}/></label>
      <label>Ngày bắt đầu dự kiến<input name="plannedStartDate" type="date" required defaultValue={selected?.plannedStartDate}/></label>
      <label>Múi giờ<input name="timezone" required defaultValue={selected?.timezone??"Asia/Ho_Chi_Minh"}/></label>
</div></section>
<p>Để bỏ giảm giá, xóa cả ba trường giảm giá. Sau khi lưu bản nháp, cấu hình lịch tuần rồi tạo buổi học. Khi đã có buổi học, ngày bắt đầu, múi giờ và số buổi không thể thay đổi.</p>
      <div className={`${styles.toolbar} ${styles.wide}`}><button className="ui-button ui-button-primary">Lưu bản nháp</button><button type="button" className="ui-button ui-button-secondary" onClick={()=>setEditing(false)}>Hủy</button></div>
    </fieldset></form>}
    {selected&&selected.id===courseId&&!editing&&!blocked&&<button className="ui-button ui-button-primary" onClick={onSchedule}>Quản lý lịch & buổi học</button>}
    </div>
    <div hidden={!schedulePanel || preview}>
    <button className="ui-button ui-button-secondary" onClick={()=>{setSessionsFor("");router.replace(`/teacher/courses/${courseId}`);}}>Thông tin khóa học</button>
    <h3>{selected?.name ?? "Chưa chọn khóa học"}</h3>
    {(!selected||editing||blocked)&&<p>Chọn một khóa học trong mục Khóa học của tôi và tải chi tiết trước khi quản lý lịch.</p>}
    {selected&&!editing&&!blocked&&<TeacherSessions key={selected.id} session={session} course={selected}/>}
    </div>
  </section>;
}
