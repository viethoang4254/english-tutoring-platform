import Link from "next/link";
import { Icon, PublicLayout } from "../../components/site-shell";
import styles from "../public-pages.module.css";

export const metadata = {title: "Giới thiệu | Tiếng Anh cùng gia sư"};
const values = [
  {icon:"video",title:"Học tập linh hoạt",text:"Tham gia lớp trực tuyến theo lịch đã công bố, từ không gian học tập phù hợp với bạn."},
  {icon:"book",title:"Lộ trình rõ ràng",text:"Tìm hiểu mô tả, mục tiêu và lộ trình từng buổi trước khi chọn khóa học."},
  {icon:"person",title:"Kết nối giáo viên",text:"Học cùng giáo viên trực tiếp xây dựng nội dung và quản lý khóa học của mình."},
  {icon:"check",title:"Trải nghiệm minh bạch",text:"Học phí, trạng thái đăng ký và điều kiện tham gia lớp được thể hiện rõ ràng."},
] as const;

function LearningIllustration({schedule=false}:{schedule?:boolean}) {
  return <div className={styles.illustration} aria-hidden="true">
    <div className={styles.illustrationLabel}>TIẾNG ANH · TỪNG BƯỚC TIẾN BỘ</div>
    <div className={styles.lessonBoard}><span className={styles.iconTile}><Icon kind={schedule?"check":"book"}/></span>
      <span className={styles.boardTitle}>{schedule?"Một kế hoạch học rõ ràng":"Mở ra cuộc hội thoại mới"}</span>
      <div className={styles.wordTiles}>{(schedule?["Mục tiêu","Lịch học","Thực hành"]:["Hello!","Listen","Speak"]).map(word=><span key={word}>{word}</span>)}</div>
      <div className={styles.paperLines}><span/><span/><span/></div>
    </div>
    <div className={styles.floatingNote}><Icon kind={schedule?"book":"video"}/><span>{schedule?"Học theo từng khóa":"Lớp học qua Google Meet"}</span></div>
  </div>;
}

export default function About() {
  return <PublicLayout showTeacherRegistration={false}><main className={styles.about}>
    <section className={styles.introduction}><p className={styles.eyebrow}>TIẾNG ANH CÙNG GIA SƯ</p><h1>Về chúng tôi</h1>
      <p>Kết nối học viên với giáo viên tiếng Anh qua từng khóa học có mục tiêu, lộ trình và lịch học rõ ràng. Giáo viên quản lý khóa học của mình; học viên đăng ký và thanh toán theo từng khóa, học trực tuyến qua Google Meet.</p>
    </section>
    <section className={styles.splitSection} aria-labelledby="welcome-title"><LearningIllustration/>
      <div><p className={styles.eyebrow}>HỌC CÙNG NGƯỜI HƯỚNG DẪN</p><h2 id="welcome-title">Chào mừng bạn đến với Tiếng Anh Cùng Gia Sư</h2>
        <p>Mỗi người có một lý do để bắt đầu học tiếng Anh. Nền tảng giúp bạn tìm hiểu khóa học, lựa chọn nội dung phù hợp và theo sát kế hoạch học cùng giáo viên.</p>
        <p>Từ giao tiếp hằng ngày đến tiếng Anh trong công việc, hãy bắt đầu bằng mục tiêu rõ ràng và những buổi thực hành có hướng dẫn.</p>
        <div className={styles.actions}><Link href="/courses">Khám phá khóa học <span aria-hidden="true">↗</span></Link></div>
      </div>
    </section>
    <section className={styles.values} aria-labelledby="values-title"><div className={styles.sectionInner}>
      <p className={styles.eyebrow}>ĐIỀU CHÚNG TÔI HƯỚNG ĐẾN</p><h2 id="values-title">Mục tiêu của chúng tôi</h2>
      <p className={styles.sectionLead}>Giúp việc lựa chọn và tham gia một khóa học trở nên rõ ràng, thuận tiện và gần gũi.</p>
      <div className={styles.valueGrid}>{values.map(value=><article key={value.title}><span className={styles.iconTile}><Icon kind={value.icon}/></span><h3>{value.title}</h3><p>{value.text}</p></article>)}</div>
    </div></section>
    <section className={styles.splitSection} aria-labelledby="benefits-title"><div>
      <p className={styles.eyebrow}>KHÔNG GIAN HỌC CỦA BẠN</p><h2 id="benefits-title">Học tiếng Anh mọi lúc, mọi nơi</h2>
      <p>Khám phá khóa học trên thiết bị của bạn và tham gia lớp trực tuyến đúng lịch đã đăng ký. Học chủ động, với kế hoạch cụ thể.</p>
      <ul className={styles.checkList}>{["Học theo từng khóa tiếng Anh với mục tiêu rõ ràng.","Các buổi học trực tuyến được sắp lịch trước.","Lộ trình và nội dung học được giới thiệu theo khóa.","Google Meet được bảo vệ theo quyền tham gia của học viên.","Học phí minh bạch, thanh toán trực tiếp cho giáo viên.","Theo dõi lịch học ngay trong bảng học tập cá nhân."].map(text=><li key={text}><Icon kind="check"/><span>{text}</span></li>)}</ul>
      <div className={styles.actions}><Link href="/courses">Xem các khóa học</Link></div>
    </div><LearningIllustration schedule/></section>
    <section className={styles.teacherInfo} aria-labelledby="teacher-title"><span className={styles.iconTile}><Icon kind="person"/></span><div><p className={styles.eyebrow}>SỰ ĐỒNG HÀNH TRONG TỪNG BUỔI HỌC</p><h2 id="teacher-title">Đồng hành cùng giáo viên</h2><p>Giáo viên xây dựng khóa học, quản lý lịch dạy và trực tiếp hướng dẫn các buổi học. Thông tin khóa học giúp bạn hiểu nội dung, yêu cầu đầu vào và kết quả mong đợi trước khi đăng ký.</p><p>Đăng ký có hiệu lực và thanh toán được xác nhận là điều kiện để truy cập lớp học được bảo vệ. Nền tảng không thu giữ học phí thay giáo viên.</p></div></section>
  </main></PublicLayout>;
}
