"use client";
import {Fragment,useEffect,useState} from "react";
import {authorized,safeMessage,type Session} from "../auth/browser-api";
import {useSession} from "../auth/session-provider";
import {ApiRequestError} from "../../lib/api";
import ui from "./admin-users.module.css";
type Course={id:string;name:string;description:string|null;teacher:{id:string;fullName:string};category:{id:string;name:string};tuition:{amount:string;currency:string};status:string;timezone:string;enrollmentCount:number;activeEnrollmentCount:number;plannedSessions:number;recordedSessions:number;scheduledSessions:number;firstSessionAt:string|null;lastSessionAt:string|null};
type Page={items:Course[];page:number;size:number;hasNext:boolean};
const statuses:Record<string,string>={DRAFT:"Bản nháp",PUBLISHED:"Đã xuất bản",COMPLETED:"Đã hoàn thành",CANCELLED:"Đã hủy",ARCHIVED:"Đã lưu trữ"};
export function AdminCourses({session}:{session:Session}){
 const {clearSession}=useSession();
 const [query,setQuery]=useState({page:0,search:"",teacherId:"",categoryId:"",status:"",attempt:0});
 const [selected,setSelected]=useState("");
 const [options,setOptions]=useState<{teachers:Record<string,string>;categories:Record<string,string>}>({teachers:{},categories:{}});
 const [result,setResult]=useState<Page>(),[detail,setDetail]=useState<Course>(),[error,setError]=useState("");
 useEffect(()=>{let active=true;const params=new URLSearchParams({page:String(query.page),size:"20"});
  for(const key of ["search","teacherId","categoryId","status"] as const)if(query[key])params.set(key,query[key]);
  const path=selected?`/admin/courses/${selected}`:`/admin/courses?${params}`;
  authorized<Page|Course>(session.accessToken,path).then(value=>{if(!active)return;if(!value)throw new Error("Missing response");if(selected)setDetail(value as Course);else {
    const page=value as Page;
    if(!Array.isArray(page.items)||page.page!==query.page||typeof page.hasNext!=="boolean")throw new Error("Invalid page");
    setResult(page);
    setOptions(previous=>{const teachers={...previous.teachers},categories={...previous.categories};for(const c of page.items){teachers[c.teacher.id]=c.teacher.fullName;categories[c.category.id]=c.category.name;}return {teachers,categories};});
   }})
   .catch(e=>{if(active){setError(safeMessage(e));if(e instanceof ApiRequestError&&e.status===401)clearSession();}});
  return()=>{active=false;};
 },[session.accessToken,query,selected,clearSession]);
 function load(next=query,id=""){setError("");setDetail(undefined);setResult(undefined);setSelected(id);setQuery({...next,attempt:query.attempt+1});}
 const money=(c:Course)=>new Intl.NumberFormat("vi-VN",{style:"currency",currency:c.tuition.currency}).format(Number(c.tuition.amount));
 const date=(value:string|null,zone:string)=>value?new Intl.DateTimeFormat("vi-VN",{dateStyle:"medium",timeStyle:"short",timeZone:zone}).format(new Date(value)):"Chưa có lịch";
 return <section className={ui.panel} aria-label="Danh sách khóa học">
 <p>Chỉ xem thông tin. Gia sư sở hữu quản lý nội dung, học phí và lịch học.</p>
 {selected?<button onClick={()=>load()}>Về danh sách khóa học</button>:<form className={ui.filters} onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);load({page:0,attempt:0,search:String(f.get("search")??"").trim(),teacherId:String(f.get("teacherId")??""),categoryId:String(f.get("categoryId")??""),status:String(f.get("status")??"")});}}>
 <label>Tên khóa học<input name="search" maxLength={100} defaultValue={query.search}/></label>
 <label>Gia sư<select name="teacherId" aria-label="Gia sư" defaultValue={query.teacherId}><option value="">Tất cả gia sư</option>{Object.entries(options.teachers).sort((a,b)=>a[1].localeCompare(b[1],"vi")).map(([id,name])=><option key={id} value={id}>{name} (#{id})</option>)}</select></label>
 <label>Danh mục<select name="categoryId" aria-label="Danh mục" defaultValue={query.categoryId}><option value="">Tất cả danh mục</option>{Object.entries(options.categories).sort((a,b)=>a[1].localeCompare(b[1],"vi")).map(([id,name])=><option key={id} value={id}>{name}</option>)}</select></label>
 <label>Trạng thái<select name="status" aria-label="Trạng thái khóa học" defaultValue={query.status}><option value="">Tất cả</option>{Object.entries(statuses).map(([v,label])=><option key={v} value={v}>{label}</option>)}</select></label><button>Tìm khóa học</button>
 </form>}
 {!selected&&<p>Bộ lọc gia sư và danh mục lấy từ các trang khóa học đã tải. Chọn tất cả để xem thêm các trang khác.</p>}
 <button onClick={()=>load(query,selected)}>{error?"Thử lại":"Tải lại khóa học"}</button>
 {error?<p role="alert">{error}</p>:!result&&!detail?<p role="status">Đang tải khóa học…</p>:null}
 {result&&<>{!result.items.length?<p>Không tìm thấy khóa học.</p>:<table><caption>Khóa học của các gia sư</caption><thead><tr><th>Khóa học / Gia sư</th><th>Danh mục</th><th>Học phí</th><th>Đăng ký / Trạng thái</th><th>Chi tiết</th></tr></thead><tbody>{result.items.map(c=><tr key={c.id}>
 <td><strong>{c.name}</strong><p>{c.teacher.fullName} · Mã {c.teacher.id}</p></td><td>{c.category.name} · Mã {c.category.id}</td><td>{money(c)}</td><td>{c.enrollmentCount} lượt đăng ký · {statuses[c.status]}</td><td><button onClick={()=>load(query,c.id)}>Xem khóa học {c.id}</button></td>
 </tr>)}</tbody></table>}<nav aria-label="Phân trang khóa học"><button disabled={query.page===0} onClick={()=>load({...query,page:query.page-1})}>Trang trước</button><span>Trang {query.page+1}</span><button disabled={!result.hasNext} onClick={()=>load({...query,page:query.page+1})}>Trang sau</button></nav></>}
 {detail&&<article aria-label="Chi tiết khóa học"><h2>{detail.name}</h2><dl className={ui.details}>{Object.entries({"Mô tả":detail.description??"Chưa có mô tả","Gia sư":detail.teacher.fullName,"Danh mục":detail.category.name,"Học phí":money(detail),"Trạng thái":statuses[detail.status],"Tổng đăng ký đã ghi nhận":detail.enrollmentCount,"Đăng ký đang hoạt động":detail.activeEnrollmentCount,"Buổi học dự kiến":detail.plannedSessions,"Buổi học đã tạo":detail.recordedSessions,"Buổi học đã lên lịch":detail.scheduledSessions,"Buổi lên lịch đầu tiên":date(detail.firstSessionAt,detail.timezone),"Kết thúc buổi lên lịch cuối":date(detail.lastSessionAt,detail.timezone),"Múi giờ":detail.timezone}).map(([label,value])=><Fragment key={label}><dt>{label}</dt><dd>{value}</dd></Fragment>)}</dl><p>Tổng đăng ký bao gồm mọi trạng thái; không phải số học viên được cấp quyền tham gia.</p></article>}
 </section>;
}
