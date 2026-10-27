import Link from "next/link";
export default function TeacherPage() {
  return <section><h2>Chào mừng bạn</h2><p>Quản lý khóa học, lịch dạy và các buổi học của bạn.</p><Link className="ui-button ui-button-primary" href="/teacher/courses">Mở khóa học của tôi</Link></section>;
}
