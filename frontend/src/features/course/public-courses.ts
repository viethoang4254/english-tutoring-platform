export type PublicCourse = {
  id: string; name: string; description: string;
  tuition: {amount: string; currency: string};
  category: {id: string; name: string}; teacher: {id: string; fullName: string};
  plannedStartDate: string; timezone: string; sessionCount: number;
};
export type PublicCoursePage = {items: PublicCourse[]; page: number; size: number; hasNext: boolean};
export type PublicCourseDetail = {outline?:import("./course-outline").CourseOutline;course: PublicCourse; rules: {id: string; dayOfWeek: number; startTime: string; endTime: string}[]};
export function tuitionLabel(amount: string) {
  const [whole, fraction = '00'] = amount.split('.');
  const dong = BigInt(whole) + (fraction[0] >= '5' ? BigInt(1) : BigInt(0));
  return `${new Intl.NumberFormat('vi-VN').format(dong)} ₫`;
}
export function courseDate(value: string) { return value.split('-').reverse().join('/'); }
export function searchText(value: string) {
  return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[đĐ]/g, 'd').toLowerCase().trim();
}
