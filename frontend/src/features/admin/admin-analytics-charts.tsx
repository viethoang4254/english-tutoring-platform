import {Icon} from "../../components/site-shell";
import type {Analytics} from "./admin-analytics";
import styles from "./admin-analytics.module.css";
const colors=["var(--ui-accent)","var(--ui-highlight)","var(--ui-warning)","var(--ui-danger)"];
const number=(value:number)=>value.toLocaleString("vi-VN");
function Donut({title,total,items}:{title:string;total:number;items:[string,number][]}){
 return <section className={styles.chartCard} aria-label={title}><h2>{title}</h2><p>Phân bố toàn thời gian</p><div className={styles.distribution}>
 <div className={styles.donut}><svg viewBox="0 0 120 120" role="img" aria-label={`${title}: ${number(total)}`}><circle cx="60" cy="60" r="45" fill="none" stroke="var(--ui-border)" strokeWidth="18"/>{items.map(([label,value],i)=>{const portion=total?value/total*100:0;const start=items.slice(0,i).reduce((sum,[,previous])=>sum+(total?previous/total*100:0),0);return <circle key={label} cx="60" cy="60" r="45" fill="none" stroke={colors[i]} strokeWidth="18" pathLength="100" strokeDasharray={`${portion} ${100-portion}`} strokeDashoffset={-start} transform="rotate(-90 60 60)"/>;})}</svg><strong>{number(total)}</strong></div>
 <ul className={styles.legend}>{items.map(([label,value],i)=><li key={label}><span className={styles.dot} style={{background:colors[i]}}/><span>{label}</span><b>{number(value)}</b><span>{new Intl.NumberFormat("vi-VN",{maximumFractionDigits:1}).format(total?value/total*100:0)}%</span></li>)}</ul></div>{total===0&&<p>Chưa có dữ liệu.</p>}</section>;
}
export function OverviewSummary({data}:{data:Analytics}){
 const t=data.totals;
 const cards:[string,number,"person"|"book"|"check"][]=[["Tổng người dùng",t.users,"person"],["Giáo viên đã duyệt",t.approvedTeachers,"check"],["Tổng khóa học",t.courses,"book"],["Tổng lượt ghi danh",t.enrollments,"person"],["Giao dịch đã xác nhận",t.confirmedPayments,"check"]];
 return <><dl className={styles.summary}>{cards.map(([label,value,icon])=><div key={label}><span className={styles.icon}><Icon kind={icon}/></span><dt>{label}</dt><dd>{number(value)}</dd><small>Toàn thời gian</small></div>)}</dl>
 <div className={styles.charts}><Donut title="Trạng thái khóa học" total={t.courses} items={[["Đã xuất bản",t.publishedCourses],["Bản nháp",t.draftCourses],["Khác",t.courses-t.publishedCourses-t.draftCourses]]}/><Donut title="Trạng thái thanh toán" total={t.payments} items={[["Đã xác nhận",t.confirmedPayments],["Đang chờ",t.pendingPayments],["Hết hạn",t.expiredPayments],["Đã hủy",t.cancelledPayments]]}/></div>
 </>;
}
export function ActivityChart({days}:{days:Analytics["days"]}){
 const series=[{key:"users",label:"Đăng ký mới"},{key:"enrollments",label:"Ghi danh"},{key:"courses",label:"Khóa học mới"}] as const;
 const maximum=Math.max(1,...days.flatMap(day=>series.map(s=>day[s.key])));
 const first=Date.parse(days[0].date),last=Date.parse(days[days.length-1].date);
 const x=(date:string)=>40+(last===first?220:(Date.parse(date)-first)/(last-first)*440);
 const y=(value:number)=>180-value/maximum*145;
 return <figure className={styles.lineChart}><svg viewBox="0 0 520 230" role="img" aria-label="Xu hướng đăng ký mới, ghi danh và khóa học mới; số liệu chi tiết trong bảng bên cạnh">
 {[0,.5,1].map(f=><g key={f}><line x1="40" x2="480" y1={y(maximum*f)} y2={y(maximum*f)} stroke="var(--ui-border)"/><text x="32" y={y(maximum*f)+4} textAnchor="end">{number(maximum*f)}</text></g>)}
 {series.map((s,i)=><g key={s.key}><polyline fill="none" stroke={colors[i]} strokeWidth="2" points={days.map(d=>`${x(d.date)},${y(d[s.key])}`).join(" ")}/>{days.map(d=><circle key={d.date} cx={x(d.date)} cy={y(d[s.key])} r="3" fill={colors[i]}><title>{d.date}: {s.label} {number(d[s.key])}</title></circle>)}</g>)}
 <text x="40" y="210">{days[0].date.split("-").reverse().join("/")}</text><text x="480" y="210" textAnchor="end">{days[days.length-1].date.split("-").reverse().join("/")}</text></svg>
 <figcaption>{series.map((s,i)=><span key={s.key}><i className={styles.dot} style={{background:colors[i]}}/>{s.label}</span>)}</figcaption><p>Chỉ vẽ các ngày API ghi nhận; đường nối không bổ sung số liệu cho ngày bị lược bỏ.</p></figure>;
}
