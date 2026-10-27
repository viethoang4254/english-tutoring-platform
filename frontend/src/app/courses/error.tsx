"use client";
import Link from 'next/link';
import styles from '../../features/course/discovery.module.css';
export default function ErrorPage({reset}: {reset:()=>void}) { return <main className={styles.page}><h1>Chưa thể tải khóa học</h1><p role="alert">Vui lòng thử lại sau. Thông tin đăng ký của bạn không bị thay đổi.</p><button className="ui-button ui-button-primary" onClick={reset}>Thử lại</button> <Link href="/courses">Về danh sách khóa học</Link></main>; }