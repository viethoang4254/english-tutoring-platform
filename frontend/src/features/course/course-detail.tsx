"use client";
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { Icon } from '../../components/site-shell';
import { apiRequest, ApiRequestError } from '../../lib/api';
import { type PublicCourseDetail, courseDate, tuitionLabel } from './public-courses';
import styles from './discovery.module.css';
import { CourseRegistration } from './course-registration';

export function CourseDetail({courseId}: {courseId: string}) {
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState<{key: string; data?: PublicCourseDetail; missing?: boolean}>();
  const key = `${courseId}:${attempt}`;
  useEffect(() => {
    const controller = new AbortController();
    apiRequest<PublicCourseDetail>(`/public/courses/${encodeURIComponent(courseId)}`, {credentials:'omit', cache:'no-store', signal:controller.signal})
      .then(data => { if (!data) throw new Error('Missing response'); if (!controller.signal.aborted) setResult({key,data}); })
      .catch(error => { if (!controller.signal.aborted) setResult({key,missing:error instanceof ApiRequestError && error.status === 404}); });
    return () => controller.abort();
  }, [courseId,key]);
  if (result?.key !== key) return <p role="status">Đang tải khóa học…</p>;
  if (!result.data) return <section className={styles.empty}><h1>{result.missing ? 'Không tìm thấy khóa học' : 'Chưa thể tải khóa học'}</h1><p role="alert">{result.missing ? 'Khóa học không tồn tại hoặc chưa được công khai.' : 'Vui lòng kiểm tra kết nối và thử lại.'}</p>{!result.missing && <button className="ui-button ui-button-primary" onClick={() => setAttempt(value => value + 1)}>Thử lại</button>} <Link href="/courses">← Khám phá khóa học khác</Link></section>;
  const {course,rules} = result.data;
  return <>
    <header className={styles.detailHeader}><span className={styles.tag}>{course.category.name}</span><h1>{course.name}</h1><p>Mã khóa học: {course.id}</p><p>{course.description}</p></header>
    <div className={styles.detailGrid}><div>
      <section className={styles.panel}><h2>Về khóa học</h2><p>{course.description}</p></section>
      <section className={styles.panel}><h2>Gia sư phụ trách</h2><div className={styles.teacherHeading}><span className={styles.avatar}><Icon kind="person" /></span><h3>{course.teacher.fullName}</h3></div></section>
      <section className={styles.panel}><h2>Lịch học dự kiến</h2><p>Dự kiến bắt đầu: {courseDate(course.plannedStartDate)}</p><p>{course.sessionCount} buổi học · Múi giờ: {course.timezone}</p>{rules.length ? <ul>{rules.map(rule => <li key={rule.id}>{rule.dayOfWeek === 7 ? 'Chủ nhật' : `Thứ ${rule.dayOfWeek + 1}`} · {rule.startTime}–{rule.endTime}</li>)}</ul> : <p>Chưa có lịch học hằng tuần.</p>}<p className={styles.muted}>Đây là lịch hằng tuần dự kiến. Lịch từng buổi có thể thay đổi; thông tin tham gia và nội dung học được bảo vệ.</p></section>
    </div><aside className={styles.enroll}><span className={styles.eyebrow}>HỌC PHÍ GỐC</span><strong className={styles.price}>{tuitionLabel(course.tuition.amount)}</strong><p>Cho toàn bộ khóa học; chưa phải báo giá thanh toán.</p><hr /><dl><div><dt>Số buổi</dt><dd>{course.sessionCount}</dd></div></dl><CourseRegistration courseId={course.id} /><Link href="/courses">← Khám phá khóa học khác</Link></aside></div>
  </>;
}
