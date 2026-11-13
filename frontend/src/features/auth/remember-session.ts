import { signIn, type Session } from "./browser-api.ts";
const key = "remember-login";
export function rememberLogin(enabled: boolean) {
  try { if (enabled) localStorage.setItem(key, "1"); else localStorage.removeItem(key); } catch { /* Browser storage may be disabled. */ }
}
let restoring: Promise<Session | null> | null = null;
export function restoreRememberedSession(): Promise<Session | null> {
  if (restoring) return restoring;
  try { if (localStorage.getItem(key) !== "1") return Promise.resolve(null); } catch { return Promise.resolve(null); }
  // Consume the hint before rotation: an uncertain result must not retry on reload.
  rememberLogin(false);
  restoring = signIn().then(session => { rememberLogin(true); return session; })
    .catch(() => null).finally(() => { restoring = null; });
  return restoring;
}
