"use client";
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { Icon } from '../../components/site-shell';
import { apiRequest } from '../../lib/api';
import { type PublicCoursePage, courseDate, searchText, tuitionLabel } from './public-courses';
import styles from './discovery.module.css';

export function CourseCatalog({basePath="/courses"}:{basePath?:string}) {
  const internal = basePath !== "/courses";
  // The Student catalog mounts after the session/profile check, including on refresh.
  const [initial] = useState(() => new URLSearchParams(internal && typeof window !== "undefined" ? window.location.search : ""));
  const requestedPage = Number(initial.get("page"));
  const [page, setPage] = useState(Number.isSafeInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0);
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const refresh = () => { if (document.visibilityState === 'visible') setAttempt(value => value + 1); };
    document.addEventListener('visibilitychange', refresh);
    return () => document.removeEventListener('visibilitychange', refresh);
  }, []);
  const [result, setResult] = useState<{key: string; data?: PublicCoursePage; error?: boolean}>();
  const [query, setQuery] = useState(initial.get('q') ?? '');
  const [category, setCategory] = useState(initial.get('category') ?? '');
  useEffect(() => {
    if (!internal || window.location.pathname !== basePath) return;
    const filters = new URLSearchParams();
    if (page) filters.set("page",String(page));
    if (query) filters.set("q",query);
    if (category) filters.set("category",category);
    const url = filters.size ? basePath + "?" + filters : basePath;
    if (window.location.pathname + window.location.search !== url) window.history.replaceState(null,"",url);
  },[internal,basePath,page,query,category]);
  const key = `${page}:${attempt}`;
  useEffect(() => {
    const controller = new AbortController();
    // An immediately unmounted effect should not start an HTTP request.
    queueMicrotask(() => {
      if (controller.signal.aborted) return;
      apiRequest<PublicCoursePage>(`/public/courses?page=${page}&size=20`, {credentials:'omit', cache:'no-store', signal:controller.signal})
      .then(data => { if (!data) throw new Error('Missing response'); if (!controller.signal.aborted) setResult({key,data}); })
      .catch(() => { if (!controller.signal.aborted) setResult({key,error:true}); });
    });
    return () => controller.abort();
  }, [page,key]);
  if (result?.key !== key) return <p role="status">Đang tải khóa học…</p>;
  if (result.error || !result.data) return <section className={styles.empty}><p role="alert">Chưa thể tải khóa học. Vui lòng thử lại.</p><button className="ui-button ui-button-primary" onClick={() => setAttempt(value => value + 1)}>Thử lại</button></section>;
  const data = result.data;
  const categories = [...new Map(data.items.map(course => [course.category.id,course.category])).values()];
  const terms = searchText(query).split(/\s+/).filter(Boolean);
  const courses = data.items.filter(course => (!category || course.category.id === category) && terms.every(term => searchText(`${course.name} ${course.description} ${course.teacher.fullName} ${course.category.name}`).includes(term)));
  function resetFilters() { setQuery(''); setCategory(''); }
  function changePage(next: number) { resetFilters(); setPage(next); }
  return <>
    <section className={styles.filters} aria-label="Tìm kiếm và lọc trên trang hiện tại">
      <label>Tìm khóa học<input type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Tìm trên trang hiện tại…" /></label>
      <label>Danh mục<select value={category} onChange={event => setCategory(event.target.value)}><option value="">Tất cả danh mục trên trang</option>{categories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    </section>
    <div className={styles.results}><p role="status">{courses.length} khóa học trên trang {data.page + 1}</p>{(query || category) && <button className="ui-button ui-button-secondary" onClick={resetFilters}>Xóa bộ lọc</button>}</div>
    {courses.length ? <div className={styles.grid}>{courses.map(course => <article className={styles.card} key={course.id}>
      <div className={styles.indigo}><Icon kind="book" /><span>{course.category.name}</span></div>
      <div className={styles.cardBody}><span className={styles.tag}>Mã khóa học: {course.id}</span><h2><Link href={`${basePath}/${course.id}`}>{course.name}</Link></h2><p className={styles.teacher}><Icon kind="person" />{course.teacher.fullName}</p><p>{course.description}</p>
        <div className={styles.schedule}><Icon kind="check" /><div>Dự kiến bắt đầu: {courseDate(course.plannedStartDate)}<small>{course.timezone} · {course.sessionCount} buổi</small></div></div>
        <div className={styles.cardBottom}><div><small>Học phí gốc / cả khóa</small><strong>{tuitionLabel(course.tuition.amount)}</strong></div><Link href={`${basePath}/${course.id}`} aria-label={`Xem chi tiết ${course.name}`}>Chi tiết →</Link></div>
      </div>
    </article>)}</div> : <section className={styles.empty}><Icon kind="book" /><h2>{data.items.length ? 'Không tìm thấy khóa học phù hợp' : 'Chưa có khóa học trên trang này'}</h2>{(query || category) && <button className="ui-button ui-button-primary" onClick={resetFilters}>Đặt lại tìm kiếm</button>}</section>}
    <nav className={styles.results} aria-label="Phân trang khóa học"><button className="ui-button ui-button-secondary" disabled={page === 0} onClick={() => changePage(page - 1)}>Trang trước</button><span>Trang {data.page + 1}</span><button className="ui-button ui-button-secondary" disabled={!data.hasNext} onClick={() => changePage(page + 1)}>Trang sau</button></nav>
  </>;
}
