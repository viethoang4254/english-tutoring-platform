import Link from 'next/link';
import { notFound } from 'next/navigation';
import { SiteHeader, SiteFooter } from '../../../components/site-shell';
import { CourseDetail } from '../../../features/course/course-detail';
import styles from '../../../features/course/discovery.module.css';
export default async function CourseDetailPage({params}: {params:Promise<{slug:string}>}) {
  const {slug} = await params;
  if (!/^[1-9][0-9]*$/.test(slug) || slug.length > 19 || BigInt(slug) > BigInt('9223372036854775807')) notFound();
  return <><SiteHeader /><main className={styles.page}>
    <nav className={styles.breadcrumb} aria-label="Đường dẫn"><Link href="/">Trang chủ</Link><span>/</span><Link href="/courses">Khám phá khóa học</Link><span>/</span><span aria-current="page">Chi tiết khóa học</span></nav>
    <CourseDetail key={slug} courseId={slug} />
  </main><SiteFooter /></>;
}
