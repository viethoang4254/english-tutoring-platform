import { SiteHeader, SiteFooter } from "../../../components/site-shell";
import { AdminDashboard } from "../../../features/admin/admin-dashboard";

export default function AdminDashboardPage() {
  return <><SiteHeader /><AdminDashboard /><SiteFooter /></>;
}
