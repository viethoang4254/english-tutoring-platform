"use client";

import { useState } from "react";
import { statusLabels, formatDate } from "../../lib/vi";
import { ApiRequestError } from "../../lib/api";
import { authorized, type Session } from "../auth/browser-api";

type Item = { id: string; teacherId: string; status: "PENDING"; submittedAt: string };
type Queue = { items: Item[]; page: number; size: number; hasNext: boolean };
type Detail = Omit<Item, "status"> & {
  status: "PENDING" | "APPROVED" | "REJECTED";
  specialization: string; teachingExperience: string; introduction: string;
  reviewedBy: string | null; reviewedAt: string | null; rejectionReason: string | null;
};
const root = "/admin/teacher-applications";
const id = (value: string) => typeof value === "string" && /^[1-9][0-9]*$/.test(value);

export function AdminApplications({ session, busy, run }: {
  session: Session; busy: boolean; run: (work: () => Promise<void>) => Promise<void>;
}) {
  const [queue, setQueue] = useState<Queue | null>(null);
  const [detail, setDetail] = useState<Detail | null>(null);
  const [reviewable, setReviewable] = useState(false);
  const [notice, setNotice] = useState("");

  async function loadQueue(page = 0, clearDetail = true) {
    setQueue(null);
    if (clearDetail) { setDetail(null); setReviewable(false); }
    const result = await authorized<Queue>(session.accessToken, `${root}/pending?page=${page}&size=20`);
    if (!result || result.page !== page || result.size !== 20 || typeof result.hasNext !== "boolean" ||
        !Array.isArray(result.items) || result.items.some(item => !id(item.id) || !id(item.teacherId) || item.status !== "PENDING")) throw new Error("Invalid queue response");
    setQueue(result);
  }
  async function loadDetail(applicationId: string) {
    setDetail(null); setReviewable(false);
    const result = await authorized<Detail>(session.accessToken, `${root}/${applicationId}`);
    if (!result || result.id !== applicationId || !id(result.teacherId) || !["PENDING", "APPROVED", "REJECTED"].includes(result.status)) throw new Error("Invalid application response");
    setDetail(result); setReviewable(result.status === "PENDING");
  }
  async function review(action: "approve" | "reject", reason?: string) {
    if (!detail || !reviewable) return;
    if (action === "reject" && (!reason?.trim() || reason.includes("\0"))) throw new ApiRequestError(400, "INVALID_REQUEST", "Invalid input");
    const selectedId = detail.id; const page = queue?.page ?? 0;
    setReviewable(false); setNotice("");
    try {
      await authorized<void>(session.accessToken, `${root}/${selectedId}/${action}`, action === "reject" ? { reason: reason! } : undefined, "POST");
    } catch (error) {
      // Read committed state after conflicts/uncertainty; never replay a review mutation.
      setNotice("Chưa xác nhận được kết quả xét duyệt. Vui lòng kiểm tra hồ sơ đã cập nhật trước khi tiếp tục.");
      if (error instanceof ApiRequestError && [401, 403].includes(error.status)) { setDetail(null); throw error; }
      try { await loadDetail(selectedId); await loadQueue(page, false); }
      catch (lookupError) { setDetail(null); setReviewable(false); throw lookupError; }
      throw error;
    }
    setDetail(null);
    setNotice(action === "approve" ? "Đã phê duyệt hồ sơ." : "Đã từ chối hồ sơ.");
    await loadQueue(page);
  }

  return <section aria-labelledby="admin-applications-title">
    <h2 id="admin-applications-title">Hồ sơ gia sư chờ xét duyệt</h2>
    <p>Hồ sơ gửi trước được hiển thị trước. Danh sách có thể thay đổi trong quá trình xét duyệt.</p>
    <p role="status">{notice}</p>
    <button disabled={busy} onClick={() => void run(() => loadQueue())}>Cập nhật danh sách chờ xét duyệt</button>
    {queue && <>
      <p>Trang {(queue.page + 1).toLocaleString("vi-VN")}</p>
      {queue.items.length === 0 ? <p>Không có hồ sơ chờ xét duyệt trên trang này.</p> : <ul>
        {queue.items.map(item => <li key={item.id}>
          <p>Hồ sơ {item.id} · Gia sư {item.teacherId} · {statusLabels[item.status]}</p>
          <time dateTime={item.submittedAt}>{formatDate(item.submittedAt)}</time>{" "}
          <button disabled={busy} onClick={() => void run(() => loadDetail(item.id))}>Xem hồ sơ {item.id}</button>
        </li>)}
      </ul>}
      <div className="account-actions">
        <button disabled={busy || queue.page === 0} onClick={() => void run(() => loadQueue(queue.page - 1))}>Trang trước</button>
        <button disabled={busy || !queue.hasNext || (queue.page + 1) * 20 > 2147483647} onClick={() => void run(() => loadQueue(queue.page + 1))}>Trang sau</button>
      </div>
    </>}
    {detail && <section aria-label="Chi tiết hồ sơ">
      <h3>Hồ sơ {detail.id}</h3>
      <dl className="review-reason">
        <dt>Mã gia sư</dt><dd>{detail.teacherId}</dd>
        <dt>Trạng thái</dt><dd>{statusLabels[detail.status]}</dd>
        <dt>Chuyên môn</dt><dd>{detail.specialization}</dd>
        <dt>Kinh nghiệm giảng dạy</dt><dd>{detail.teachingExperience}</dd>
        <dt>Giới thiệu</dt><dd>{detail.introduction}</dd>
        <dt>Ngày gửi</dt><dd>{formatDate(detail.submittedAt)}</dd>
        <dt>Mã người xét duyệt</dt><dd>{detail.reviewedBy ?? "Chưa xét duyệt"}</dd>
        <dt>Ngày xét duyệt</dt><dd>{detail.reviewedAt ? formatDate(detail.reviewedAt) : "Chưa xét duyệt"}</dd>
        <dt>Lý do từ chối</dt><dd>{detail.rejectionReason ?? "Không có"}</dd>
      </dl>
      {detail.status === "PENDING" && <>
        <button disabled={busy || !reviewable} onClick={() => void run(() => review("approve"))}>Phê duyệt hồ sơ</button>
        <form key={detail.id} onSubmit={event => {
          event.preventDefault(); const reason = String(new FormData(event.currentTarget).get("reason") ?? "");
          void run(() => review("reject", reason));
        }}><fieldset disabled={busy || !reviewable}><label>Lý do từ chối<textarea name="reason" required rows={3} /></label>
          <button type="submit">Từ chối hồ sơ</button>
        </fieldset></form>
      </>}
    </section>}
  </section>;
}
