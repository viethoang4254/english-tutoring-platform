import { AuthPage } from "../../features/auth/auth-page";

export default async function LoginPage({ searchParams }: { searchParams: Promise<{ registered?: string; reset?: string; next?: string }> }) {
  const { registered, reset, next } = await searchParams;
  return <AuthPage view="login" returnTo={next} registered={registered === "1"} reset={reset === "1"} />;
}
