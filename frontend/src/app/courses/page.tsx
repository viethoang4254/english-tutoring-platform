import Link from 'next/link';
import { PublicLayout } from '../../components/site-shell';
import { CourseCatalog } from '../../features/course/course-catalog';
import styles from '../../features/course/discovery.module.css';
export default function CoursesPage() {
 return <PublicLayout><main className={styles.page}>
  <nav className={styles.breadcrumb} aria-label="Đường dẫn"><Link href="/">Trang chủ</Link><span aria-hidden="true">/</span><span aria-current="page">Khám phá khóa học</span></nav>
  <header className={styles.hero}><div><p className={styles.eyebrow}>HỌC CÙNG GIA SƯ</p><h1>Tìm khóa học.<br /><span>Mở thêm cơ hội.</span></h1><p>Khám phá cách học tiếng Anh trực tuyến cùng gia sư, theo lịch trình phù hợp với bạn.</p></div><div className={styles.heroNote}><span>01</span><strong>Chọn một khóa học</strong><p>Tìm hiểu gia sư, lịch học và học phí cho cả khóa trước khi đăng ký.</p></div></header>
  <aside className={styles.notice}><strong>Khóa học công khai</strong><p>Thông tin từ các khóa học đã được công khai. Đăng ký và thanh toán chưa được tích hợp.</p></aside>
  <CourseCatalog />
 </main></PublicLayout>;
}