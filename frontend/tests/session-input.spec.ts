import test from "node:test";
import assert from "node:assert/strict";
import {localToInstant,localInput,nullableText,validateRules} from "../src/features/teacher/session-input.ts";
test("weekly rules reject overlaps and duplicates while allowing adjacent intervals",()=>{
  const a={dayOfWeek:1,startTime:"09:00",endTime:"10:00"};
  assert.throws(()=>validateRules([a,a]));assert.throws(()=>validateRules([a,{...a,startTime:"09:30"}]));
  assert.equal(validateRules([a,{...a,startTime:"10:00",endTime:"11:00"}]).length,2);
  assert.throws(()=>validateRules([]));assert.throws(()=>validateRules(Array(101).fill(a)));
  assert.throws(()=>validateRules([{...a,startTime:"9:00"}]));assert.throws(()=>validateRules([{...a,dayOfWeek:0}]));
});
test("text validation preserves code point limits and null clearing",()=>{
  assert.equal(nullableText("",200),null);assert.equal(nullableText("  Text  ",200),"Text");
  assert.equal(nullableText("😀".repeat(200),200),"😀".repeat(200));assert.throws(()=>nullableText("😀".repeat(201),200));
  for(const v of [" ","a\u0000","\uD800","a\n"])assert.throws(()=>nullableText(v,200));
  assert.equal(nullableText("<b>text</b>\n",10000,true),"<b>text</b>\n");
});
test("Course local conversion is independent of browser timezone",()=>{
  assert.equal(localToInstant("2030-01-01T19:00","Asia/Ho_Chi_Minh"),"2030-01-01T12:00:00.000Z");
  assert.equal(localInput("2030-01-01T12:00:00Z","Asia/Ho_Chi_Minh"),"2030-01-01T19:00");
  assert.equal(localToInstant("2030-01-01T09:00","Asia/Kathmandu"),"2030-01-01T03:15:00.000Z");
});
test("DST gaps and repeated times fail rather than choosing an offset",()=>{
  assert.throws(()=>localToInstant("2030-03-10T02:30","America/New_York"));
  assert.throws(()=>localToInstant("2030-11-03T01:30","America/New_York"));
  assert.equal(localToInstant("2030-03-10T03:30","America/New_York"),"2030-03-10T07:30:00.000Z");
  assert.throws(()=>localToInstant("2030-02-30T12:00","UTC"));
});
