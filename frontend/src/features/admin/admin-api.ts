import { apiRequest } from "../../lib/api";
export async function adminMutation<T>(token:string,path:string,body?:Record<string,unknown>,method="POST") {
  const csrf=await apiRequest<{token:string}>("/admin/csrf",{credentials:"include",cache:"no-store",headers:{Authorization:`Bearer ${token}`}});
  if(!csrf?.token)throw new Error("Missing CSRF token");
  return apiRequest<T>(path,{method,credentials:"include",cache:"no-store",headers:{Authorization:`Bearer ${token}`,"X-XSRF-TOKEN":csrf.token,...(body?{"Content-Type":"application/json"}:{})},body:body?JSON.stringify(body):undefined});
}
