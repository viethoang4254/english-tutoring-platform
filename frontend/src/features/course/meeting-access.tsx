"use client";

import { useEffect, useRef, useState } from "react";
import { authorized } from "../auth/browser-api";
import { useSession } from "../auth/session-provider";
import { ApiRequestError } from "../../lib/api";

// Remount when identity, resource or session status changes; never persist a room URL.
export function MeetingAccess({ endpoint, teacher = false }: { endpoint: string; teacher?: boolean }) {
  const { session, clearSession } = useSession();
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [url, setUrl] = useState("");
  const alive = useRef(true), flight = useRef(false);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  async function resolve(open: boolean) {
    if (!session || flight.current) return;
    flight.current = true; setBusy(true); setMessage(""); setUrl("");
    try {
      const result = await authorized<{ meetUrl?: string }>(session.accessToken, endpoint);
      if (!alive.current) return;
      if (!result?.meetUrl && teacher) { setMessage("Chưa có liên kết Google Meet hợp lệ. Cấu hình khi xuất bản khóa học."); return; }
      if (!result?.meetUrl || !/^https:\/\/meet\.google\.com\/[a-z]{3}-[a-z]{4}-[a-z]{3}$/.test(result.meetUrl)) throw new Error("Invalid meeting response");
      if (open) window.location.assign(result.meetUrl);
      else setUrl(result.meetUrl);
    } catch (error) {
      if (!alive.current) return;
      if (error instanceof ApiRequestError && error.status === 401) { clearSession(); return; }
      setMessage(error instanceof ApiRequestError && [403, 404].includes(error.status)
        ? "Bạn không có quyền mở phòng học này hoặc buổi học không còn khả dụng."
        : "Không thể mở Google Meet. Vui lòng thử lại.");
    } finally { flight.current = false; if (alive.current) setBusy(false); }
  }
  return <div aria-label="Truy cập Google Meet" aria-busy={busy}>
    <button type="button" className="ui-button ui-button-primary" disabled={busy || !session} onClick={() => void resolve(!teacher)}>{busy ? "Đang kiểm tra quyền truy cập…" : teacher ? "Xem phòng Google Meet" : "Tham gia Google Meet"}</button>
    {url && <p><a href={url} referrerPolicy="no-referrer" onClick={event => { event.preventDefault(); void resolve(true); }}>Mở Google Meet</a></p>}
    {message && <p role="alert">{message}</p>}
  </div>;
}
