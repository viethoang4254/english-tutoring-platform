"use client";
import {useEffect,useState} from "react";
import {authorized,safeMessage,type Session} from "../auth/browser-api";
import {useSession} from "../auth/session-provider";
import {ApiRequestError} from "../../lib/api";
import {OverviewSummary,ActivityChart} from "./admin-analytics-charts";
import styles from "./admin-analytics.module.css";
type Money={amount:string;currency:string};
type Activity={users:number;enrollments:number;courses:number;confirmedPayments:number;confirmedTuition:Money};
type Totals=Activity & {students:number;teachers:number;approvedTeachers:number;pendingApplications:number;publishedCourses:number;draftCourses:number;payments:number;pendingPayments:number;expiredPayments:number;cancelledPayments:number};
export type Analytics={range:string;timezone:string;from:string|null;asOf:string;totals:Totals;period:Activity;days:(Activity&{date:string})[];courseTuition:{courseId:string;courseName:string;teacherId:string;teacherName:string;confirmedPayments:number;confirmedTuition:Money}[];moreCourses:boolean;teacherTuition:{teacherId:string;teacherName:string;confirmedPayments:number;confirmedTuition:Money}[];moreTeachers:boolean};
const ranges={"7":"7 ngày","30":"30 ngày","90":"90 ngày",all:"Toàn bộ thời gian"};
const money=(value:Money)=>{const [whole,fraction="00"]=value.amount.split(".");return `${BigInt(whole).toLocaleString("vi-VN")},${fraction.padEnd(2,"0")} ₫`;};
const count=(value:number)=>new Intl.NumberFormat("vi-VN").format(value);
function Cards({values}:{values:[string,string|number][]}){return <dl className={styles.cards}>{values.map(([label,value])=><div key={label}><dt>{label}</dt><dd>{typeof value==="number"?count(value):<span className={styles.amount}>{value}</span>}</dd></div>)}</dl>;}
export function AdminAnalytics({session,payments=false}:{session:Session;payments?:boolean}){
 const {clearSession}=useSession();
 const [range,setRange]=useState("30"),[attempt,setAttempt]=useState(0),[data,setData]=useState<Analytics>(),[error,setError]=useState("");
 useEffect(()=>{let active=true;
  authorized<Analytics>(session.accessToken,`/admin/analytics/overview?range=${range}`).then(value=>{
   if(!active)return;
   if(!value||value.range!==range||!value.totals||!value.period||!Array.isArray(value.days)||!Array.isArray(value.courseTuition)||!Array.isArray(value.teacherTuition))throw new Error("Invalid analytics response");
   setData(value);
  }).catch(failure=>{if(active){setError(safeMessage(failure));if(failure instanceof ApiRequestError&&failure.status===401)clearSession();}});
  return()=>{active=false;};
 },[session.accessToken,range,attempt,clearSession]);
 function reload(next=range){setData(undefined);setError("");setRange(next);setAttempt(value=>value+1);}
 return <section className={styles.panel} aria-label={payments?"Thống kê học phí":"Thống kê hệ thống"}>
  <header className={styles.overviewHeader}><div><h2>{payments?"Thống kê học phí":"Tổng quan hệ thống"}</h2><p>Thống kê hoạt động và số liệu tổng hợp toàn hệ thống</p></div><div className={styles.filters}><div role="group" aria-label="Khoảng thời gian">{Object.entries(ranges).map(([value,label])=><button key={value} aria-pressed={range===value} onClick={()=>reload(value)}>{value==="all"?"Toàn bộ":label}</button>)}</div><button onClick={()=>reload()}>{error?"Thử lại thống kê":"Tải lại thống kê"}</button></div></header>
  <p>Học phí được thanh toán trực tiếp cho gia sư. Các số liệu dưới đây không phải doanh thu của nền tảng.</p>
  {error?<p role="alert">{error}</p>:!data?<div role="status" aria-label="Đang tải thống kê" className={styles.skeleton}>Đang tải thống kê…<div/><div/><div/></div>:<>
   <p>Cập nhật: {new Intl.DateTimeFormat("vi-VN",{dateStyle:"short",timeStyle:"short",timeZone:data.timezone}).format(new Date(data.asOf))} · {data.timezone}</p>
   {!payments&&<OverviewSummary data={data}/>}
   <details className={styles.secondary} open={payments}><summary>Số liệu bổ sung và định nghĩa</summary>
   {!payments&&<><h2>Tổng quan toàn hệ thống</h2><p>Tổng số bản ghi hiện có, không phụ thuộc bộ lọc thời gian.</p><Cards values={[
    ["Tài khoản đã đăng ký",data.totals.users],["Học viên",data.totals.students],["Gia sư",data.totals.teachers],
    ["Gia sư đã được duyệt",data.totals.approvedTeachers],["Hồ sơ gia sư chờ duyệt",data.totals.pendingApplications],
    ["Khóa học",data.totals.courses],["Khóa học đã xuất bản",data.totals.publishedCourses],["Khóa học bản nháp",data.totals.draftCourses],["Đăng ký khóa học đã ghi nhận",data.totals.enrollments]]}/>
    <p>Gia sư đã được duyệt được đếm theo tài khoản, kể cả tài khoản đang khóa. Số đăng ký bao gồm mọi trạng thái, không biểu thị quyền tham gia lớp.</p></>}
   <h2>Thanh toán đã ghi nhận — toàn thời gian</h2><Cards values={[["Yêu cầu thanh toán",data.totals.payments],["Đã xác nhận",data.totals.confirmedPayments],["Chờ xác nhận",data.totals.pendingPayments],["Đã hết hạn",data.totals.expiredPayments],["Đã hủy",data.totals.cancelledPayments],["Tổng học phí đã xác nhận",money(data.totals.confirmedTuition)]]}/>
   <p>Tổng giá trị thanh toán đã xác nhận, chưa trừ hoàn tiền; không phải số dư ngân hàng. Mỗi lần thanh toán được ghi nhận riêng.</p>
   </details>
   <section className={styles.activityCard} aria-label="Hoạt động theo ngày"><h2>Hoạt động theo ngày</h2>
   <p><strong>Hoạt động trong kỳ — {ranges[range as keyof typeof ranges]}</strong></p><p>{data.from?"Từ 00:00 ngày đầu kỳ đến thời điểm cập nhật, bao gồm hôm nay.":"Từ khi có dữ liệu đến thời điểm cập nhật."} Ngày đăng ký/tạo mới và ngày xác nhận thanh toán được tính theo giờ Việt Nam.</p>
   <Cards values={[["Tài khoản mới trong kỳ",data.period.users],["Đăng ký mới trong kỳ",data.period.enrollments],["Khóa học mới trong kỳ",data.period.courses],["Thanh toán xác nhận trong kỳ",data.period.confirmedPayments],["Học phí xác nhận trong kỳ",money(data.period.confirmedTuition)]]}/>
   {data.period.users+data.period.enrollments+data.period.courses+data.period.confirmedPayments===0&&<p>Chưa có hoạt động trong khoảng thời gian này.</p>}
   <div className={styles.activityGrid}>
   {range==="all"?<p>Chọn 7, 30 hoặc 90 ngày để xem chi tiết hoạt động theo ngày.</p>:data.days.length>0&&<table><caption>Hoạt động theo ngày (ngày không có hoạt động được lược bỏ)</caption><thead><tr><th>Ngày</th><th>Tài khoản mới</th><th>Đăng ký mới</th><th>Khóa học mới</th><th>Thanh toán xác nhận</th><th>Học phí xác nhận</th></tr></thead><tbody>{data.days.map(day=><tr key={day.date}><th scope="row"><span className={styles.date}>{day.date.split("-").reverse().join("/")}</span></th><td data-label="Tài khoản mới">{count(day.users)}</td><td data-label="Đăng ký mới">{count(day.enrollments)}</td><td data-label="Khóa học mới">{count(day.courses)}</td><td data-label="Thanh toán xác nhận">{count(day.confirmedPayments)}</td><td data-label="Học phí xác nhận"><span className={styles.amount}>{money(day.confirmedTuition)}</span></td></tr>)}</tbody></table>}
   {range!=="all"&&data.days.length>0&&<ActivityChart days={data.days}/>}</div></section>
   {payments&&data.teacherTuition.length>0&&<><h2>Học phí đã xác nhận theo gia sư trong kỳ</h2><p>Tối đa 20 gia sư theo học phí giảm dần.</p><table><caption>Học phí từ các khóa học của từng gia sư</caption><thead><tr><th>Gia sư</th><th>Thanh toán xác nhận</th><th>Học phí xác nhận</th></tr></thead><tbody>{data.teacherTuition.map(teacher=><tr key={teacher.teacherId}><th scope="row">{teacher.teacherName}</th><td data-label="Thanh toán xác nhận">{count(teacher.confirmedPayments)}</td><td data-label="Học phí xác nhận"><span className={styles.amount}>{money(teacher.confirmedTuition)}</span></td></tr>)}</tbody></table>{data.moreTeachers&&<p>Còn các gia sư khác ngoài 20 dòng hiển thị.</p>}</>}
   {payments&&<><h2>Học phí đã xác nhận theo khóa học trong kỳ</h2><p>Tối đa 20 khóa học theo học phí giảm dần, kèm gia sư sở hữu. Đây không phải tổng học phí của từng gia sư.</p>{data.courseTuition.length===0?<p>Chưa có học phí được xác nhận trong kỳ.</p>:<table><caption>Khóa học và gia sư nhận học phí</caption><thead><tr><th>Khóa học</th><th>Gia sư</th><th>Thanh toán xác nhận</th><th>Học phí xác nhận</th></tr></thead><tbody>{data.courseTuition.map(course=><tr key={course.courseId}><th scope="row">{course.courseName}</th><td data-label="Gia sư">{course.teacherName}</td><td data-label="Thanh toán xác nhận">{count(course.confirmedPayments)}</td><td data-label="Học phí xác nhận"><span className={styles.amount}>{money(course.confirmedTuition)}</span></td></tr>)}</tbody></table>}{data.moreCourses&&<p>Còn các khóa học khác ngoài 20 dòng hiển thị. Tổng học phí phía trên vẫn bao gồm tất cả khóa học.</p>}</>}
  </>}
 </section>;
}
