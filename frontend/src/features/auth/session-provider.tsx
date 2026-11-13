"use client";

import { createContext, useCallback, useContext, useState, useEffect, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { rememberLogin, restoreRememberedSession } from "./remember-session";
import type { Session } from "./browser-api";

const Context = createContext<{
  session: Session | null;
  setSession: (session: Session | null) => void;
  clearSession: () => void;
} | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const queryClient = useQueryClient();
  const [restoring, setRestoring] = useState(true);
  useEffect(() => {
    let active = true;
    restoreRememberedSession().then(value => {
      if (active) { setSession(value); setRestoring(false); }
    });
    return () => { active = false; };
  }, []);
  const clearSession = useCallback(() => { rememberLogin(false); setSession(null); queryClient.clear(); }, [queryClient]);
  return <Context.Provider value={{ session, setSession, clearSession }}>{restoring ? <p role="status">Đang kiểm tra phiên đăng nhập…</p> : children}</Context.Provider>;
}

export function useSession() {
  const value = useContext(Context);
  if (!value) throw new Error("SessionProvider is required");
  return value;
}
