"use client";
import { outlineFields,type OutlineField } from "./course-outline";
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { Icon } from '../../components/site-shell';
import { apiUrl, apiRequest, ApiRequestError } from '../../lib/api';
import { type PublicCourseDetail, courseDate, tuitionLabel } from './public-courses';
import styles from './discovery.module.css';
import { CourseRegistration } from './course-registration';

const links=[["tong-quan","Tổng quan"],["lo-trinh","Lộ trình"],["chi-tiet","Chi tiết"],["dang-ky","Đăng ký"]];

export function CourseDetail({courseId,basePath="/courses"}: {courseId: string;basePath?:string}) {
  const [section,setSection]=useState("tong-quan");
  useEffect(()=>{
    const sync=()=>{
      const hash=window.location.hash.slice(1);
      setSection(links.some(([id])=>id===hash)?hash:"tong-quan");
    };
    sync();window.addEventListener("hashchange",sync);return ()=>window.removeEventListener("hashchange",sync);
  },[]);
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState<{key: string; data?: PublicCourseDetail; missing?: boolean}>();
  const key = `${courseId}:${attempt}`;
  useEffect(() => {
    const controller = new AbortController();
    queueMicrotask(() => {
      if (controller.signal.aborted) return;
      apiRequest<PublicCourseDetail>(`/public/courses/${encodeURIComponent(courseId)}`, {credentials:'omit', cache:'no-store', signal:controller.signal})
        .then(data => { if (!data) throw new Error('Missing response'); if (!controller.signal.aborted) setResult({key,data}); })
        .catch(error => { if (!controller.signal.aborted) setResult({key,missing:error instanceof ApiRequestError && error.status === 404}); });
    });
    return () => controller.abort();
  }, [courseId,key]);
  useEffect(() => {
    if (result?.key !== key || !result.data) return;
    const target = window.location.hash.slice(1);
    if (links.some(([id]) => id === target)) document.getElementById(target)?.scrollIntoView();
  }, [key, result]);
  if (result?.key !== key) return <p role="status">Đang tải khóa học…</p>;
  if (!result.data) return <section className={styles.empty}><h1>{result.missing ? 'Không tìm thấy khóa học' : 'Chưa thể tải khóa học'}</h1><p role="alert">{result.missing ? 'Khóa học không tồn tại hoặc chưa được công khai.' : 'Vui lòng kiểm tra kết nối và thử lại.'}</p>{!result.missing && <button className="ui-button ui-button-primary" onClick={() => setAttempt(value => value + 1)}>Thử lại</button>} <Link href={basePath}>← Khám phá khóa học khác</Link></section>;
  const {course,rules,outline} = result.data;
  return <div className={styles.courseSpread}>
    <div className={styles.courseBody}>
      <header className={styles.introBand}><span className={styles.tag}>{course.category.name}</span>
        <h1>{course.name}</h1><p>{outline?.introduction || course.description}</p>
        {outline?.requirements && <p><strong>Yêu cầu đầu vào · </strong>{outline.requirements}</p>}
        <div className={styles.byline}><Icon kind="person"/>{course.teacher.fullName}</div>
      </header>
      <nav className={styles.sectionTabs} aria-label="Nội dung khóa học">{links.map(([id,label])=><a key={id} href={'#'+id} aria-current={section===id?'location':undefined}>{label}</a>)}</nav>
      <section id="tong-quan" className={styles.panel}><h2>Về khóa học</h2><p>{course.description}</p>
        {outline && <div className={styles.goalGrid}>{(["objectives","audience","outcomes"] as OutlineField[]).map(key=>outline[key] && <article key={key}><Icon kind={key==="audience"?"person":"check"}/><h3>{outlineFields[key]}</h3><p>{outline[key]}</p></article>)}</div>}</section>
      <section id="lo-trinh" className={styles.panel}><h2>Lộ trình học</h2>
        {outline?.stages.length ? <><p>{outline.stages.length} giai đoạn · {outline.stages.reduce((n,s)=>n+s.topics.length,0)} chủ đề · {course.sessionCount} buổi học dự kiến</p>
          {outline.stages.map((stage,i)=><details className={styles.stage} key={i}><summary>{i+1}. {stage.title}</summary><ol>{stage.topics.map((topic,j)=><li key={j}><strong>{topic.title}</strong><p>{topic.description}</p></li>)}</ol></details>)}</> : <p>Chưa có đề cương công khai.</p>}</section>
      <section id="chi-tiet" className={styles.panel}><h2>Chi tiết lớp học</h2><p>{course.sessionCount} buổi học · Múi giờ: {course.timezone}</p><p>Mã khóa học: {course.id}</p><Link href={basePath}>← Khám phá khóa học khác</Link></section>
    </div>
    <aside id="dang-ky" className={styles.registrationCard}>
      {course.hasCover ? <img className={styles.coverImage} src={apiUrl(`/public/courses/${course.id}/cover`)} alt={`Ảnh bìa: ${course.name}`}/> : <div className={styles.courseCover} aria-hidden="true"><Icon kind="book"/><span>{course.category.name}</span><strong>{course.name}</strong></div>}
      <small>Học phí cả khóa</small><strong className={styles.price}>{tuitionLabel(course.tuition.amount)}</strong>
      <CourseRegistration courseId={course.id}/>
      <div className={styles.facts}><p>Dự kiến bắt đầu: {courseDate(course.plannedStartDate)}</p>
        <h3>Lịch học hằng tuần</h3>{rules.length ? <ul>{rules.map(rule=><li key={rule.id}>{rule.dayOfWeek===7?'Chủ nhật':`Thứ ${rule.dayOfWeek+1}`} · {rule.startTime}–{rule.endTime}</li>)}</ul> : <p>Chưa có lịch học hằng tuần.</p>}
        <dl><div><dt>Số buổi trực tuyến</dt><dd>{course.sessionCount}</dd></div><div><dt>Gia sư</dt><dd>{course.teacher.fullName}</dd></div></dl>
      </div>
    </aside></div>;
}
