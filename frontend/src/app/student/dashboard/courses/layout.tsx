import type { ReactNode } from "react";
import { StudentDashboard } from "../../../../features/student/student-dashboard";

export default function Layout({children}:{children:ReactNode}) {
  return <><StudentDashboard>{children}</StudentDashboard></>;
}
