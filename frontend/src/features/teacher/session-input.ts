export type WeeklyRule = { id?: string; dayOfWeek: number; startTime: string; endTime: string };
export function validateRules(rules: WeeklyRule[]) {
  if (!rules.length || rules.length > 100) throw new Error("Cần từ 1 đến 100 quy tắc.");
  const sorted = [...rules].sort((a,b)=>a.dayOfWeek-b.dayOfWeek || a.startTime.localeCompare(b.startTime));
  sorted.forEach((r,i)=>{
    if (!Number.isInteger(r.dayOfWeek)||r.dayOfWeek<1||r.dayOfWeek>7||![r.startTime,r.endTime].every(t=>/^(?:[01]\d|2[0-3]):[0-5]\d$/.test(t))||r.startTime>=r.endTime)
      throw new Error("Chọn thứ và giờ hợp lệ; giờ kết thúc phải sau giờ bắt đầu trong cùng ngày.");
    const before=sorted[i-1];if(before&&before.dayOfWeek===r.dayOfWeek&&r.startTime<before.endTime)throw new Error("Các khoảng giờ bị trùng hoặc chồng lấn.");
  });
  return sorted.map(({dayOfWeek,startTime,endTime})=>({dayOfWeek,startTime,endTime}));
}
export function nullableText(value: string, limit: number, multiline=false) {
  if(value==="")return null;
  const text=value.replace(/^ +| +$/g,"");
  if(!text.trim()||Array.from(text).length>limit||!text.isWellFormed()||Array.from(text).some(c=>{
    const n=c.codePointAt(0)!;return (n<32||n>=127&&n<=159)&&!(multiline&&[9,10,13].includes(n));
  }))throw new Error(`Nội dung không hợp lệ hoặc vượt quá ${limit} ký tự.`);
  return text;
}
function formatter(zone:string) {return new Intl.DateTimeFormat("en-CA",{timeZone:zone,year:"numeric",month:"2-digit",day:"2-digit",hour:"2-digit",minute:"2-digit",hourCycle:"h23"});}
function parts(value:Date,format:Intl.DateTimeFormat) {
  const p=Object.fromEntries(format.formatToParts(value).map(x=>[x.type,x.value]));return `${p.year}-${p.month}-${p.day}T${p.hour}:${p.minute}`;
}
export function localInput(instant:string,zone:string) {return parts(new Date(instant),formatter(zone));}
export function localToInstant(local:string,zone:string) {
  if(!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(local))throw new Error("Nhập ngày giờ đầy đủ đến phút.");
  const nominal=new Date(`${local}:00Z`);
  if(!Number.isFinite(nominal.getTime())||nominal.toISOString().slice(0,16)!==local)throw new Error("Ngày giờ không hợp lệ.");
  const format=formatter(zone), matches:string[]=[];
  // Enumerate the bounded offset range rather than guess an offset across a DST transition.
  for(let minutes=-26*60;minutes<=26*60;minutes++) {
    const candidate=new Date(nominal.getTime()+minutes*60000);
    if(parts(candidate,format)===local)matches.push(candidate.toISOString());
  }
  if(matches.length!==1)throw new Error(matches.length?"Giờ này xuất hiện hai lần do đổi giờ mùa hè. Hãy chọn giờ khác.":"Giờ này không tồn tại trong múi giờ khóa học. Hãy chọn giờ khác.");
  return matches[0];
}
