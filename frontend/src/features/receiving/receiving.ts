import {apiRequest, ApiRequestError} from "../../lib/api.ts";

export interface Account {
  id: string; bankBin: string | null; bankCode: string; bankName: string | null;
  holderName: string; accountNumber: string; reviewStatus: "PENDING" | "APPROVED" | "REJECTED" | "UNREVIEWED";
  selectionStatus: "ACTIVE" | "INACTIVE"; hasQrImage: boolean; createdAt: string;
}
export interface Page<T> { items: T[]; page: number; size: number; hasNext: boolean }
export interface QueueItem { id: string; teacherId: string; bankName: string; submittedAt: string }
export interface Review { teacherId: string; account: Account; reviewedBy: string | null; reviewedAt: string | null; rejectionReason: string | null }
export const statuses = {PENDING:"Chờ xét duyệt", APPROVED:"Đã phê duyệt", REJECTED:"Đã từ chối", UNREVIEWED:"Chưa xét duyệt"};
export const teacherPath = "/teacher/payment-accounts", adminPath = "/admin/payment-accounts";

export function privateRequest(token: string, signal?: AbortSignal): RequestInit {
  return {credentials:"omit", cache:"no-store", headers:{Authorization:`Bearer ${token}`}, signal};
}
export async function read<T>(token: string, path: string, signal: AbortSignal): Promise<T> {
  const data = await apiRequest<T>(path, privateRequest(token, signal));
  if (!data) throw new ApiRequestError(500,"INVALID_RESPONSE","Missing response");
  return data;
}
const controls = (s: string) => Array.from(s).some(c => c.charCodeAt(0)<32 || (c.charCodeAt(0)>=127 && c.charCodeAt(0)<=159));
export function fieldsError(bank: string, supported: readonly string[], holder: string, number: string) {
  if (!supported.includes(bank)) return "Chọn ngân hàng trong danh sách.";
  if (!holder.trim() || holder.length > 150 || controls(holder)) return "Nhập tên chủ tài khoản hợp lệ (tối đa 150 ký tự).";
  if (!/^[0-9]{1,34}$/.test(number)) return "Số tài khoản phải gồm 1 đến 34 chữ số.";
}
export function reasonError(reason: string) {
  if (!reason.trim() || reason.length>2000 || controls(reason)) return "Nhập lý do từ chối (tối đa 2000 ký tự, không có ký tự điều khiển).";
}
export async function imageError(file: File | null) {
  if (!file || !file.size || file.size > 5*1024*1024 || !["image/png","image/jpeg"].includes(file.type))
    return "Chọn ảnh PNG hoặc JPEG tối đa 5 MiB.";
  const bytes = new Uint8Array(await file.arrayBuffer());
  const matches = file.type==="image/png" ? [137,80,78,71,13,10,26,10].every((v,i)=>bytes[i]===v) : bytes[0]===255 && bytes[1]===216 && bytes[2]===255;
  if (!matches) return "Nội dung ảnh không khớp với định dạng đã chọn.";
  // Check declared dimensions before allocating a decoded bitmap.
  const view=new DataView(bytes.buffer);
  let width=0, height=0;
  if(file.type==="image/png" && bytes.length>=24) {
    width=view.getUint32(16);height=view.getUint32(20);
  } else if(file.type==="image/jpeg") {
    for(let i=2;i+4<=bytes.length && bytes[i]===255;){
      while(bytes[i]===255)i++;
      const marker=bytes[i++];
      if(marker===0xda || marker===0xd9 || i+2>bytes.length)break;
      const length=view.getUint16(i);
      if(length<2 || i+length>bytes.length)break;
      if([0xc0,0xc1,0xc2,0xc3,0xc5,0xc6,0xc7,0xc9,0xca,0xcb,0xcd,0xce,0xcf].includes(marker) && length>=8){
        height=view.getUint16(i+3);width=view.getUint16(i+5);break;
      }
      i+=length;
    }
  }
  if(!width || !height)return "Không thể đọc kích thước ảnh QR.";
  if(width>4096 || height>4096 || width*height>16_000_000)return "Ảnh vượt giới hạn 4096 px mỗi chiều hoặc 16 megapixel.";
  try {
    const bitmap = await createImageBitmap(file);
    try {
      if (bitmap.width>4096 || bitmap.height>4096 || bitmap.width*bitmap.height>16_000_000) return "Ảnh vượt giới hạn 4096 px mỗi chiều hoặc 16 megapixel.";
    } finally {bitmap.close();}
  } catch {return "Không thể đọc ảnh QR.";}
}
