import { Suspense, type ReactNode } from "react";
import { DashboardRoutes } from "../../components/dashboard-routes";
export default function Layout({children}:{children:ReactNode}) {return <Suspense fallback={<main role="status">Đang mở Dashboard…</main>}><DashboardRoutes>{children}</DashboardRoutes></Suspense>;}
