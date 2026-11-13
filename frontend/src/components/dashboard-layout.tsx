"use client";

import Link from "next/link";
import { useEffect, useId, useRef, useState, type ReactNode } from "react";
import { Icon } from "./site-shell";
import styles from "./dashboard-layout.module.css";

type Props = { title: string; roleLabel: string; navigation: ReactNode; navigationLabel: string; accountHref: string; busy?: boolean; onLogout: () => void; children: ReactNode };
export function DashboardLayout({title,roleLabel,navigation,navigationLabel,accountHref,busy,onLogout,children}:Props) {
  const drawer=useRef<HTMLDialogElement>(null), trigger=useRef<HTMLButtonElement>(null);
  const [open,setOpen]=useState(false);
  const id=useId();
  function close(){drawer.current?.close();setOpen(false);trigger.current?.focus();}
  useEffect(()=>{
    if(!open)return;
    const previous=document.body.style.overflow;
    document.body.style.overflow="hidden";
    drawer.current?.showModal();
    const media=window.matchMedia("(min-width: 901px)");
    const resize=()=>{if(media.matches){drawer.current?.close();setOpen(false);}};
    media.addEventListener("change",resize);
    return()=>{document.body.style.overflow=previous;media.removeEventListener("change",resize);};
  },[open]);
  const menu=<><div className={styles.brand}><Icon kind="book"/><div>TIẾNG ANH <span>CÙNG GIA SƯ</span></div></div><p className={styles.role}>{roleLabel}</p><nav aria-label={navigationLabel} className={styles.navigation} onClick={event=>{if((event.target as HTMLElement).closest("a,button"))close();}}>{navigation}</nav><button className={styles.logout} disabled={busy} onClick={onLogout}>Đăng xuất</button></>;
  return <div className={styles.shell} data-dashboard-layout>
    <aside className={styles.sidebar}>{menu}</aside>
    <div className={styles.body}>
      <header className={styles.topbar}><button ref={trigger} aria-label={navigationLabel} data-dashboard-toggle className={styles.toggle} aria-expanded={open} aria-controls={id} onClick={()=>setOpen(true)}>☰ <span>Điều hướng</span></button><div><p>{roleLabel}</p><h1>{title}</h1></div><details className={styles.account} data-dashboard-account><summary>Tài khoản</summary><div><Link href={accountHref}>Hồ sơ và bảo mật</Link><button disabled={busy} onClick={onLogout}>Đăng xuất</button></div></details></header>
      <main className={styles.content}>{children}</main>
    </div>
    <dialog ref={drawer} id={id} className={styles.drawer} aria-label={navigationLabel} onCancel={close} onClick={event=>{if(event.target===event.currentTarget){const rect=event.currentTarget.getBoundingClientRect();if(event.clientX>rect.right)close();}}}><button className={styles.close} onClick={close} aria-label="Đóng điều hướng">×</button>{open&&menu}</dialog>
  </div>;
}
