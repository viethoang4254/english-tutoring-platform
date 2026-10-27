"use client";
import {useEffect,useRef,useState,type FormEvent} from "react";
import {authorized,safeMessage,type Session} from "../auth/browser-api";
import {useSession} from "../auth/session-provider";
import {ApiRequestError} from "../../lib/api";
import {roleLabels} from "../../lib/vi";
type Profile={id:string;fullName:string;username:string|null;email:string;phone:string|null;emailVerified:boolean};
export function StudentProfile({session}:{session:Session}){
 const [profile,setProfile]=useState<Profile>();const [name,setName]=useState("");const [phone,setPhone]=useState("");const [editing,setEditing]=useState(false);const [busy,setBusy]=useState(false);const [message,setMessage]=useState("");const [uncertain,setUncertain]=useState(false);const pending=useRef(false);const {clearSession}=useSession();
 function apply(value:Profile){setProfile(value);setName(value.fullName);setPhone(value.phone??"");setEditing(false);setUncertain(false);}
 function failure(error:unknown){if(error instanceof ApiRequestError && error.status===401)clearSession();setMessage(error instanceof ApiRequestError && error.code==="PROFILE_UNAVAILABLE"?"Chưa xác nhận được thay đổi. Vui lòng tải lại hồ sơ trước khi tiếp tục.":safeMessage(error));}
 useEffect(()=>{let active=true;authorized<Profile>(session.accessToken,"/users/me/profile").then(value=>{if(active && value){setProfile(value);setName(value.fullName);setPhone(value.phone??"");}}).catch(error=>{if(active){if(error instanceof ApiRequestError && error.status===401)clearSession();setMessage(safeMessage(error));}});return()=>{active=false;};},[session.accessToken,clearSession]);
 async function reload(){if(pending.current)return;pending.current=true;setBusy(true);try{const value=await authorized<Profile>(session.accessToken,"/users/me/profile");if(value){apply(value);setMessage("");}}catch(error){failure(error);}finally{pending.current=false;setBusy(false);}}
 async function save(event:FormEvent){event.preventDefault();if(pending.current||!profile||uncertain)return;
  const fields:Record<string,string>={};if(name!==profile.fullName)fields.fullName=name;if(phone!==(profile.phone??""))fields.phone=phone;
  if(!Object.keys(fields).length){setEditing(false);return;}
  if(fields.fullName!==undefined && (!name.trim()||name.includes("\0"))){setMessage("Họ và tên không được để trống hoặc chứa ký tự không hợp lệ.");return;}
  if(fields.phone!==undefined && !/^(?:0|\+84)[35789][0-9]{8}$/.test(phone.replace(/^ +| +$/g,""))){setMessage("Số điện thoại không hợp lệ. Ví dụ: 0912345678.");return;}
  pending.current=true;setBusy(true);setMessage("");try{const value=await authorized<Profile>(session.accessToken,"/users/me/profile",fields,"PATCH");if(value){apply(value);setMessage("Đã cập nhật thông tin cá nhân.");}else{setUncertain(true);setMessage("Chưa xác nhận được thay đổi. Vui lòng tải lại hồ sơ.");}}
  catch(error){if(!(error instanceof ApiRequestError)||error.status>=500)setUncertain(true);failure(error);}finally{pending.current=false;setBusy(false);}
 }
 const initials = (profile?.fullName || "EL").trim().split(/\s+/).map((part: string)=>part[0]).join("").slice(-2).toUpperCase();
 return <section className="profile-card" aria-labelledby="student-profile-heading"><div className="profile-hero"><div className="profile-avatar" aria-hidden="true">{initials || "EL"}</div><div><p className="profile-eyebrow">TÀI KHOẢN CỦA BẠN</p><h2 id="student-profile-heading">{profile?.fullName || "Hồ sơ cá nhân"}</h2><span className="profile-role">{roleLabels[session.account.role]}</span></div></div>
 {message && <p aria-live="polite">{message}</p>}
 {!profile?<><p>{message?"Chưa tải được hồ sơ.":"Đang tải hồ sơ…"}</p>{message&&<button disabled={busy} onClick={()=>void reload()}>Tải lại hồ sơ</button>}</>:<>
 <div className="profile-grid"><div className="profile-field"><span>Tên người dùng</span><strong>{profile.username??"Chưa có tên người dùng"}</strong></div><div className="profile-field"><span>Email</span><strong>{profile.email}</strong></div><div className="profile-field"><span>Trạng thái email</span><strong className={profile.emailVerified?"profile-verified":"profile-unverified"}>{profile.emailVerified?"Đã xác minh":"Chưa xác minh"}</strong></div></div>
 {!editing?<> <div className="profile-editable"><div><span>Họ và tên</span><strong>{profile.fullName}</strong></div><div><span>Số điện thoại</span><strong>{profile.phone??"Chưa cung cấp"}</strong></div></div><button className="profile-action" onClick={()=>{setEditing(true);setMessage("");}}>Chỉnh sửa thông tin</button></>:<form onSubmit={save} noValidate><fieldset className="student-profile-grid" disabled={busy||uncertain}>
 <label>Họ và tên<input autoComplete="name" value={name} onChange={e=>setName(e.target.value)}/></label><label>Số điện thoại<input type="tel" autoComplete="tel" placeholder="0912345678" value={phone} onChange={e=>setPhone(e.target.value)}/></label>
 <button className="ui-button ui-button-primary" type="submit">{busy?"Đang lưu…":"Lưu thay đổi"}</button><button className="ui-button ui-button-secondary" type="button" onClick={()=>{apply(profile);setMessage("");}}>Hủy</button></fieldset></form>}
 {uncertain&&<button disabled={busy} onClick={()=>void reload()}>Tải lại hồ sơ</button>}
 </>}</section>;
}
