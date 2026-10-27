import { previewCourses, type PreviewCourse } from '../student/dashboard-preview';

// Preview-only records. Replace with an approved public DTO when discovery APIs exist.
export type DiscoveryCourse = PreviewCourse & {
  description: string;
  tuition: number;
  schedule: string;
  specialization: string;
  experience: string;
};
export const previewCategories = [
  { id: 'conversation', name: 'Giao tiếp' },
  { id: 'work', name: 'Công việc' },
] as const;
export const discoveryCourses: DiscoveryCourse[] = previewCourses.map((course, index) => ({
  ...course,
  description: index === 0
    ? 'Cùng gia sư thực hành giao tiếp trong những tình huống quen thuộc: giới thiệu bản thân, hỏi đường và trao đổi hằng ngày.'
    : 'Thực hành trao đổi trong cuộc họp, trình bày ý tưởng và giao tiếp với đồng nghiệp dưới sự hướng dẫn của gia sư.',
  tuition: index === 0 ? 1800000 : 2000000,
  schedule: index === 0 ? 'Thứ Ba và Thứ Năm · 19:00–20:30' : 'Thứ Hai và Thứ Tư · 18:30–20:00',
  specialization: index === 0 ? 'Tiếng Anh giao tiếp' : 'Tiếng Anh công sở',
  experience: 'Thông tin gia sư minh họa; chưa phải hồ sơ giảng dạy đã được xác nhận.',
}));
export const tuitionLabel = (value: number) => new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND', maximumFractionDigits: 0}).format(value);
export function searchText(value: string) {
  return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[đĐ]/g, 'd').toLowerCase().trim();
}