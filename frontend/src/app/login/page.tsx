import { AuthPage } from "../../features/auth/auth-page";

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ registered?: string; reset?: string }> }) {
  const { registered, reset } = await searchParams;
  return <AuthPage view="login" registered={registered === "1"} reset={reset === "1"} />;
}
