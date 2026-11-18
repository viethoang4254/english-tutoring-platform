import Link from "next/link";
import { PublicLayout } from "../../components/site-shell";
import styles from "../public-pages.module.css";

export const metadata = {title: "Giới thiệu | Tiếng Anh cùng gia sư"};
export default function About() {
  return <PublicLayout><main className={styles.page}>
    <p className={styles.eyebrow}>TIẾNG ANH CÙNG GIA SƯ</p>
    <h1>Học theo khóa, tiến bộ cùng gia sư</h1>
    <p className={styles.lead}>Nền tảng kết nối học viên với giáo viên tiếng Anh qua những khóa học trực tuyến có nội dung và lịch học rõ ràng.</p>
    <div className={styles.grid}>
      <section className={styles.card}><h2>Dành cho học viên</h2><p>Tìm hiểu giáo viên, mục tiêu, học phí và lịch học trước khi đăng ký. Theo dõi các buổi học trong lịch cá nhân trên website và tham gia Google Meet khi đủ điều kiện truy cập.</p></section>
      <section className={styles.card}><h2>Dành cho giáo viên</h2><p>Đăng ký hồ sơ để được xét duyệt, xây dựng khóa học của riêng mình, quản lý lịch dạy và theo dõi học viên đăng ký. Giáo viên trực tiếp nhận học phí cho khóa học mình phụ trách.</p></section>
    </div>
    <section className={styles.card}><h2>Một khóa học, một kế hoạch rõ ràng</h2><ol><li>Khám phá nội dung, học phí và lịch các buổi học.</li><li>Đăng ký cả khóa và thanh toán trực tiếp cho giáo viên theo hướng dẫn của khóa học.</li><li>Sau khi thanh toán được xác nhận và đăng ký có hiệu lực, học theo lịch qua Google Meet.</li></ol><p>Nền tảng không thu giữ học phí thay giáo viên. Liên kết tham gia lớp được bảo vệ theo quyền truy cập; học viên không cần kết nối tài khoản Google để xem lịch học trên website.</p></section>
    <div className={styles.actions}><Link href="/courses">Khám phá khóa học</Link><Link href="/become-a-teacher">Đăng ký giáo viên</Link></div>
  </main></PublicLayout>;
}
