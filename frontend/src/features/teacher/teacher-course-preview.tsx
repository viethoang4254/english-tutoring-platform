"use client";

import { useEffect, useRef, useState } from "react";
import { authorized, safeMessage, type Session } from "../auth/browser-api";
import { useSession } from "../auth/session-provider";
import { ApiRequestError } from "../../lib/api";
import type { Course } from "./teacher-courses";
import type { Lesson, Schedule } from "./teacher-sessions";
import styles from "./teacher-courses.module.css";

type SessionPage = { items: Lesson[]; page: number; size: number; hasNext: boolean };

export function TeacherCoursePreview({ session, courseId }: { session: Session; courseId: string }) {
  const { clearSession } = useSession();
  const [snapshot, setSnapshot] = useState<{ course: Course; schedule: Schedule; sessions: SessionPage } | null>(null);
  const [detail, setDetail] = useState<Lesson | null>(null);
  const [number, setNumber] = useState(0), [attempt, setAttempt] = useState(0);
  const [busy, setBusy] = useState(true), [message, setMessage] = useState("");
  const flight = useRef(false);
  const root = `/teacher/courses/${courseId}`;

  useEffect(() => {
    let active = true;
    async function read() {
      setBusy(true); setSnapshot(null); setDetail(null); setMessage("");
      try {
        const course = await authorized<Course>(session.accessToken, root);
        const schedule = await authorized<Schedule>(session.accessToken, `${root}/schedule`);
        const sessions = await authorized<SessionPage>(session.accessToken, `${root}/sessions?page=${number}&size=20`);
        if (!course || course.id !== courseId || typeof course.description !== "string" ||
            !schedule || schedule.courseId !== courseId || !Array.isArray(schedule.rules) ||
            !sessions || sessions.page !== number || typeof sessions.hasNext !== "boolean" || !Array.isArray(sessions.items) ||
            sessions.items.some(item => typeof item.id !== "string" || item.courseId !== courseId)) throw new Error("Invalid preview response");
        if (active) setSnapshot({ course, schedule, sessions });
      } catch (error) {
        if (active) {
          setMessage(safeMessage(error));
          if (error instanceof ApiRequestError && error.status === 401) clearSession();
        }
      } finally { if (active) setBusy(false); }
    }
    void read();
    return () => { active = false; };
  }, [session.accessToken, courseId, root, number, attempt, clearSession]);

  async function readContent(id: string) {
    if (flight.current) return;
    flight.current = true; setBusy(true); setDetail(null); setMessage("");
    try {
      const value = await authorized<Lesson>(session.accessToken, `${root}/sessions/${id}`);
      if (!value || value.id !== id || value.courseId !== courseId || !(value.content === null || typeof value.content === "string")) throw new Error("Invalid session preview");
      setDetail(value);
    } catch (error) {
      setMessage(safeMessage(error));
      if (error instanceof ApiRequestError && [401, 403, 404].includes(error.status)) setSnapshot(null);
      if (error instanceof ApiRequestError && error.status === 401) clearSession();
    } finally { flight.current = false; setBusy(false); }
  }

  const display = (value: string) => new Intl.DateTimeFormat("vi-VN", {
    timeZone: snapshot!.course.timezone, dateStyle: "medium", timeStyle: "short",
  }).format(new Date(value));

  return <section className={styles.panel} aria-label="Xem trước dành cho gia sư" aria-busy={busy}>
    <h3>5. Xem trước khóa học</h3>
    <p>Chỉ hiển thị dữ liệu đã lưu. Đây là bản xem trước riêng của gia sư, không phải trang công khai cho học viên.</p>
    <button className="ui-button ui-button-secondary" disabled={busy} onClick={() => setAttempt(attempt + 1)}>Tải lại bản xem trước</button>
    {busy && <p role="status">Đang tải bản xem trước…</p>}
    {message && <p role="alert" className={styles.notice}>{message}</p>}
    {snapshot && <>
      <h4>{snapshot.course.name}</h4>
      <p className={styles.plain}>{snapshot.course.description}</p>
      <dl className={styles.previewFacts}>
        <dt>Danh mục</dt><dd>{snapshot.course.category.name}{!snapshot.course.category.active && " (ngừng hoạt động)"}</dd>
        <dt>Học phí</dt><dd>{snapshot.course.tuition.amount} {snapshot.course.tuition.currency}</dd>
        <dt>Sĩ số</dt><dd>{snapshot.course.minStudents}–{snapshot.course.maxStudents}</dd>
        <dt>Giảm giá</dt><dd>{snapshot.course.discount ? `${snapshot.course.discount.percent}% · ${snapshot.course.discount.startAt} → ${snapshot.course.discount.endAt}` : "Không"}</dd>
        <dt>Ngày bắt đầu dự kiến</dt><dd>{snapshot.course.plannedStartDate}</dd>
        <dt>Múi giờ</dt><dd>{snapshot.course.timezone}</dd>
        <dt>Số buổi</dt><dd>{snapshot.schedule.generatedSessionCount} đã tạo / {snapshot.course.sessionCount} dự kiến</dd>
      </dl>
      <h4>Lịch tuần đã lưu</h4>
      {!snapshot.schedule.rules.length && <p>Chưa có lịch tuần.</p>}
      <ul>{snapshot.schedule.rules.map((rule, index) => <li key={index}>{rule.dayOfWeek === 7 ? "Chủ nhật" : `Thứ ${rule.dayOfWeek + 1}`}: {rule.startTime}–{rule.endTime}</li>)}</ul>
      <h4>Nội dung các buổi học</h4>
      <p>Thứ tự theo số buổi; đổi lịch không đổi số thứ tự.</p>
      {!snapshot.sessions.items.length && <p>Chưa có buổi học trên trang này.</p>}
      <ol className={styles.list}>{snapshot.sessions.items.map(item => <li key={item.id}>
        <div><strong>Buổi {item.sessionNumber}: {item.title ?? "Chưa có tiêu đề"}</strong>
          <p>{display(item.scheduledStartAt)} – {display(item.scheduledEndAt)}</p>
          <span>{item.status === "CANCELLED" ? "Đã hủy" : "Đã lên lịch"}</span>
          {detail?.id === item.id && <div className={styles.plain}><p>{detail.content ?? "Chưa có nội dung."}</p>{detail.cancellationReason && <p>Lý do hủy: {detail.cancellationReason}</p>}</div>}
        </div>
        <button className="ui-button ui-button-secondary" disabled={busy} onClick={() => void readContent(item.id)}>Đọc nội dung buổi {item.sessionNumber}</button>
      </li>)}</ol>
      <div className={styles.toolbar}>
        <button disabled={busy || number === 0} onClick={() => setNumber(number - 1)}>Xem trước: trang trước</button>
        <span>Trang {number + 1}</span>
        <button disabled={busy || !snapshot.sessions.hasNext} onClick={() => setNumber(number + 1)}>Xem trước: trang sau</button>
      </div>
    </>}
  </section>;
}
