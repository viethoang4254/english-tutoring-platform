export const outlineFields = {
  introduction: "Giới thiệu", objectives: "Mục tiêu học tập", audience: "Đối tượng học viên",
  requirements: "Yêu cầu đầu vào", outcomes: "Kết quả mong đợi",
};
export type OutlineField = keyof typeof outlineFields;
export type Topic = {title:string; description:string};
export type Stage = {title:string; topics:Topic[]};
export type CourseOutline = Record<OutlineField,string> & {stages:Stage[]};
