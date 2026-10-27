"use client";

import { useState } from "react";
import { authorized, type Session } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import styles from "./admin-categories.module.css";

type Category = { id: string; name: string; active: boolean; createdAt: string; updatedAt: string };
type Page = { items: Category[]; page: number; size: number; hasNext: boolean };
const root = "/admin/categories";
function validCategory(value: Category) {
  return value && typeof value.id === "string" && /^[1-9][0-9]*$/.test(value.id) &&
    typeof value.name === "string" && typeof value.active === "boolean" &&
    typeof value.createdAt === "string" && typeof value.updatedAt === "string";
}
export function AdminCategories({ session, busy, run }: {
  session: Session; busy: boolean; run: (work: () => Promise<void>) => Promise<void>;
}) {
  const [result, setResult] = useState<Page | null>(null);
  const [filter, setFilter] = useState("");
  const [name, setName] = useState("");
  const [editing, setEditing] = useState<string | null>(null);
  const [notice, setNotice] = useState("");
  const [blocked, setBlocked] = useState(false);

  async function load(page = 0, active = filter) {
    setResult(null);
    const data = await authorized<Page>(session.accessToken,
      `${root}?page=${page}&size=20${active ? `&active=${active}` : ""}`);
    if (!data || data.page !== page || data.size !== 20 || typeof data.hasNext !== "boolean" ||
        !Array.isArray(data.items) || !data.items.every(validCategory)) throw new Error("Invalid category response");
    setResult(data); setBlocked(false);
  }
  function refresh(page = 0, active = filter) {
    void run(async () => {
      setNotice("");
      try { await load(page, active); }
      catch (error) { setBlocked(true); throw error; }
    });
  }
  function mutate(id: string | null, body: { name: string } | { active: boolean }) {
    void run(async () => {
      setNotice("");
      try {
        const saved = await authorized<Category>(session.accessToken, id ? `${root}/${id}` : root, body, id ? "PATCH" : "POST");
        if (!saved || !validCategory(saved) || (id && saved.id !== id)) throw new Error("Invalid category response");
        setName(""); setEditing(null);
        setNotice("Đã lưu danh mục.");
        await load(result?.page ?? 0);
      } catch (error) {
        if (error instanceof ApiRequestError && error.status === 409) {
          setNotice("Tên danh mục đã được sử dụng, kể cả danh mục ngừng hoạt động."); return;
        }
        if (error instanceof ApiRequestError && error.status === 400) {
          setNotice("Thông tin chưa hợp lệ. Vui lòng kiểm tra tên danh mục."); return;
        }
        setBlocked(true); setResult(null);
        setNotice("Chưa xác nhận được kết quả. Hãy tải lại danh sách để kiểm tra trước khi tiếp tục. Yêu cầu không được tự động gửi lại.");
        throw error;
      }
    });
  }
  function submit(event: React.FormEvent) {
    event.preventDefault();
    const trimmed = name.replace(/^ +| +$/g, "");
    if (!trimmed || [...trimmed].length > 100 || [...trimmed].some(c => {
      const code = c.codePointAt(0)!;
      return code < 32 || (code >= 127 && code <= 159) || (code >= 0xd800 && code <= 0xdfff);
    })) { setNotice("Tên danh mục phải có 1–100 ký tự và không chứa ký tự điều khiển."); return; }
    mutate(editing, { name: trimmed });
  }
  return <section className={styles.panel} aria-label="Quản lý danh mục">
    <div className={styles.toolbar}>
      <label>Trạng thái<select value={filter} disabled={busy} onChange={e => {
        setFilter(e.target.value); setEditing(null); setName(""); refresh(0, e.target.value);
      }}><option value="">Tất cả</option><option value="true">Đang hoạt động</option><option value="false">Ngừng hoạt động</option></select></label>
      <button className="ui-button ui-button-secondary" disabled={busy} onClick={() => refresh(result?.page ?? 0)}>Tải lại danh sách</button>
    </div>
    {notice && <p role="status">{notice}</p>}
    <form onSubmit={submit} className={styles.toolbar}>
      <label>{editing ? "Tên danh mục mới" : "Tên danh mục"}<input value={name} disabled={busy || blocked} onChange={e => setName(e.target.value)} placeholder="Ví dụ: Tiếng Anh giao tiếp" /></label>
      <button className="ui-button ui-button-primary" disabled={busy || blocked}>{editing ? "Lưu tên" : "Tạo danh mục"}</button>
      {editing && <button type="button" className="ui-button ui-button-secondary" disabled={busy} onClick={() => { setEditing(null); setName(""); }}>Hủy</button>}
    </form>
    {!result && !busy && <p>Chọn tải lại danh sách để xem dữ liệu danh mục.</p>}
    {result && <>
      {result.items.length === 0 && <p>Không có danh mục trên trang này.</p>}
      <ul className={styles.list}>{result.items.map(item => <li key={item.id}>
        <div><strong>{item.name}</strong><p>{item.active ? "Đang hoạt động" : "Ngừng hoạt động"}</p></div>
        <div className={styles.actions}>
          <button className="ui-button ui-button-secondary" disabled={busy || blocked} onClick={() => { setEditing(item.id); setName(item.name); setNotice(""); }}>Đổi tên</button>
          <button className="ui-button ui-button-secondary" disabled={busy || blocked} onClick={() => mutate(item.id, { active: !item.active })}>{item.active ? "Ngừng hoạt động" : "Kích hoạt"}</button>
        </div>
      </li>)}</ul>
      <nav className={styles.toolbar} aria-label="Phân trang danh mục">
        <button className="ui-button ui-button-secondary" disabled={busy || result.page === 0} onClick={() => refresh(result.page - 1)}>Trang trước</button>
        <span>Trang {result.page + 1}</span>
        <button className="ui-button ui-button-secondary" disabled={busy || !result.hasNext} onClick={() => refresh(result.page + 1)}>Trang sau</button>
      </nav>
    </>}
  </section>;
}
