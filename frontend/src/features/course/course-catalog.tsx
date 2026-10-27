"use client";
import Link from 'next/link';
import { useState } from 'react';
import { Icon } from '../../components/site-shell';
import { discoveryCourses, previewCategories, searchText, tuitionLabel } from './discovery-preview';
import styles from './discovery.module.css';

export function CourseCatalog() {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const terms = searchText(query).split(/\s+/).filter(Boolean);
  const categoryName = previewCategories.find(item => item.id === category)?.name;
  const courses = discoveryCourses.filter(course => (!categoryName || course.category === categoryName) && terms.every(term => searchText(`${course.title} ${course.description} ${course.teacher} ${course.category}`).includes(term)));
  return <>
    <section className={styles.filters} aria-label="Tìm kiếm và lọc khóa học">
      <label>Tìm khóa học<input type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="Tên khóa học, gia sư hoặc từ khóa…" /></label>
      <label>Danh mục<select value={category} onChange={event => setCategory(event.target.value)}><option value="">Tất cả danh mục</option>{previewCategories.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    </section>
    <div className={styles.results}><p role="status">{courses.length} khóa học minh họa phù hợp</p>{(query || category) && <button className="ui-button ui-button-secondary" onClick={() => {setQuery(''); setCategory('');}}>Xóa bộ lọc</button>}</div>
    {courses.length ? <div className={styles.grid}>{courses.map(course => <article className={styles.card} key={course.id}>
      <div className={course.tone === 'cyan' ? styles.cyan : styles.indigo}><Icon kind="book" /><span>{course.category}</span></div>
      <div className={styles.cardBody}><span className={styles.tag}>Khóa học mẫu</span><h2><Link href={`/courses/${course.id}`}>{course.title}</Link></h2><p className={styles.teacher}><Icon kind="person" />{course.teacher}</p><p>{course.description}</p>
        <div className={styles.schedule}><Icon kind="check" /><div>{course.schedule}<small>Giờ Việt Nam · {course.sessionCount} buổi · Lịch minh họa</small></div></div>
        <div className={styles.cardBottom}><div><small>Học phí mẫu / cả khóa</small><strong>{tuitionLabel(course.tuition)}</strong></div><Link href={`/courses/${course.id}`} aria-label={`Xem chi tiết ${course.title}`}>Chi tiết →</Link></div>
      </div>
    </article>)}</div> : <section className={styles.empty}><Icon kind="book" /><h2>Không tìm thấy khóa học phù hợp</h2><p>Thử từ khóa khác hoặc chọn tất cả danh mục. Đây là kết quả tìm kiếm, không phải danh sách khóa học đã đăng ký.</p><button className="ui-button ui-button-primary" onClick={() => {setQuery('');setCategory('');}}>Đặt lại tìm kiếm</button></section>}
  </>;
}