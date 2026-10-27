import { notFound } from "next/navigation";
export default async function CoursePage({ params }: { params: Promise<{ courseId: string }> }) {
  const { courseId } = await params;
  if (!/^[1-9][0-9]*$/.test(courseId)) notFound();
  return null;
}
