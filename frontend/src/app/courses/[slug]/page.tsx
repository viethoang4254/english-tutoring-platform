import Link from 'next/link';
import { notFound } from 'next/navigation';
import { SiteHeader, SiteFooter, Icon } from '../../../components/site-shell';
import { discoveryCourses, tuitionLabel } from '../../../features/course/discovery-preview';
import styles from '../../../features/course/discovery.module.css';
export function generateStaticParams() { return discoveryCourses.map(course => ({slug:course.id})); }
export default async function CourseDetail({params}: {params:Promise<{slug:string}>}) {
 const {slug} = await params;
 const course = discoveryCourses.find(item => item.id === slug);
 if (!course) notFound();
 return <><SiteHeader /><main className={styles.page}>
  <nav className={styles.breadcrumb} aria-label="Đường dẫn"><Link href="/">Trang chủ</Link><span>/</span><Link href="/courses">Khám phá khóa học</Link><span>/</span><span aria-current="page">Chi tiết khóa học</span></nav>
  <aside className={styles.notice}><strong>Chi tiết khóa học minh họa</strong><p>Đây không phải khóa học đang mở bán hoặc khóa học bạn đã đăng ký. Tất cả thông tin dưới đây dùng để xem trước giao diện.</p></aside>
  <header className={styles.detailHeader}><span className={styles.tag}>{course.category} · Dữ liệu mẫu</span><h1>{course.title}</h1><p>{course.description}</p></header>
  <div className={styles.detailGrid}><div>
    <section className={styles.panel}><h2>Về khóa học</h2><p>{course.description}</p><p>Học trực tuyến qua Google Meet, cùng một gia sư xuyên suốt khóa học. Thông tin tham gia lớp được bảo vệ và chỉ cung cấp khi có quyền truy cập phù hợp.</p></section>
    <section className={styles.panel}><h2>Gia sư phụ trách</h2><div className={styles.teacherHeading}><span className={styles.avatar}><Icon kind="person" /></span><div><h3>{course.teacher}</h3><p>{course.specialization}</p></div></div><p>{course.experience}</p></section>
    <section className={styles.panel}><h2>Lịch học minh họa</h2><p>{course.schedule}</p><p>{course.sessionCount} buổi học · Giờ Việt Nam (UTC+7)</p><p className={styles.muted}>Chưa có ngày khai giảng hoặc lịch buổi học thực tế. Bản xem trước không cung cấp liên kết phòng học.</p></section>
  </div><aside className={styles.enroll}><span className={styles.eyebrow}>HỌC PHÍ MINH HỌA</span><strong className={styles.price}>{tuitionLabel(course.tuition)}</strong><p>Cho toàn bộ khóa học</p><hr /><dl><div><dt>Số buổi</dt><dd>{course.sessionCount} buổi mẫu</dd></div><div><dt>Sức chứa</dt><dd>Chưa có thông tin công khai</dd></div><div><dt>Trạng thái đăng ký</dt><dd>Chưa kết nối</dd></div></dl><button className="ui-button ui-button-primary" disabled>Đăng ký khóa học — chưa khả dụng</button><p>Chưa thể đăng ký hoặc thanh toán trong bản xem trước. Không có giao dịch hay lượt đăng ký nào được tạo.</p><p className={styles.muted}>Theo mô hình nền tảng, học phí được thanh toán trực tiếp cho gia sư. Quản trị viên không thu học phí.</p><Link href="/courses">← Khám phá khóa học khác</Link></aside></div>
 </main><SiteFooter /></>;
}