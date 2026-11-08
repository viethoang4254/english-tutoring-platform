import assert from "node:assert/strict";
import {readFileSync} from "node:fs";
import test from "node:test";
import {apiRequest, ApiRequestError} from "../src/lib/api.ts";
import {fieldsError, imageError, privateRequest, reasonError} from "../src/features/receiving/receiving.ts";

test("selector contains exactly the transfer-supported entries in the approved snapshot",()=>{
  const snapshot=JSON.parse(readFileSync(new URL("../../backend/src/main/resources/bank-directory/vietqr-v1.json",import.meta.url),"utf8"));
  type Bank={bin:string;code:string;name:string;transferSupported:boolean};
  const options=JSON.parse(readFileSync(new URL("../src/features/receiving/bank-options.json",import.meta.url),"utf8"));
  assert.deepEqual(options,snapshot.banks.filter((b:Bank)=>b.transferSupported).map(({bin,code,name}:Bank)=>({bin,code,name})));
});
test("submission validates bank, holder and account number without losing leading zeros",()=>{
  const valid=(b:string,h:string,n:string)=>fieldsError(b,["970436"],h,n);
  assert.equal(valid("970436","Gia sư","0000123"),undefined);
  for(const data of [["other","Gia sư","0001"],["970436","  ","1"],["970436","a\nb","1"],["970436","a".repeat(151),"1"],["970436","Gia sư","12a"],["970436","Gia sư","0".repeat(35)]]){
    assert.ok(valid(data[0],data[1],data[2]));
  }
  assert.ok(reasonError("  "));assert.ok(reasonError("a\nb"));assert.ok(reasonError("a".repeat(2001)));assert.equal(reasonError("Thông tin không khớp"),undefined);
});
test("QR validation rejects missing, oversized, mislabeled and undecodable data",async()=>{
  assert.ok(await imageError(null));
  assert.ok(await imageError(new File([new Uint8Array(5*1024*1024+1)],"large.png",{type:"image/png"})));
  assert.ok(await imageError(new File(["text"],"qr.png",{type:"image/png"})));
  assert.ok(await imageError(new File([new Uint8Array([255,216,255])],"broken.jpg",{type:"image/jpeg"})));
});
test("QR dimension caps apply before decoding PNG and JPEG headers",async()=>{
  const png=new Uint8Array(24);png.set([137,80,78,71,13,10,26,10]);const view=new DataView(png.buffer);
  view.setUint32(16,4001);view.setUint32(20,4001);
  assert.match((await imageError(new File([png],"qr.png",{type:"image/png"})))!,/megapixel/);
  const jpeg=Uint8Array.of(255,216,255,192,0,8,8,0,1,16,1,0);
  assert.match((await imageError(new File([jpeg],"qr.jpg",{type:"image/jpeg"})))!,/4096/);
});
test("QR transport requires PNG from the configured API and preserves authorization errors",async t=>{
  const original=process.env.NEXT_PUBLIC_API_ORIGIN;
  process.env.NEXT_PUBLIC_API_ORIGIN="https://backend.example";
  t.after(()=>{if(original===undefined)delete process.env.NEXT_PUBLIC_API_ORIGIN;else process.env.NEXT_PUBLIC_API_ORIGIN=original;});
  const path="/admin/payment-accounts/9/qr-image";
  const fetchMock=t.mock.method(globalThis,"fetch",async (url:RequestInfo|URL,init?:RequestInit)=>{
    assert.equal(url,"https://backend.example/api/v1"+path);assert.equal(init?.redirect,"error");
    assert.equal(init?.cache,"no-store");assert.equal(init?.credentials,"omit");
    assert.equal(new Headers(init?.headers).get("Accept"),"image/png");
    return new Response(new Uint8Array([1,2]),{headers:{"Content-Type":"image/png"}});
  });
  assert.equal((await apiRequest(path,privateRequest("fixture"),"image/png")).size,2);
  fetchMock.mock.mockImplementation(async()=>new Response("html",{headers:{"Content-Type":"text/html"}}));
  await assert.rejects(apiRequest(path,privateRequest("fixture"),"image/png"),ApiRequestError);
  fetchMock.mock.mockImplementation(async()=>Response.json({code:"ACCESS_DENIED",message:"Denied"},{status:403}));
  await assert.rejects(apiRequest(path,privateRequest("fixture"),"image/png"),(e:unknown)=>e instanceof ApiRequestError && e.status===403);
});
test("private request options keep credentials in the authorization header only",()=>{
  const request=privateRequest("fixture");
  assert.equal(request.cache,"no-store");assert.equal(request.credentials,"omit");
  assert.equal(new Headers(request.headers).get("Authorization"),"Bearer fixture");
});
