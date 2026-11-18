import { PublicLayout } from "../../components/site-shell";
import { ContactForm } from "../../features/contact/contact-form";
import { ContactInformation, ContactMap, contactInformation } from "../../features/contact/contact-information";
import styles from "../public-pages.module.css";
import Link from "next/link";
export const metadata={title:"Liên hệ | Tiếng Anh cùng gia sư"};
export default function Contact(){return <PublicLayout><main className={styles.page}>
  <nav aria-label="Đường dẫn"><Link href="/">Trang chủ</Link><span aria-hidden="true"> / </span><span aria-current="page">Liên hệ</span></nav>
  <p className={styles.eyebrow}>CHÚNG TÔI LẮNG NGHE</p><h1>Liên hệ</h1>
  <p className={styles.lead}>Gửi câu hỏi về việc sử dụng nền tảng. Không gửi mật khẩu, mã xác thực, thông tin ngân hàng hoặc liên kết lớp học riêng tư.</p>
  <div className={styles.contactGrid}>
    <section className={styles.contactIntro} aria-labelledby="contact-info"><p className={styles.eyebrow}>KẾT NỐI VÀ CHIA SẺ</p><h2 id="contact-info">Giữ liên lạc với chúng tôi</h2>
      <p>Bạn có câu hỏi về cách học hoặc sử dụng nền tảng? Hãy chia sẻ nhu cầu qua biểu mẫu. Thông tin giúp chúng tôi hiểu rõ vấn đề bạn đang gặp.</p>
      <ContactInformation/>
      <p>Với lịch học hoặc xác nhận học phí, hãy kiểm tra thông tin khóa học và trạng thái đăng ký trong tài khoản của bạn.</p>
    </section>
    <section className={styles.card} aria-labelledby="contact-form" id="gui-tin-nhan"><h2 id="contact-form">Gửi tin nhắn</h2><p>Các trường có dấu * là bắt buộc.</p><ContactForm/></section>
  </div>
  <ContactMap address={contactInformation.address} embedUrl={contactInformation.mapEmbedUrl}/>
  <section className={styles.consultation} aria-labelledby="consultation"><div><p className={styles.eyebrow}>BẮT ĐẦU TỪ MỤC TIÊU CỦA BẠN</p><h2 id="consultation">Đăng ký nhận tư vấn</h2>
    <p>Để lại thông tin để chúng tôi có thể hỗ trợ bạn.</p></div>
    <div className={styles.actions}><a href="#gui-tin-nhan">Gửi nhu cầu tư vấn <span aria-hidden="true">↗</span></a></div>
  </section>
</main></PublicLayout>;}
