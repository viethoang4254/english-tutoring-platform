import { signIn, type Session } from "../auth/browser-api";

const marker = "google-calendar-return";
const lifetime = 10 * 60_000;
const outcomes = new Set(["DISCONNECT_FIRST","REVOCATION_PENDING","CONNECTED", "CONSENT_DENIED", "EXPIRED_OR_INVALID", "INVALID_RESPONSE", "MISSING_PERMISSIONS", "RECONNECT_REQUIRED", "CONNECTION_CONFLICT", "DENIED", "UNAVAILABLE"]);
type VerifiedReturn = { session: Session; outcome: string };
let restoring: Promise<VerifiedReturn | null> | null = null;

export function prepareGoogleReturn(accountId: string) {
  if (!/^[1-9][0-9]*$/.test(accountId)) throw new Error("Invalid account");
  sessionStorage.setItem(marker, JSON.stringify({ accountId, started: Date.now() }));
}

export function restoreGoogleReturn(): Promise<VerifiedReturn | null> | null {
  const url = new URL(window.location.href);
  if (url.pathname !== "/account" || !url.searchParams.has("googleCalendar")) return null;
  if (restoring) return restoring;
  // Every callback claims this navigation, including malformed/replayed returns.
  // A resolved null prevents falling back to a different remembered account.
  try {
    const raw = sessionStorage.getItem(marker);
    sessionStorage.removeItem(marker);
    const outcomesInUrl = url.searchParams.getAll("googleCalendar");
    const outcome = outcomesInUrl[0];
    if (!raw || outcomesInUrl.length !== 1 || !outcomes.has(outcome)) return Promise.resolve(null);
    const record = JSON.parse(raw);
    const now = Date.now();
    if (typeof record?.accountId !== "string" || !/^[1-9][0-9]*$/.test(record.accountId) ||
        !Number.isSafeInteger(record.started) || record.started > now || now - record.started >= lifetime)
      return Promise.resolve(null);

    restoring = signIn().then(session => session.account.id === record.accountId ? { session, outcome } : null)
      .catch(() => null).finally(() => { restoring = null; });
    return restoring;
  } catch { return Promise.resolve(null); }
}
