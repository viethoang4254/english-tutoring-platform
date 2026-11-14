import { CourseCheckout } from "../../../../../../features/enrollment/course-checkout";
export default async function CheckoutPage({params}:{params:Promise<{courseId:string}>}) {
  const {courseId}=await params;
  return <CourseCheckout courseId={courseId}/>;
}
