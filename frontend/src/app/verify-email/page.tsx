import { AuthPage } from "../../features/auth/auth-page";

export default async function Page({ searchParams }: { searchParams: Promise<{ registered?: string }> }) {
  const { registered } = await searchParams;
  return <AuthPage view="verify" registered={registered === "1"} />;
}
