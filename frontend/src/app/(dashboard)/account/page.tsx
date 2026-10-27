import { redirect } from "next/navigation";
export default async function AccountPage({searchParams}:{searchParams:Promise<{view?:string}>}) { const {view}=await searchParams; if(view==="student")redirect("/register"); if(view==="register")redirect("/become-a-teacher"); if(view==="verify")redirect("/verify-email"); return null; }
