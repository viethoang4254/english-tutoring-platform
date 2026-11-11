"use client";
import {useEffect,useRef,useState} from "react";
import {authorized,safeMessage,type Session} from "../auth/browser-api";
import {outlineFields,type CourseOutline,type OutlineField,type Stage} from "../course/course-outline";

function move<T>(items:T[],from:number,to:number) {
  const next=items.slice();
  next.splice(to,0,next.splice(from,1)[0]);
  return next;
}
function Order({index,count,onMove,onRemove}:{index:number;count:number;onMove:(to:number)=>void;onRemove:()=>void}) {
  return <div><button type="button" disabled={!index} onClick={()=>onMove(index-1)}>Lên</button>
    <button type="button" disabled={index+1===count} onClick={()=>onMove(index+1)}>Xuống</button>
    <button type="button" onClick={onRemove}>Xóa</button></div>;
}
export function OutlineEditor({id,session}:{id:string;session:Session}) {
  const path='/teacher/courses/'+id+'/outline';
  const [value,setValue]=useState<CourseOutline>(),[message,setMessage]=useState(''),[busy,setBusy]=useState(false);
  const [attempt,setAttempt]=useState(0);
  const saving=useRef(false);
  useEffect(()=>{
    let active=true;
    authorized<CourseOutline>(session.accessToken,path).then(data=>{
      if(!data || !Array.isArray(data.stages))throw new Error();
      if(active){setValue(data);setMessage('');}
    }).catch(e=>{if(active)setMessage(safeMessage(e));});
    return ()=>{active=false;};
  },[session.accessToken,path,attempt]);
  if(!value)return <p role="status">{message || "Đang tải nội dung…"} {message&&<button onClick={()=>setAttempt(n=>n+1)}>Thử lại</button>}</p>;
  const stages=value.stages;
  function changeStage(index:number,stage:Stage){setValue({...value!,stages:stages.map((old,i)=>i===index?stage:old)});}
  return <form onSubmit={async e=>{
    e.preventDefault();
    if(saving.current)return;
    saving.current=true;setBusy(true);setMessage('');
    try {
      await authorized(session.accessToken,path,value,'PUT');
      setMessage('Đã lưu nội dung giới thiệu và lộ trình.');
    } catch(error){setMessage(safeMessage(error));}
    finally{saving.current=false;setBusy(false);}
  }}><fieldset disabled={busy}><legend>Nội dung công khai</legend>
    {(Object.keys(outlineFields) as OutlineField[]).map(key=><label key={key}>{outlineFields[key]}<textarea rows={3} maxLength={10000} value={value[key]} onChange={e=>setValue({...value,[key]:e.target.value})}/></label>)}
    <h3>Lộ trình học</h3><p>Đề cương này sẽ hiển thị công khai. Quản lý lịch học trực tuyến và tài liệu riêng ở mục Buổi học.</p>
    {stages.map((stage,i)=><fieldset key={i}><legend>Giai đoạn {i+1}</legend>
      <label>Tên giai đoạn<input required maxLength={200} value={stage.title} onChange={e=>changeStage(i,{...stage,title:e.target.value})}/></label>
      <Order index={i} count={stages.length} onMove={to=>setValue({...value,stages:move(stages,i,to)})} onRemove={()=>setValue({...value,stages:stages.filter((_,j)=>i!==j)})}/>
      {stage.topics.map((topic,j)=><fieldset key={j}><legend>Chủ đề {i+1}.{j+1}</legend>
        {(["title","description"] as const).map(key=><label key={key}>{key==="title"?"Tên chủ đề":"Mô tả chủ đề"}<textarea required maxLength={key==="title"?200:2000} value={topic[key]} onChange={e=>changeStage(i,{...stage,topics:stage.topics.map((t,k)=>k===j?{...t,[key]:e.target.value}:t)})}/></label>)}
        <Order index={j} count={stage.topics.length} onMove={to=>changeStage(i,{...stage,topics:move(stage.topics,j,to)})} onRemove={()=>changeStage(i,{...stage,topics:stage.topics.filter((_,k)=>k!==j)})}/>
      </fieldset>)}
      <button type="button" disabled={stage.topics.length>=100} onClick={()=>changeStage(i,{...stage,topics:[...stage.topics,{title:"",description:""}]})}>Thêm chủ đề</button>
    </fieldset>)}
    <button type="button" disabled={stages.length>=60} onClick={()=>setValue({...value,stages:[...stages,{title:"",topics:[]}]})}>Thêm giai đoạn</button>
    <button type="submit">Lưu nội dung công khai</button>
  </fieldset>{message&&<p role="status">{message}</p>}</form>;
}
