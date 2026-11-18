"use client";
import { useEffect,useRef,useState } from "react";
import { authorized,safeMessage,type Session } from "../auth/browser-api";
import { useSession } from "../auth/session-provider";
import { ApiRequestError } from "../../lib/api";
import { adminMutation } from "./admin-api";
import { CreateUser, UserDialog } from "./create-user";
import styles from "./admin-users.module.css";
type User={id:string;fullName:string;email:string;role:string;locked:boolean;emailVerified:boolean};
type Page={items:User[];page:number;size:number;hasNext:boolean};
export function AdminUsers({session}:{session:Session}) {
 const {clearSession}=useSession();
 const [query,setQuery]=useState({page:0,search:"",role:"",locked:"",attempt:0});
 const [result,setResult]=useState<Page>(),[error,setError]=useState(""),[detail,setDetail]=useState<User>(),[confirm,setConfirm]=useState<User>();
 const [creating,setCreating]=useState(false),[editing,setEditing]=useState<User>(),[deleting,setDeleting]=useState<User>();
 const [busy,setBusy]=useState(false),[notice,setNotice]=useState("");const flight=useRef(false);
 useEffect(()=>{let active=true;const params=new URLSearchParams({page:String(query.page),size:"20",search:query.search});if(query.role)params.set("role",query.role);if(query.locked)params.set("locked",query.locked);
  authorized<Page>(session.accessToken,`/admin/users?${params}`).then(data=>{if(!data||!Array.isArray(data.items)||data.page!==query.page)throw new Error("Invalid directory");if(active)setResult(data);}).catch(e=>{if(active){setError(safeMessage(e));if(e instanceof ApiRequestError&&e.status===401)clearSession();}});return()=>{active=false;};
 },[session.accessToken,query,clearSession]);
 function load(next= query){setResult(undefined);setError("");setDetail(undefined);setConfirm(undefined);setQuery({...next,attempt:query.attempt+1});}
 async function action(user:User,lock=false){if(flight.current)return;flight.current=true;setBusy(true);setError("");setNotice("");try{
  if(lock){const saved=await adminMutation<User>(session.accessToken,`/admin/users/${user.id}/lock`,{locked:!user.locked},"PATCH");if(!saved||saved.id!==user.id||saved.locked===user.locked)throw new Error("Unconfirmed status");setNotice(saved.locked?"Đã khóa tài khoản.":"Đã mở khóa tài khoản.");load();}
  else {const data=await authorized<User>(session.accessToken,`/admin/users/${user.id}`);if(!data||data.id!==user.id)throw new Error("Invalid detail");setDetail(data);}
 }catch(e){setError(safeMessage(e));setConfirm(undefined);if(e instanceof ApiRequestError&&e.status===401)clearSession();}finally{flight.current=false;setBusy(false);}}
 async function save(user:User,name?:string){if(flight.current)return;flight.current=true;setBusy(true);setError("");try{
 await adminMutation(session.accessToken,`/admin/users/${user.id}${name===undefined?"":"/profile"}`,name===undefined?undefined:{fullName:name},name===undefined?"DELETE":"PATCH");
 setEditing(undefined);setDeleting(undefined);setNotice(name===undefined?"Đã xóa vĩnh viễn tài khoản.":"Đã cập nhật người dùng.");load(name===undefined?{...query,page:0}:query);
 }catch(e){setError(e instanceof ApiRequestError&&e.status===409?"Tài khoản có dữ liệu liên quan và không thể xóa.":safeMessage(e));if(e instanceof ApiRequestError&&e.status===401)clearSession();}finally{flight.current=false;setBusy(false);}}
 return <section aria-label="Danh sách người dùng" className={styles.panel}>
 <button disabled={busy} onClick={()=>setCreating(true)}>Thêm người dùng</button>
 {creating&&<CreateUser session={session} close={()=>setCreating(false)} created={message=>{setCreating(false);setNotice(message);load();}}/>}
 {editing&&<UserDialog title="Chỉnh sửa người dùng" busy={busy} close={()=>setEditing(undefined)}><form onSubmit={e=>{e.preventDefault();void save(editing,String(new FormData(e.currentTarget).get("fullName")??"").trim());}}><label>Họ và tên<input name="fullName" defaultValue={editing.fullName} required maxLength={200}/></label>{error&&<p role="alert">{error}</p>}<button disabled={busy}>Lưu thay đổi</button><button type="button" disabled={busy} onClick={()=>setEditing(undefined)}>Hủy</button></form></UserDialog>}
 {deleting&&<UserDialog title="Xóa vĩnh viễn người dùng" busy={busy} close={()=>setDeleting(undefined)}><p>Xóa tài khoản {deleting.fullName}? Không thể hoàn tác. Tài khoản có dữ liệu nghiệp vụ sẽ không được xóa.</p>{error&&<p role="alert">{error}</p>}<button disabled={busy} onClick={()=>void save(deleting)}>Xác nhận xóa vĩnh viễn</button><button disabled={busy} onClick={()=>setDeleting(undefined)}>Hủy</button></UserDialog>}
 <form className={styles.filters} onSubmit={e=>{e.preventDefault();const f=new FormData(e.currentTarget);load({page:0,search:String(f.get("search")??"").trim(),role:String(f.get("role")??""),locked:String(f.get("locked")??""),attempt:0});}}>
 <label>Tìm theo tên hoặc email<input name="search" maxLength={100}/></label>
 <label>Vai trò<select name="role"><option value="">Tất cả</option>{["STUDENT","TEACHER","ADMIN"].map(r=><option key={r}>{r}</option>)}</select></label>
 <label>Trạng thái tài khoản<select name="locked"><option value="">Tất cả</option><option value="false">Hoạt động</option><option value="true">Đã khóa</option></select></label>
 <button disabled={busy}>Tìm kiếm</button><button type="button" disabled={busy} onClick={()=>load()}>Tải lại người dùng</button></form>
 {error&&<p role="alert">{error}</p>}{notice&&<p role="status">{notice}</p>}{!result&&!error&&<p role="status" className={styles.loading}>Đang tải người dùng…</p>}
 {confirm&&<section aria-label="Xác nhận trạng thái tài khoản"><p>{confirm.locked?"Mở khóa":"Khóa"} tài khoản {confirm.fullName}?</p><button disabled={busy} onClick={()=>void action(confirm,true)}>Xác nhận</button><button disabled={busy} onClick={()=>setConfirm(undefined)}>Hủy</button></section>}
 {detail&&<section aria-label="Chi tiết người dùng"><h3>{detail.fullName}</h3><p>{detail.email} · {detail.role}</p><p>{detail.locked?"Đã khóa":"Hoạt động"} · {detail.emailVerified?"Email đã xác minh":"Email chưa xác minh"}</p><button onClick={()=>setDetail(undefined)}>Đóng chi tiết</button></section>}
 {result&&<>{result.items.length===0?<p>Không tìm thấy người dùng.</p>:<table><caption>Danh sách tài khoản</caption><thead><tr><th>Họ tên / Email</th><th>Vai trò</th><th>Trạng thái</th><th>Xác minh email</th><th>Thao tác</th></tr></thead><tbody>{result.items.map(u=><tr key={u.id}>
 <td><strong>{u.fullName}</strong><div>{u.email}</div></td><td data-label="Vai trò">{u.role}</td><td data-label="Trạng thái">{u.locked?"Đã khóa":"Hoạt động"}</td><td data-label="Email">{u.emailVerified?"Đã xác minh":"Chưa xác minh"}</td><td><button disabled={busy} onClick={()=>void action(u)}>Chi tiết</button>{u.role!=="ADMIN"&&u.id!==session.account.id&&<><button disabled={busy} onClick={()=>setConfirm(u)}>{u.locked?"Mở khóa":"Khóa"}</button><button disabled={busy} onClick={()=>{setError("");setEditing(u);}}>Chỉnh sửa</button><button disabled={busy} onClick={()=>{setError("");setDeleting(u);}}>Xóa vĩnh viễn</button></>}</td></tr>)}</tbody></table>}
 <nav aria-label="Phân trang người dùng"><button disabled={busy||query.page===0} onClick={()=>load({...query,page:query.page-1})}>Trang trước</button><span>Trang {query.page+1}</span><button disabled={busy||!result.hasNext} onClick={()=>load({...query,page:query.page+1})}>Trang sau</button></nav></>}
 </section>;
}
