import { CourseDetail } from "../../../../../features/course/course-detail";
export default async function StudentCourse({params}:{params:Promise<{courseId:string}>}) {
  const {courseId}=await params;
  return <CourseDetail courseId={courseId} basePath="/student/dashboard/courses" />;
}
