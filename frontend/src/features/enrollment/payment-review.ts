// Tab-local display context only. The API always supplies the actual transfer amount.
type Review = { amount: string; paymentId: string | null; deadline: string | null };
const key = (student: string, enrollment: string) => `checkout-review:${student}:${enrollment}`;

export function rememberReview(student: string, enrollment: string, review: Review) {
  try { sessionStorage.setItem(key(student,enrollment), JSON.stringify(review)); }
  catch { /* Without storage the payment panel asks for confirmation. */ }
}

export function reviewedAmount(student: string, enrollment: string, paymentId: string, expiresAt: string) {
  try {
    const review = JSON.parse(sessionStorage.getItem(key(student,enrollment)) ?? "null");
    if (typeof review?.amount !== "string" || !/^\d+$/.test(review.amount)) return null;
    if (review.paymentId === paymentId || (review.paymentId === null &&
        Date.parse(review.deadline) === Date.parse(expiresAt))) return review.amount as string;
  } catch { /* Missing or unusable context requires confirmation. */ }
  return null;
}
