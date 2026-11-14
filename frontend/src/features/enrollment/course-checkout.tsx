"use client";
import Link from "next/link";
import {useEffect,useRef,useState} from "react";
import {useRouter} from "next/navigation";
import {authorized} from "../auth/browser-api";
import {useSession} from "../auth/session-provider";
import {ApiRequestError} from "../../lib/api";
import {courseDate,tuitionLabel,type PublicCourseDetail} from "../course/public-courses";
import {enrollmentError,statusLabel,type Enrollment} from "./enrollment";
import {rememberReview} from "./payment-review";
import styles from "./checkout.module.css";

export type Checkout = Pick<PublicCourseDetail,"course"|"rules"> & {
  price:{originalAmount:string;discountPercent:string;exactDiscountAmount:string;roundingAdjustment:string;finalAmount:string;currency:string};
  enrollment:Enrollment|null;paymentId:string|null;available:boolean;blockedReason:string|null;calculatedAt:string;
};
type PaymentResult={id:string;enrollmentId:string;status:string;finalAmount:string;currency:string};
const weekdays=["","Thứ Hai","Thứ Ba","Thứ Tư","Thứ Năm","Thứ Sáu","Thứ Bảy","Chủ nhật"];
const reasons:Record<string,string>={COURSE_FULL:"Khóa học đã hết chỗ.",ENROLLMENT_ACTIVE:"Bạn đang học khóa học này.",ENROLLMENT_COMPLETED:"Bạn đã hoàn thành khóa học này.",ENROLLMENT_CANCELLED:"Đăng ký đã hủy. Chưa hỗ trợ đăng ký lại khóa học này."};
function checkoutError(failure:unknown) {
  if(failure instanceof ApiRequestError && failure.code==="COURSE_NOT_FOUND")return "Khóa học không tồn tại hoặc không còn khả dụng.";
  if(failure instanceof ApiRequestError && failure.code==="COURSE_FULL")return reasons.COURSE_FULL;
  return enrollmentError(failure);
}
export function CourseCheckout({courseId}:{courseId:string}) {
  const {session}=useSession();
  if(!session || session.account.role!=="STUDENT")return null;
  return <Confirmation key={`${courseId}:${session.accessToken}`} courseId={courseId} student={session.account.id} token={session.accessToken}/>;
}
function Confirmation({courseId,student,token}:{courseId:string;student:string;token:string}) {
  const router=useRouter();const {clearSession}=useSession();
  const [data,setData]=useState<Checkout|null>(null),[error,setError]=useState("");
  const [busy,setBusy]=useState(false),[attempt,setAttempt]=useState(0);
  const [reservation,setReservation]=useState<Enrollment|null>(null);
  const [changed,setChanged]=useState<PaymentResult|null>(null);
  const flight=useRef(false),alive=useRef(true),leaving=useRef(false);
  useEffect(()=>{alive.current=true;return()=>{alive.current=false;};},[]);
  useEffect(()=>{
    let active=true;
    authorized<Checkout>(token,`/student/courses/${encodeURIComponent(courseId)}/checkout`).then(value=>{
      if(!value || value.course.id!==courseId || value.price.currency!=="VND" || !/^\d+$/.test(value.price.finalAmount))throw new Error("Invalid checkout");
      if(active)setData(value);
    }).catch(failure=>{if(active){setError(checkoutError(failure));if(failure instanceof ApiRequestError && failure.status===401)clearSession();}});
    return()=>{active=false;};
  },[courseId,token,attempt,clearSession]);
  const back=`/student/dashboard/courses/${encodeURIComponent(courseId)}`;
  function open(id:string){leaving.current=true;router.push(`/student/enrollments?id=${encodeURIComponent(id)}`);}
  async function confirm() {
    if(flight.current || leaving.current || !data?.available || error || changed)return;
    flight.current=true;setBusy(true);
    try {
      const row=await authorized<Enrollment>(token,`/student/courses/${courseId}/enrollment`,undefined,"POST");
      if(!row || row.courseId!==courseId || !/^[1-9]\d*$/.test(row.id) || !["PENDING","ACTIVE"].includes(row.status))throw new Error("Invalid enrollment");
      if(!alive.current)return;
      setReservation(row);
      if(row.status==="ACTIVE"){open(row.id);return;}
      rememberReview(student,row.id,{amount:data.price.finalAmount,paymentId:null,deadline:row.seatReservedUntil});
      const payment=await authorized<PaymentResult>(token,`/student/enrollments/${row.id}/payments`,undefined,"POST");
      if(!payment || payment.enrollmentId!==row.id || !/^[1-9]\d*$/.test(payment.id) || payment.currency!=="VND" || payment.status!=="PENDING" || !/^\d+$/.test(payment.finalAmount))throw new Error("Invalid payment");
      if(!alive.current)return;
      rememberReview(student,row.id,{amount:data.price.finalAmount,paymentId:payment.id,deadline:null});
      if(payment.finalAmount!==data.price.finalAmount)setChanged(payment);
      else open(row.id);
    }catch(failure){if(alive.current){setError(checkoutError(failure));if(failure instanceof ApiRequestError && failure.status===401)clearSession();}}
    finally{if(!leaving.current){flight.current=false;if(alive.current)setBusy(false);}}
  }
  const existing=reservation??data?.enrollment;
  return <section className={styles.checkout} aria-label="Xác nhận đăng ký">
    <header><p>Đăng ký khóa học</p><h1>Xác nhận đăng ký</h1><p>Kiểm tra thông tin trước khi giữ chỗ và tạo yêu cầu thanh toán.</p></header>
    {error&&<div role="alert"><p>{error}</p><p>Không tự động gửi lại yêu cầu. Đăng ký đã tạo vẫn được giữ nguyên.</p>
      <button className="ui-button ui-button-secondary" disabled={busy} onClick={()=>{setData(null);setError("");setChanged(null);setAttempt(n=>n+1);}}>Tải lại thông tin</button>
      <Link href={existing?`/student/enrollments?id=${existing.id}`:"/student/enrollments"}>Kiểm tra đăng ký và khôi phục thanh toán</Link></div>}
    {!data&&!error&&<p role="status">Đang tải thông tin đăng ký…</p>}
    {data&&<div className={styles.columns}>
      <article className={styles.panel}><h2 className={styles.courseName}>{data.course.name}</h2>
        <dl><dt>Danh mục</dt><dd>{data.course.category.name}</dd><dt>Gia sư</dt><dd>{data.course.teacher.fullName}</dd>
        <dt>Số buổi học</dt><dd>{data.course.sessionCount}</dd><dt>Ngày bắt đầu dự kiến</dt><dd>{courseDate(data.course.plannedStartDate)}</dd>
        <dt>Múi giờ</dt><dd>{data.course.timezone==="Asia/Ho_Chi_Minh"?"Giờ Việt Nam (UTC+7)":data.course.timezone}</dd>
        <dt>Lịch học hằng tuần</dt><dd>{data.rules.length?<ul>{data.rules.map(rule=><li key={rule.id}>{weekdays[rule.dayOfWeek]} · {rule.startTime.slice(0,5)}–{rule.endTime.slice(0,5)}</li>)}</ul>:"Chưa có lịch học khả dụng."}</dd></dl>
        <p>Chỗ được giữ tối đa 15 phút và không quá giờ bắt đầu buổi đầu tiên. Chỉ đăng ký đang hoạt động sau xác nhận thanh toán mới được vào lớp.</p>
        {data.enrollment&&<p>Đăng ký hiện tại: <strong>{statusLabel[data.enrollment.status]}</strong></p>}
      </article>
      <aside className={styles.panel} aria-label="Học phí"><h2>Học phí khóa học</h2>
        <dl><dt>Học phí gốc</dt><dd>{tuitionLabel(data.price.originalAmount)}</dd>
        <dt>Giảm giá áp dụng</dt><dd>{Number(data.price.discountPercent).toLocaleString("vi-VN")}%</dd>
        <dt>Số tiền giảm</dt><dd>{tuitionLabel(data.price.exactDiscountAmount)}</dd>
        <dt>Thành tiền (làm tròn VND)</dt><dd className={styles.amount}>{tuitionLabel(data.price.finalAmount)}</dd></dl>
        <p>Máy chủ xác định học phí tại thời điểm tạo thanh toán. Yêu cầu đang chờ hợp lệ giữ nguyên giá đã lưu.</p>
        {!data.available&&<p role="status">{reasons[data.blockedReason??""]??enrollmentError(new ApiRequestError(409,data.blockedReason??"", ""))}</p>}
        {changed?<div role="alert"><p>Học phí đã thay đổi. Số tiền thanh toán mới: <strong>{tuitionLabel(changed.finalAmount)}</strong>.</p>
          <p>Vui lòng xác nhận số tiền mới trước khi xem hướng dẫn chuyển khoản.</p>
          <button className="ui-button ui-button-primary" onClick={()=>{rememberReview(student,changed.enrollmentId,{amount:changed.finalAmount,paymentId:changed.id,deadline:null});open(changed.enrollmentId);}}>Tôi đồng ý với số tiền mới</button></div>:
          <button className="ui-button ui-button-primary" disabled={!data.available||busy||!!error} onClick={()=>void confirm()}>{busy?"Đang xác nhận…":"Xác nhận và thanh toán"}</button>}
        {existing&&!changed&&<Link href={`/student/enrollments?id=${existing.id}`}>Xem đăng ký hiện có</Link>}
      </aside>
    </div>}
    <Link className={`ui-button ui-button-secondary ${styles.back}`} href={back}>Quay lại khóa học</Link>
  </section>;
}
