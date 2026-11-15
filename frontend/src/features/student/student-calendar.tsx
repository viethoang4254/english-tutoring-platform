"use client";

import Link from "next/link";
import { MeetingAccess } from "../course/meeting-access";
import { useEffect, useState } from "react";
import { Icon } from "../../components/site-shell";
import { useSession } from "../auth/session-provider";
import { authorized } from "../auth/browser-api";
import { ApiRequestError } from "../../lib/api";
import { dashboardTimeZone, dayKey, monthDays, shiftDay, shiftMonth, occursOn } from "./calendar-dates";
import styles from "./student-dashboard.module.css";

export type StudentCalendarSession = {
  id: string; courseId: string; courseTitle: string; teacherDisplayName: string;
  sessionNumber: number; title: string | null; startsAt: string; endsAt: string;
  timezone: string; status: "SCHEDULED" | "CANCELLED"; rescheduled: boolean;
};
type CalendarPage = { items: StudentCalendarSession[]; page: number; size: number; hasNext: boolean };
const dateFormat = new Intl.DateTimeFormat("vi-VN", { timeZone: dashboardTimeZone, weekday: "long", day: "numeric", month: "long", year: "numeric" });
const timeFormat = new Intl.DateTimeFormat("vi-VN", { timeZone: dashboardTimeZone, hour: "2-digit", minute: "2-digit", hour12: false });
const monthFormat = new Intl.DateTimeFormat("vi-VN", { timeZone: dashboardTimeZone, month: "long", year: "numeric" });

export function StudentCalendar() {
  const { session, clearSession } = useSession();
  const [now] = useState(() => new Date());
  const today = dayKey(now);
  const [month, setMonth] = useState(today.slice(0, 7));
  const [selected, setSelected] = useState(today);
  const [attempt, setAttempt] = useState(0);
  const key = `${month}:${attempt}:${session?.accessToken}`;
  const [result, setResult] = useState<{ key: string; items: StudentCalendarSession[]; error?: "denied" | "unavailable" }>();
  const loading = result?.key !== key;
  const error = loading ? undefined : result?.error;
  const sessions = !loading && !error ? result?.items ?? [] : [];
  useEffect(() => {
    if (!session) return;
    let active = true;
    async function load() {
      const days = monthDays(month);
      const items: StudentCalendarSession[] = [];
      const ids = new Set<string>();
      for (let page = 0; ; page++) {
        const query = new URLSearchParams({ from: days[0] + "T00:00:00+07:00", to: shiftDay(days[days.length - 1], 1) + "T00:00:00+07:00", page: String(page), size: "100" });
        const response = await authorized<CalendarPage>(session!.accessToken, `/student/calendar/sessions?${query}`);
        if (!active) return;
        if (!response || response.page !== page || response.size !== 100 || !Array.isArray(response.items)
            || typeof response.hasNext !== "boolean" || (response.hasNext && response.items.length !== 100)) throw new Error("Invalid calendar page");
        for (const item of response.items) {
          if (ids.has(item.id)) throw new Error("Calendar changed during pagination");
          ids.add(item.id); items.push(item);
        }
        if (!response.hasNext) break;
      }
      if (active) setResult({ key, items });
    }
    load().catch(error => {
      if (!active) return;
      if (error instanceof ApiRequestError && error.status === 401) { clearSession(); return; }
      setResult({ key, items: [], error: error instanceof ApiRequestError && error.status === 403 ? "denied" : "unavailable" });
    });
    return () => { active = false; };
  }, [session, month, key, clearSession]);
  const selectedSessions = sessions.filter(session => occursOn(session, selected));
  const upcoming = sessions.filter(session => session.status === "SCHEDULED" && new Date(session.startsAt) > now).slice(0, 3);
  function selectDay(day: string) {
    setSelected(day);
    if (!day.startsWith(month)) { setMonth(day.slice(0, 7)); setAttempt(value => value + 1); }
  }
  function navigateMonth(offset: number) { selectDay(shiftMonth(month, offset) + "-01"); }
  return <section aria-label="Lịch học đã đăng ký">
    <div className={styles.sectionHeading}><p>Buổi học từ khóa học đang học, đã xác nhận thanh toán.</p><button className="ui-button ui-button-secondary" disabled={loading} onClick={() => setAttempt(value => value + 1)}>Tải lại lịch học</button></div>
    {error && <div role="alert"><p>{error === "denied" ? "Tài khoản không có quyền xem lịch học." : "Không thể tải lịch học. Vui lòng thử lại."}</p>{error !== "denied" && <button className="ui-button ui-button-secondary" onClick={() => setAttempt(value => value + 1)}>Thử lại lịch học</button>}</div>}
    {!loading && !error && !sessions.length && <p role="status">Chưa có buổi học trong kỳ lịch này. <Link href="/student/enrollments">Xem khóa học của tôi</Link></p>}
      <div className={styles.scheduleLayout}>
        <section className={styles.calendar} id="lich-hoc" aria-labelledby="calendar-title">
          <div className={styles.sectionHeading}><div><p className={styles.eyebrow}>CHỦ ĐỘNG THỜI GIAN</p><h2 id="calendar-title">Lịch học của tôi</h2></div><span className={styles.timezone}>Giờ Việt Nam · UTC+7</span></div>
          <div className={styles.calendarToolbar}><h3 aria-live="polite">{monthFormat.format(new Date(month + "-01T12:00:00Z"))}</h3><div><button onClick={() => selectDay(today)}>Hôm nay</button><button aria-label="Tháng trước" onClick={() => navigateMonth(-1)}>‹</button><button aria-label="Tháng sau" onClick={() => navigateMonth(1)}>›</button></div></div>
          <div className={styles.weekdays} aria-hidden="true">{["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map(day => <span key={day}>{day}</span>)}</div>
          <div aria-busy={loading} className={styles.calendarGrid} role="group" aria-label="Các ngày trong tháng">
            {monthDays(month).map(day => {
              const items = sessions.filter(session => occursOn(session, day));
              const outside = !day.startsWith(month);
              return <button key={day} className={[styles.day, outside ? styles.outside : "", selected === day ? styles.selected : "", day === today ? styles.today : ""].join(" ")} aria-pressed={selected === day} aria-current={day === today ? "date" : undefined} aria-label={dateFormat.format(new Date(day + "T12:00:00Z")) + (items.length ? ", " + items.length + " buổi học" : ", không có buổi học")} onClick={() => selectDay(day)}>
                <span>{Number(day.slice(-2))}</span>
                <span className={styles.dayEvents} aria-hidden="true">{items.map(item => <span key={item.id} className={item.status === "CANCELLED" ? styles.cancelledIndicator : styles.sessionIndicator}>{timeFormat.format(new Date(item.startsAt))}</span>)}</span>
              </button>;
            })}
          </div>
          <div className={styles.legend}><span><i /> Có buổi học</span><span><i className={styles.cancelledDot} /> Đã hủy</span><span>Nhấn vào ngày để xem lịch</span></div>
          {!loading && !error && <div className={styles.dayDetails} aria-live="polite"><h3>{dateFormat.format(new Date(selected + "T12:00:00Z"))}</h3>{selectedSessions.length ? selectedSessions.map(session => <SessionCard key={session.id} session={session} />) : <div className={styles.empty}><Icon kind="check" /><strong>Không có buổi học trong ngày này</strong><p>Chọn ngày có dấu đánh dấu để xem lịch học.</p></div>}</div>}
        </section>
        <aside className={styles.upcoming} aria-labelledby="upcoming-title"><p className={styles.eyebrow}>LỊCH HỌC</p><h2 id="upcoming-title">Sắp diễn ra</h2><p className={styles.muted}>Các buổi sắp tới trong kỳ lịch đang xem</p>{loading ? <p role="status">Đang tải lịch học…</p> : error ? <p>Chưa thể tải các buổi sắp tới.</p> : upcoming.length ? upcoming.map(session => <div key={session.id} className={styles.upcomingItem}><span className={styles.dateBadge}>{new Date(session.startsAt).toLocaleDateString("vi-VN", { timeZone: dashboardTimeZone, day: "2-digit", month: "2-digit" })}</span><div><strong>{session.title || `Buổi ${session.sessionNumber}`}</strong><p>{session.courseTitle} · {session.teacherDisplayName}</p><p>{timeFormat.format(new Date(session.startsAt))} · Giờ Việt Nam</p><button className={styles.textButton} onClick={() => { selectDay(dayKey(session.startsAt)); document.getElementById("lich-hoc")?.scrollIntoView({ block: "start" }); }}>Xem trong lịch →</button></div></div>) : <p className={styles.empty}>Không có buổi học sắp tới trong kỳ lịch đang xem.</p>}<div className={styles.meetNote}><Icon kind="book" /><p>Mở khóa học để xem thông tin truy cập được cấp phép. Quyền tham gia Google Meet được kiểm tra khi bạn nhấn tham gia.</p></div></aside>
      </div>
  </section>;
}

function SessionCard({ session }: { session: StudentCalendarSession }) {
  const { session: auth } = useSession();
  return <article className={styles.sessionCard}><div className={styles.sessionTime}><strong>{timeFormat.format(new Date(session.startsAt))}</strong><span>{timeFormat.format(new Date(session.endsAt))}</span></div><div className={styles.sessionInfo}><span className={styles.muted}>{session.courseTitle}</span><h4>{session.title || `Buổi ${session.sessionNumber}`}</h4><p>{session.teacherDisplayName} · Buổi {session.sessionNumber}</p><p>Múi giờ khóa học: {session.timezone}</p><span className={session.status === "CANCELLED" ? styles.cancelled : styles.pill}>{session.status === "CANCELLED" ? "Đã hủy" : "Đã lên lịch"}</span>{session.rescheduled && <span className={styles.pill}>Đã đổi lịch</span>}</div><div className={styles.sessionActions}><Link className="ui-button ui-button-secondary" href={`/student/dashboard/courses/${session.courseId}`}>Xem khóa học</Link>{session.status === "SCHEDULED" ? <MeetingAccess key={`${auth?.accessToken}:${session.id}:${session.startsAt}`} endpoint={`/student/sessions/${session.id}/join`} /> : <button className="ui-button ui-button-secondary" disabled>Buổi học đã hủy</button>}</div></article>;
}
