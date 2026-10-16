"use client";

import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import type { Session } from "./browser-api";

const Context = createContext<{
  session: Session | null;
  setSession: (session: Session | null) => void;
  clearSession: () => void;
} | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const queryClient = useQueryClient();
  const clearSession = useCallback(() => { setSession(null); queryClient.clear(); }, [queryClient]);
  return <Context.Provider value={{ session, setSession, clearSession }}>{children}</Context.Provider>;
}

export function useSession() {
  const value = useContext(Context);
  if (!value) throw new Error("SessionProvider is required");
  return value;
}
