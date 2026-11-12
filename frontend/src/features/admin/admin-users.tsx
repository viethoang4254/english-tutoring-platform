"use client";
import { CreateUser, UserDialog } from "./create-user";
import ui from "./admin-users.module.css";
import { useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { authorized, safeMessage, type Session } from "../auth/browser-api";
import { useSession } from "../auth/session-provider";
import { ApiRequestError } from "../../lib/api";
import styles from "./admin-dashboard.module.css";
type User = {id:string;fullName:string;email:string;role:string;emailVerified:boolean;locked:boolean};
type Result = {items:User[];page:number;size:number;hasNext:boolean};
const roles:Record<string,string>={STUDENT:"Học viên",TEACHER:"Gia sư",ADMIN:"Quản trị viên"};
export function AdminUsers({session}:{session:Session}) {
 const params=useSearchParams(),router=useRouter(),{clearSession}=useSession();
 const pageText=params.get("page")??"0",search=params.get("search")??"",role=params.get("role")??"";
 const page=Number(pageText),valid=/^[0-9]+$/.test(pageText)&&Number.isSafeInteger(page)&&page<=107374182;
 const [result,setResult]=useState<Result|null>(null),[detail,setDetail]=useState<User|null>(null),[error,setError]=useState(""),[loading,setLoading]=useState(true),[retry,setRetry]=useState(0);
 function url(nextPage=page){const q=new URLSearchParams({section:"users",page:String(nextPage)});if(search)q.set("search",search);if(role)q.set("role",role);return `/admin/dashboard?${q}`;}
 useEffect(()=>{let active=true;queueMicrotask(()=>{if(!active)return;setLoading(true);setError("");setResult(null);setDetail(null);
  if(!valid){setError("Số trang không hợp lệ.");setLoading(false);return;}
  const q=new URLSearchParams({page:String(page),size:"20",search});if(role)q.set("role",role);
  const request=authorized<Result>(session.accessToken,`/admin/users?${q}`).then(value=>{
    if (!value) throw new Error("Missing user list response");
    if (active) setResult(value);
  });
  request.catch(e=>{if(active){setError(safeMessage(e));if(e instanceof ApiRequestError&&e.status===401)clearSession();}}).finally(()=>{if(active)setLoading(false);});});return()=>{active=false;};
 },[session.accessToken,page,valid,search,role,retry,clearSession]);
 const [saving,setSaving]=useState(false),[notice,setNotice]=useState("");
 const mutation=useRef(false);
 const [creating,setCreating]=useState(false);
 const [fullName,setFullName]=useState("");
 const [statusAction,setStatusAction]=useState<"lock"|"unlock">("lock");
 const [actionError,setActionError]=useState("");
 const [removing,setRemoving]=useState<User|null>(null);
 function openEditor(user:User){
  setDetail(user);setFullName(user.fullName);setActionError("");
 }
 function confirmStatus(user:User, action:"lock"|"unlock"){
  setActionError("");setStatusAction(action);setRemoving(user);
 }
 async function save(user:User, action:"profile"|"lock"){
  if(mutation.current)return;
  mutation.current=true;setSaving(true);setNotice("");setActionError("");
  try {
   const nextLocked = statusAction !== "unlock";
   const changes = action==="profile"?{fullName:fullName.trim()}:{locked:nextLocked};
   const saved=await authorized<User>(session.accessToken,`/admin/users/${user.id}/${action}`,changes,"PATCH");
   if (!saved || saved.id!==user.id || (action==="profile"?saved.fullName!==fullName.trim():saved.locked!==nextLocked))throw new Error("Invalid response");
   setResult(current=>current && {...current,items:current.items.map(item=>item.id===saved.id?saved:item)});
   if(action==="profile")setRetry(n=>n+1);
   setNotice(action==="profile"?"Đã lưu họ và tên.":nextLocked?"Đã khóa tài khoản.":"Đã mở khóa tài khoản.");
   setDetail(null);setRemoving(null);
  }catch(e){setActionError(safeMessage(e));if(e instanceof ApiRequestError&&e.status===401)clearSession();}
  finally{mutation.current=false;setSaving(false);}
 }
 const fields=(u:User)=><><p>{u.email}</p><p>Vai trò: {roles[u.role]??u.role} ({u.role})</p><p>{u.locked?"Đã khóa":"Không bị khóa"} · {u.emailVerified?"Email đã xác minh":"Email chưa xác minh"}</p></>;
 return <section className={styles.card} aria-label="Danh sách người dùng">
 {creating&&<CreateUser session={session} close={()=>setCreating(false)} created={message=>{setCreating(false);setNotice(message);setRetry(n=>n+1);}}/>}
 {notice&&<p role="status">{notice}</p>}
 <p>Khóa tài khoản là hạn chế truy cập có thể mở lại, không phải xóa vĩnh viễn. Chưa hỗ trợ xóa người dùng để bảo toàn hồ sơ và lịch sử liên quan.</p>
 <form className={ui.filters} key={`${search}:${role}`} onSubmit={e=>{e.preventDefault();const data=new FormData(e.currentTarget);const q=new URLSearchParams({section:"users",page:"0"});const text=String(data.get("search")??"").trim(),r=String(data.get("role")??"");if(text)q.set("search",text);if(r)q.set("role",r);router.push(`/admin/dashboard?${q}`);}}>
 <label>Tìm theo tên hoặc email <input name="search" defaultValue={search} maxLength={100}/></label>{" "}
 <label>Vai trò <select name="role" defaultValue={role}><option value="">Tất cả</option>{Object.entries(roles).map(([value,label])=><option key={value} value={value}>{label}</option>)}</select></label>{" "}<button type="submit">Tìm kiếm</button><button type="button" className={ui.addUser} onClick={()=>setCreating(true)}>+ Thêm người dùng</button></form>
 {loading&&<p role="status">Đang tải người dùng…</p>}
 {error&&<><p role="alert">{error}</p><button onClick={()=>setRetry(n=>n+1)}>Thử lại</button></>}
 {detail&&<UserDialog title={detail.role==="ADMIN"?"Chi tiết quản trị viên":"Chỉnh sửa người dùng"} close={()=>setDetail(null)} busy={saving}>
  <h3>{detail.fullName}</h3><p>ID: {detail.id}</p>{fields(detail)}
  {detail.role!=="ADMIN"&&<form id="admin-edit-name" className={ui.form} onSubmit={event=>{event.preventDefault();if(fullName.trim())void save(detail,"profile");}}>
   <label>Họ và tên<input required value={fullName} disabled={saving} onChange={event=>setFullName(event.target.value)} /></label>
   <small>Email hiện chưa hỗ trợ chỉnh sửa tại đây.</small>
  </form>}
  {actionError&&<p role="alert">{actionError}</p>}
  <footer className={ui.dialogActions}><button disabled={saving} onClick={()=>setDetail(null)}>Đóng</button>
   {detail.role!=="ADMIN"&&<button type="submit" form="admin-edit-name" disabled={saving||!fullName.trim()||fullName.trim()===detail.fullName}>Lưu thay đổi</button>}</footer>
 </UserDialog>}
 {removing&&<UserDialog title={statusAction==="lock"?"Khóa tài khoản?":"Mở khóa tài khoản?"} close={()=>setRemoving(null)} busy={saving}>
  <p>Tài khoản của <strong>{removing.fullName}</strong> ({removing.email}) {statusAction==="unlock"?"sẽ được mở khóa.":"sẽ bị khóa. Hồ sơ và lịch sử giao dịch được giữ lại."}</p>
  {actionError&&<p role="alert">{actionError}</p>}
  <footer className={ui.dialogActions}><button disabled={saving} onClick={()=>setRemoving(null)}>Hủy</button>
  <button disabled={saving} onClick={()=>void save(removing,"lock")}>{statusAction==="lock"?"Xác nhận khóa":"Xác nhận mở khóa"}</button></footer>
 </UserDialog>}
 {!loading&&result&&<>{result.items.length===0?<p>Không tìm thấy người dùng.</p>:<div className={ui.tableScroll} tabIndex={0} role="region" aria-label="Bảng người dùng"><table className={ui.table}><caption>Danh sách tài khoản</caption><thead><tr>{["ID","Họ tên / Email","Vai trò","Trạng thái","Xác minh email","Thao tác"].map(label=><th key={label} scope="col">{label}</th>)}</tr></thead><tbody>{result.items.map(u=><tr key={u.id}><td>{u.id}</td><td className={ui.identity}><strong>{u.fullName}</strong><span>{u.email}</span></td><td><span className={ui.badge}>{roles[u.role]}</span></td><td><span className={ui.badge}>{u.locked?"Đã khóa":"Không bị khóa"}</span></td><td>{u.emailVerified?"Đã xác minh":"Chưa xác minh"}</td><td><div className={ui.actions}><button onClick={()=>openEditor(u)}>{u.role==="ADMIN"?"Chi tiết":"Chỉnh sửa"}</button>{u.role!=="ADMIN"&&u.id!==session.account.id&&<><button onClick={()=>confirmStatus(u,u.locked?"unlock":"lock")}>{u.locked?"Mở khóa":"Khóa"}</button></>}</div></td></tr>)}</tbody></table></div>}
 <nav aria-label="Phân trang người dùng"><button disabled={page===0} onClick={()=>router.push(url(page-1))}>Trang trước</button> <span>Trang {page+1}</span> <button disabled={!result.hasNext} onClick={()=>router.push(url(page+1))}>Trang sau</button></nav></>}
 </section>;
}
