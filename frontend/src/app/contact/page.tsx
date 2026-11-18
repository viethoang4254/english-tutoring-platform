import { PublicLayout } from "../../components/site-shell";
import { ContactForm } from "../../features/contact/contact-form";
import styles from "../public-pages.module.css";
import Link from "next/link";
export const metadata={title:"Liên hệ | Tiếng Anh cùng gia sư"};
export default function Contact(){return <PublicLayout><main className={styles.page}>
  <nav aria-label="Đường dẫn"><Link href="/">Trang chủ</Link><span aria-hidden="true"> / </span><span aria-current="page">Liên hệ</span></nav>
  <p className={styles.eyebrow}>CHÚNG TÔI LẮNG NGHE</p><h1>Liên hệ</h1>
  <p className={styles.lead}>Gửi câu hỏi về việc sử dụng nền tảng. Không gửi mật khẩu, mã xác thực, thông tin ngân hàng hoặc liên kết lớp học riêng tư.</p>
  <div className={styles.contactGrid}>
    <section className={styles.card} aria-labelledby="contact-info"><h2 id="contact-info">Thông tin liên hệ</h2>
      <p>Bạn cần hỗ trợ sử dụng nền tảng? Gửi lời nhắn qua biểu mẫu bên cạnh để được tiếp nhận khi kênh hỗ trợ đã được cấu hình.</p>
      <dl><dt>Kênh hỗ trợ</dt><dd>Biểu mẫu liên hệ trên website</dd><dt>Email, điện thoại và địa chỉ văn phòng</dt><dd>Chưa công bố thông tin liên hệ chính thức.</dd></dl>
      <p>Với lịch học hoặc xác nhận học phí, hãy kiểm tra thông tin khóa học và trạng thái đăng ký trong tài khoản của bạn.</p>
    </section>
    <section className={styles.card} aria-labelledby="contact-form"><h2 id="contact-form">Gửi lời nhắn</h2><ContactForm/></section>
  </div>
  <section className={styles.card} aria-labelledby="consultation"><h2 id="consultation">Tìm hướng học phù hợp</h2>
    <p>Tham khảo mục tiêu, trình độ đầu vào, lịch học và học phí của từng khóa. Bạn có thể nêu nhu cầu học trong lời nhắn; không cần gửi thông tin thanh toán.</p>
    <div className={styles.actions}><Link href="/courses">Khám phá khóa học</Link><Link href="/about">Tìm hiểu nền tảng</Link></div>
  </section>
</main></PublicLayout>;}
