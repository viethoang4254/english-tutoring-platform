"use client";
import { useEffect, useMemo, useState, type FormEvent } from "react";
import { ApiRequestError, apiRequest } from "../../lib/api";
import { safeMessage, type Session } from "../auth/browser-api";

export function CourseCoverUpload({id,session}:{id:string;session:Session}) {
  const path = `/teacher/courses/${id}/cover`;
  const token = session.accessToken;
  const [revision, setRevision] = useState(0);
  const request = useMemo(() => ({path,token,revision}), [path,token,revision]);
  const [loaded, setLoaded] = useState<{request: typeof request; url: string}>();
  const preview = loaded?.request === request ? loaded.url : undefined;
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    let url = "";
    apiRequest(request.path,{headers:{Authorization:`Bearer ${request.token}`},credentials:"omit",cache:"no-store",signal:controller.signal},"image/png")
      .then(blob => {
        if (!controller.signal.aborted) { url = URL.createObjectURL(blob); setLoaded({request,url}); }
      }).catch(error => {
        if (!controller.signal.aborted && !(error instanceof ApiRequestError && error.status === 404)) setMessage(safeMessage(error));
      });
    return () => {controller.abort(); if (url) URL.revokeObjectURL(url);};
  },[request]);

  async function submit(event:FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const image = data.get("image");
    if (!(image instanceof File) || !["image/png","image/jpeg"].includes(image.type) || !image.size || image.size > 5*1024*1024) {
      setMessage("Chọn ảnh PNG hoặc JPEG tối đa 5 MiB."); return;
    }
    setBusy(true);
    try {
      await apiRequest(path,{method:"PUT",headers:{Authorization:`Bearer ${token}`},body:data,credentials:"omit"});
      form.reset(); setRevision(value => value+1); setMessage("Đã lưu ảnh bìa.");
    } catch(error) {setMessage(safeMessage(error));}
    finally {setBusy(false);}
  }
  return <form onSubmit={submit}><fieldset disabled={busy}><legend>Ảnh bìa khóa học</legend>
    {preview && <img src={preview} alt="Ảnh bìa hiện tại" style={{width:"min(100%,24rem)",aspectRatio:"16/9",objectFit:"cover"}}/>}
    <label>Chọn ảnh PNG hoặc JPEG (tối đa 5 MiB)<input type="file" name="image" accept="image/png,image/jpeg" required/></label>
    <button className="ui-button ui-button-secondary">{busy ? "Đang tải…" : "Lưu ảnh bìa"}</button>
    <p role="status">{message}</p>
  </fieldset></form>;
}
