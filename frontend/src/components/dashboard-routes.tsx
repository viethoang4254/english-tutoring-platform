"use client";
import { usePathname, useSearchParams } from "next/navigation";
import type { ReactNode } from "react";
import { useSession } from "../features/auth/session-provider";
import { TeacherDashboard } from "../features/teacher/teacher-dashboard";
import { StudentDashboard } from "../features/student/student-dashboard";
import { StudentEnrollments } from "../features/enrollment/student-enrollments";
import { AccountWorkspace } from "../features/auth/account-workspace";
import { AdminDashboard } from "../features/admin/admin-dashboard";
export function DashboardRoutes({children}:{children:ReactNode}) {
 const path=usePathname(),query=useSearchParams();const {session}=useSession();
 if(session?.account.role==="STUDENT"&&(path==="/account"||path==="/student/enrollments"))return <StudentDashboard>{path==="/account"?<AccountWorkspace embeddedPanel="settings" dashboardPanel="password"/>:<StudentEnrollments key={`${session.accessToken}:${query.get("id")??"list"}`} session={session} enrollmentId={query.get("id")}/>}</StudentDashboard>;
 if(session?.account.role==="ADMIN"&&path==="/account")return <AdminDashboard sectionName="account"/>;
 return <TeacherDashboard>{children}</TeacherDashboard>;
}
