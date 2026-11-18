import type {Analytics} from "../../src/features/admin/admin-analytics";
export function analyticsFixture(range="30"):Analytics {
 const confirmedTuition={amount:"1000000.25",currency:"VND"};
 return {range,timezone:"Asia/Ho_Chi_Minh",from:range==="all"?null:new Date(Date.parse("2026-11-17T17:00:00Z")-(Number(range)-1)*86400000).toISOString(),asOf:"2026-11-18T03:00:00Z",
  totals:{users:12,students:8,teachers:3,approvedTeachers:2,pendingApplications:1,courses:6,publishedCourses:4,draftCourses:2,enrollments:10,payments:13,confirmedPayments:8,pendingPayments:2,expiredPayments:2,cancelledPayments:1,confirmedTuition},
  period:{users:3,enrollments:5,courses:2,confirmedPayments:2,confirmedTuition},
  days:range==="all"?[]:[{date:"2026-11-18",users:3,enrollments:5,courses:2,confirmedPayments:2,confirmedTuition}],
  courseTuition:[{courseId:"11",courseName:"Tiếng Anh giao tiếp",teacherId:"2",teacherName:"Gia sư kiểm thử",confirmedPayments:2,confirmedTuition}],moreCourses:false,teacherTuition:[{teacherId:"2",teacherName:"Gia sư kiểm thử",confirmedPayments:2,confirmedTuition}],moreTeachers:false};
}
