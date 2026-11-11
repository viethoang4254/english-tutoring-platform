import type { ReactNode } from "react";
import { StudentDashboard } from "../../../../features/student/student-dashboard";
import { SiteFooter } from "../../../../components/site-shell";

export default function Layout({children}:{children:ReactNode}) {
  return <><StudentDashboard>{children}</StudentDashboard><SiteFooter /></>;
}
