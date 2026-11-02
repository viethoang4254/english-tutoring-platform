export type PublicCourse = {
  id: string; name: string; description: string;
  tuition: {amount: string; currency: string};
  category: {id: string; name: string}; teacher: {id: string; fullName: string};
  plannedStartDate: string; timezone: string; sessionCount: number;
};
export type PublicCoursePage = {items: PublicCourse[]; page: number; size: number; hasNext: boolean};
export type PublicCourseDetail = {course: PublicCourse; rules: {id: string; dayOfWeek: number; startTime: string; endTime: string}[]};
export function tuitionLabel(amount: string) {
  const [whole, fraction = '00'] = amount.split('.');
  return `${new Intl.NumberFormat('vi-VN').format(BigInt(whole))}${/^0+$/.test(fraction) ? '' : `,${fraction}`} ₫`;
}
export function courseDate(value: string) { return value.split('-').reverse().join('/'); }
export function searchText(value: string) {
  return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[đĐ]/g, 'd').toLowerCase().trim();
}
