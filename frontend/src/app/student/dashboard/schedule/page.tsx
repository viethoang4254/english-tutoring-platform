import { StudentDashboard } from "../../../../features/student/student-dashboard";
import { StudentCalendar } from "../../../../features/student/student-calendar";

export default function StudentSchedulePage() {
  return <StudentDashboard><StudentCalendar /></StudentDashboard>;
}
