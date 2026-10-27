import { Suspense, type ReactNode } from "react";
import { SiteHeader, SiteFooter } from "../../components/site-shell";
import { TeacherDashboard } from "../../features/teacher/teacher-dashboard";
export default function DashboardLayout({ children }: { children: ReactNode }) { return <><SiteHeader /><Suspense fallback={<main><p role="status">Đang mở Dashboard…</p></main>}><TeacherDashboard>{children}</TeacherDashboard></Suspense><SiteFooter /></>; }
