"use client";
import Link from "next/link";
import { useSession } from "../auth/session-provider";
import { checkoutPath } from "../enrollment/checkout-path";

export function CourseRegistration({ courseId }: { courseId: string }) {
  const { session } = useSession();
  const path=checkoutPath(courseId);
  return <section aria-label="Đăng ký khóa học">
    {session && session.account.role !== "STUDENT" ? <p>Chỉ tài khoản học viên được đăng ký khóa học.</p> :
      <Link className="ui-button ui-button-primary" href={session?path:`/login?next=${encodeURIComponent(path)}`}>
        {session?"Đăng ký khóa học":"Đăng nhập để đăng ký"}
      </Link>}
    <p>Xem thông tin khóa học và học phí trước khi xác nhận. Giữ chỗ không có nghĩa là đã thanh toán và không mở nội dung khóa học.</p>
  </section>;
}
