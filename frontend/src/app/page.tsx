import { Icon, PublicLayout } from "../components/site-shell";
import Link from "next/link";
import { CourseCatalog } from "../features/course/course-catalog";
import styles from "./landing.module.css";

export default function Home() {
  return <div className={styles.landing} id="trang-chu">
    <a className={styles.skip} href="#noi-dung">Đến nội dung chính</a>
    <PublicLayout home>
    <main className={styles.main} id="noi-dung">
      <section className={styles.hero} aria-labelledby="hero-title"><div className={styles.heroInner}>
        <div className={styles.heroCopy}>
          <p className={styles.eyebrow}><span /> KẾT NỐI GIA SƯ · HỌC TRỰC TUYẾN</p>
          <h1 id="hero-title">Học tiếng Anh.<br /><em>Có người đồng hành.</em></h1>
          <p className={styles.description}>Kết nối với gia sư, học theo từng khóa và tham gia các buổi học trực tuyến qua Google Meet. Một hành trình có lịch học, có hướng dẫn và có sự tương tác.</p>
          <div className={styles.actions}><a className={styles.primary} href="#khoa-hoc">Tìm hiểu khóa học <span aria-hidden="true">→</span></a><Link className={styles.secondary} href="/register">Đăng ký học viên</Link></div>
        </div>
        <div className={styles.heroArt} aria-hidden="true"><Icon kind="book" /></div>
      </div></section>
      <section className={styles.steps} aria-labelledby="steps-title"><div className={styles.container}>
        <p className={styles.kicker}>BẮT ĐẦU HÀNH TRÌNH</p><h2 id="steps-title">Từ lần đăng ký đầu tiên<br />đến buổi học cùng gia sư</h2>
        <p className={styles.sectionIntro}>Ba bước để hiểu cách học trên nền tảng.</p>
        <ol className={styles.stepGrid}>
          <li><div className={styles.stepIcon}><span>01</span><Icon kind="person" /></div><h3>Đăng ký tài khoản học viên</h3><p>Tạo tài khoản học viên, sau đó đăng nhập bằng email và mật khẩu để bắt đầu.</p></li>
          <li><div className={styles.stepIcon}><span>02</span><Icon kind="book" /></div><h3>Chọn khóa học phù hợp</h3><p>Tìm hiểu gia sư, lịch học và học phí trước khi đăng ký cả khóa.</p></li>
          <li><div className={styles.stepIcon}><span>03</span><Icon kind="video" /></div><h3>Bắt đầu học cùng gia sư</h3><p>Khi đăng ký khóa học có hiệu lực, tham gia các buổi học theo lịch qua Google Meet.</p></li>
        </ol>
      </div></section>
      <section className={styles.benefits} id="gioi-thieu" aria-labelledby="benefits-title"><div className={styles.container}>
        <p className={styles.kicker}>MÔ HÌNH HỌC CÙNG GIA SƯ</p><h2 id="benefits-title">Một khóa học. Sự đồng hành xuyên suốt.</h2>
        <p className={styles.sectionIntro}>Nền tảng được xây dựng cho việc học có người hướng dẫn.</p>
        <div className={styles.benefitGrid}>
          <article><Icon kind="video" /><h3>01. Gặp gỡ trong lớp học</h3><p>Học tiếng Anh trực tuyến với gia sư qua Google Meet. Tương tác trong từng buổi học, thay vì tự học một mình.</p></article>
          <article><Icon kind="book" /><h3>02. Theo sát một khóa học</h3><p>Đăng ký toàn bộ khóa học do gia sư phụ trách, với các buổi học được sắp lịch và nội dung học tập theo khóa.</p></article>
          <article><Icon kind="check" /><h3>03. Theo dõi đăng ký khóa học</h3><p>Xem trạng thái đăng ký, học phí và hướng dẫn chuyển khoản VietQR cho khóa học đã chọn.</p></article>
        </div>
      </div></section>
      <section className={styles.courses} id="khoa-hoc" aria-labelledby="courses-title"><div className={styles.container}>
        <h2 id="courses-title">Khóa học trên nền tảng</h2>
        <CourseCatalog />
        <Link href="/courses">Khám phá khóa học →</Link>
      </div></section>
    </main>
    </PublicLayout>

  </div>;
}
