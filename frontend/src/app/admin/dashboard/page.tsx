import { SiteHeader, SiteFooter } from "../../../components/site-shell";
import { AdminDashboard } from "../../../features/admin/admin-dashboard";

export default async function AdminDashboardPage({searchParams}:{searchParams:Promise<{section?:string|string[];id?:string|string[]}>}) {
  const query=await searchParams;
  return <><SiteHeader /><AdminDashboard sectionName={typeof query.section==="string"?query.section:undefined} reviewId={typeof query.id==="string"?query.id:undefined}/><SiteFooter /></>;
}
