"use client";

import { createContext, useCallback, useContext, useState, useEffect, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { restoreGoogleReturn } from "../google-calendar/google-return";
import { rememberLogin, restoreRememberedSession } from "./remember-session";
import type { Session } from "./browser-api";

const Context = createContext<{
  session: Session | null;
  setSession: (session: Session | null) => void;
  clearSession: () => void;
  googleOutcome: string | null;
} | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const queryClient = useQueryClient();
  const [restoring, setRestoring] = useState(true);
  const [googleOutcome, setGoogleOutcome] = useState<string | null>(null);
  const [returnFailed, setReturnFailed] = useState(false);
  useEffect(() => {
    let active = true;
    const callback = restoreGoogleReturn();
    if (callback) void callback.then(result => {
      if (!active) return;
      if (result) { setSession(result.session); setGoogleOutcome(result.outcome); }
      else setReturnFailed(true);
      setRestoring(false);
    });
    else void restoreRememberedSession().then(value => {
      if (active) { setSession(value); setRestoring(false); }
    });
    return () => { active = false; };
  }, []);
  const clearSession = useCallback(() => { rememberLogin(false); setGoogleOutcome(null); setSession(null); queryClient.clear(); }, [queryClient]);
  return <Context.Provider value={{ session, setSession, clearSession, googleOutcome }}>{restoring ? <p role="status">Đang kiểm tra phiên đăng nhập…</p> : returnFailed ? <main><p role="alert">Không thể xác minh phiên kết nối Google cho tài khoản ban đầu. Vui lòng đăng nhập lại.</p><a href="/login">Đăng nhập</a></main> : children}</Context.Provider>;
}

export function useSession() {
  const value = useContext(Context);
  if (!value) throw new Error("SessionProvider is required");
  return value;
}
