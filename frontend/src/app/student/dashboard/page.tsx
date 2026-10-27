import { SiteHeader, SiteFooter } from "../../../components/site-shell";
import { StudentDashboard } from "../../../features/student/student-dashboard";

export default function StudentDashboardPage() {
  return <><SiteHeader /><StudentDashboard /><SiteFooter /></>;
}
