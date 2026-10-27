import { Icon, SiteHeader, SiteFooter } from "../components/site-shell";
import Link from "next/link";
import styles from "./landing.module.css";

export default function Home() {
  return <div className={styles.landing} id="trang-chu">
    <a className={styles.skip} href="#noi-dung">Đến nội dung chính</a>
    <SiteHeader home />
    <main className={styles.main} id="noi-dung">
      <section className={styles.hero} aria-labelledby="hero-title"><div className={styles.heroInner}>
        <div className={styles.heroCopy}>
          <p className={styles.eyebrow}><span /> KẾT NỐI GIA SƯ · HỌC TRỰC TUYẾN</p>
          <h1 id="hero-title">Học tiếng Anh.<br /><em>Có người đồng hành.</em></h1>
          <p className={styles.description}>Kết nối với gia sư, học theo từng khóa và tham gia các buổi học trực tuyến qua Google Meet. Một hành trình có lịch học, có hướng dẫn và có sự tương tác.</p>
          <div className={styles.actions}><a className={styles.primary} href="#khoa-hoc">Tìm hiểu khóa học <span aria-hidden="true">→</span></a><Link className={styles.secondary} href="/register">Đăng ký học viên</Link></div>
          <p className={styles.heroNote}>Bắt đầu bằng tài khoản của bạn. Học cùng người hướng dẫn.</p>
        </div>
        <div className={styles.art} role="img" aria-label="Minh họa gia sư và học viên trong một buổi học tiếng Anh trực tuyến">
          <div className={styles.orbit} />
          <div className={styles.lesson}><div className={styles.windowBar}><i /><i /><i /><span>Lớp học trực tuyến</span></div>
            <div className={styles.people}>
              <div className={styles.teacherPortrait}><svg viewBox="0 0 200 220" aria-hidden="true"><path fill="#46dfba" d="M0 0h200v220H0z"/><circle cx="164" cy="40" r="52" fill="#a4f5d9"/><path d="M28 220v-33c0-69 144-69 144 0v33" fill="#252457"/><path d="m73 148 27 24 27-24-7-32H80z" fill="#e4a17d"/><ellipse cx="100" cy="84" rx="43" ry="53" fill="#ffc6a2"/><path d="M56 91c-16-75 67-91 87-40l-4 37-10-36c-22 23-43 19-65 12z" fill="#262045"/><path d="M83 91h1m32 0h1" stroke="#262045" strokeWidth="5" strokeLinecap="round"/><path d="M88 113q12 10 24 0" fill="none" stroke="#a25249" strokeWidth="3"/><path d="m68 151 32 22-20 30-18-41m70-11-32 22 20 30 18-41" fill="#eeeaff"/></svg><span>Gia sư</span></div>
              <div className={styles.studentPortrait}><svg viewBox="0 0 200 220" aria-hidden="true"><path fill="#b8a0ff" d="M0 0h200v220H0z"/><circle cx="30" cy="40" r="65" fill="#d5c6ff"/><path d="M38 130C16 17 170 6 165 128l-12 56H41z" fill="#30234b"/><path d="M24 220v-26c0-66 152-66 152 0v26" fill="#ffdb82"/><path d="m76 145 24 22 24-22-7-27H83z" fill="#dc9574"/><ellipse cx="100" cy="88" rx="41" ry="50" fill="#f8ba95"/><path d="M55 83c3-65 83-72 92 0-23-4-43-21-49-35-6 18-20 29-43 35" fill="#30234b"/><path d="M82 91h1m33 0h1" stroke="#30234b" strokeWidth="5" strokeLinecap="round"/><path d="M88 113q12 10 24 0" fill="none" stroke="#a25249" strokeWidth="3"/><path d="M51 81v31m98-31v31m0-5v15h-25" fill="none" stroke="#faf9ff" strokeWidth="7" strokeLinecap="round"/></svg><span>Học viên</span></div>
            </div>
            <div className={styles.callBar}><span>Học cùng nhau, tiến bộ cùng nhau</span><b aria-hidden="true">•••</b></div>
          </div>
          <div className={styles.floatingNote}><span className={styles.smallIcon}><Icon kind="video" /></span><span>Gặp nhau qua<strong>Google Meet</strong></span></div>
          <div className={styles.bookNote}><Icon kind="book" /><span>Từng buổi học.<br />Thêm tự tin.</span></div>
          <span className={styles.spark} aria-hidden="true">✳</span>
        </div>
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
          <article><Icon kind="check" /><h3>03. Học qua bài tập và phản hồi</h3><p>Định hướng học tập gắn với bài tập theo buổi học và nhận xét của gia sư, giúp học viên hiểu phần cần cải thiện.</p></article>
        </div>
      </div></section>
      <section className={styles.courses} id="khoa-hoc" aria-labelledby="courses-title"><div className={styles.container}><div className={styles.coursePanel}>
        <div><p className={styles.kicker}>KHÓA HỌC TRÊN NỀN TẢNG</p><h2 id="courses-title">Chuẩn bị cho hành trình<br />học tiếng Anh của bạn.</h2><p>Khám phá giao diện danh sách khóa học với dữ liệu minh họa. Chức năng đăng ký khóa học và thanh toán chưa khả dụng.</p><Link className={styles.primary} href="/courses">Khám phá khóa học <span aria-hidden="true">→</span></Link></div>
        <div className={styles.courseSymbol} aria-hidden="true"><Icon kind="book" /><span>Học có lịch trình.<br />Có gia sư đồng hành.</span></div>
      </div></div></section>
    </main>
    <SiteFooter />

  </div>;
}
