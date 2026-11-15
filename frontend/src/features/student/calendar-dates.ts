export const dashboardTimeZone = "Asia/Ho_Chi_Minh";
const dayFormat = new Intl.DateTimeFormat("en-CA", {
  timeZone: dashboardTimeZone, year: "numeric", month: "2-digit", day: "2-digit",
});
export function dayKey(instant: Date | string) {
  const parts = dayFormat.formatToParts(new Date(instant));
  return ["year", "month", "day"].map(type => parts.find(part => part.type === type)!.value).join("-");
}
export function shiftDay(day: string, offset: number) {
  const date = new Date(day + "T12:00:00Z");
  date.setUTCDate(date.getUTCDate() + offset);
  return date.toISOString().slice(0, 10);
}
export function monthDays(month: string) {
  const first = new Date(month + "-01T12:00:00Z");
  const leading = (first.getUTCDay() + 6) % 7;
  const count = new Date(Date.UTC(first.getUTCFullYear(), first.getUTCMonth() + 1, 0)).getUTCDate();
  return Array.from({ length: Math.ceil((leading + count) / 7) * 7 }, (_, index) =>
    shiftDay(month + "-01", index - leading));
}
export function shiftMonth(month: string, offset: number) {
  const date = new Date(month + "-01T12:00:00Z");
  date.setUTCMonth(date.getUTCMonth() + offset);
  return date.toISOString().slice(0, 7);
}

export function occursOn(session: { startsAt: string; endsAt: string }, day: string) {
  return Date.parse(session.startsAt) < Date.parse(shiftDay(day, 1) + "T00:00:00+07:00")
    && Date.parse(session.endsAt) > Date.parse(day + "T00:00:00+07:00");
}
